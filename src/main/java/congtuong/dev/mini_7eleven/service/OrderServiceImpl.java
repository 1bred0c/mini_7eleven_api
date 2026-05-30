package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.*;
import congtuong.dev.mini_7eleven.enums.OrderStatus;
import congtuong.dev.mini_7eleven.enums.PaymentMethod;
import congtuong.dev.mini_7eleven.enums.PaymentStatus;
import congtuong.dev.mini_7eleven.enums.ProductStatus;
import congtuong.dev.mini_7eleven.exception.BadRequestException;
import congtuong.dev.mini_7eleven.exception.ForbiddenException;
import congtuong.dev.mini_7eleven.exception.NotFoundException;
import congtuong.dev.mini_7eleven.pojo.*;
import congtuong.dev.mini_7eleven.repository.OrderRepository;
import congtuong.dev.mini_7eleven.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AccountService accountService;
    private final WalletService walletService;
    private final AddressService addressService;

    @Override
    @Transactional
    public OrderResponse create(String email, OrderCreateRequest request) {
        Account account = accountService.getByEmail(email);
        Address address = addressService.getOwnedAddress(email, request.getAddressId());
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        Map<Long, Integer> quantities = mergeQuantities(request.getItems());
        List<Product> products = productRepository.findAllById(quantities.keySet());
        if (products.size() != quantities.size()) {
            throw new NotFoundException("PRODUCT_NOT_FOUND", "One or more products not found");
        }

        for (Product product : products) {
            int quantity = quantities.get(product.getId());
            validateProductForOrder(product, quantity);

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(quantity)
                    .subtotal(subtotal)
                    .build();
            items.add(item);

            int newStock = product.getStockQuantity() - quantity;
            product.setStockQuantity(newStock);
            if (newStock <= 0) {
                product.setStatus(ProductStatus.OUT_OF_STOCK);
            } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
                product.setStatus(ProductStatus.ACTIVE);
            }
        }

        Order order = Order.builder()
                .account(account)
                .customerName(address.getReceiverName())
                .phoneNumber(address.getPhoneNumber())
                .address(formatAddress(address))
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.UNPAID)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));
        productRepository.saveAll(products);
        Order saved = orderRepository.save(order);

        if (saved.getPaymentMethod() == PaymentMethod.WALLET) {
            walletService.payForOrder(saved.getAccount().getId(), saved.getTotalAmount(), "Order #" + saved.getId());
            saved.setPaymentStatus(PaymentStatus.PAID);
            saved = orderRepository.save(saved);
        }

        return toDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return toDetailResponse(getOrderWithItems(id));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByIdForUser(String email, Long id) {
        Order order = getOrderWithItems(id);
        validateOwnership(order, email);
        return toDetailResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(String email, Pageable pageable) {
        Account account = accountService.getByEmail(email);
        return orderRepository.findByAccountId(account.getId(), pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> search(OrderStatus status, PaymentStatus paymentStatus, Long accountId, Pageable pageable) {
        return orderRepository.search(status, paymentStatus, accountId, pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order order = getOrderWithItems(id);
        OrderStatus next = resolveOrderStatus(order.getStatus(), status);
        if (next == order.getStatus()) {
            return toDetailResponse(order);
        }
        order.setStatus(next);
        if (next == OrderStatus.CANCELLED) {
            processRefundIfNeeded(order);
            restoreStock(order);
        }
        return toDetailResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(Long id, PaymentStatus paymentStatus) {
        Order order = getOrder(id);
        if (order.getPaymentMethod() == PaymentMethod.WALLET) {
            return updateWalletPaymentStatus(order, paymentStatus);
        }
        PaymentStatus next = resolvePaymentStatus(order.getPaymentStatus(), paymentStatus);
        if (next == order.getPaymentStatus()) {
            return toDetailResponse(getOrderWithItems(id));
        }
        order.setPaymentStatus(next);
        return toDetailResponse(getOrderWithItems(orderRepository.save(order).getId()));
    }

    @Override
    @Transactional
    public OrderResponse cancelByUser(String email, Long id) {
        Order order = getOrderWithItems(id);
        validateOwnership(order, email);

        OrderStatus next = resolveOrderStatus(order.getStatus(), OrderStatus.CANCELLED);
        if (next != OrderStatus.CANCELLED) {
            throw new BadRequestException("ORDER_STATUS_INVALID", "Order cannot be cancelled at this stage");
        }
        order.setStatus(OrderStatus.CANCELLED);
        processRefundIfNeeded(order);
        restoreStock(order);
        return toDetailResponse(orderRepository.save(order));
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
    }

    private Order getOrderWithItems(Long id) {
        return orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
    }

    private void validateOwnership(Order order, String email) {
        Account account = accountService.getByEmail(email);
        if (!order.getAccount().getId().equals(account.getId())) {
            throw new ForbiddenException("ORDER_ACCESS_DENIED", "You do not have access to this order");
        }
    }

    private void validateProductForOrder(Product product, int quantity) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("PRODUCT_NOT_AVAILABLE", "Product is not available");
        }
        if (product.getStockQuantity() < quantity) {
            throw new BadRequestException("PRODUCT_OUT_OF_STOCK", "Not enough stock for product " + product.getName());
        }
    }

    private OrderStatus resolveOrderStatus(OrderStatus current, OrderStatus requested) {
        if (requested == null || requested == current) {
            return current;
        }
        return switch (current) {
            case PENDING -> allowTransition(current, requested, OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
            case CONFIRMED -> allowTransition(current, requested, OrderStatus.PREPARING, OrderStatus.CANCELLED);
            case PREPARING -> allowTransition(current, requested, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
            case COMPLETED, CANCELLED -> throw new BadRequestException("ORDER_STATUS_INVALID", "Order status cannot be changed");
        };
    }

    private PaymentStatus resolvePaymentStatus(PaymentStatus current, PaymentStatus requested) {
        if (requested == null || requested == current) {
            return current;
        }
        return switch (current) {
            case UNPAID -> allowTransition(current, requested, PaymentStatus.PAID, PaymentStatus.FAILED);
            case FAILED -> allowTransition(current, requested, PaymentStatus.UNPAID);
            case PAID -> allowTransition(current, requested, PaymentStatus.REFUNDED);
            case REFUNDED -> throw new BadRequestException("PAYMENT_STATUS_INVALID", "Payment status cannot be changed");
        };
    }

    private <T> T allowTransition(T current, T requested, T... allowed) {
        for (T value : allowed) {
            if (value.equals(requested)) {
                return requested;
            }
        }
        throw new BadRequestException("STATUS_TRANSITION_INVALID", "Invalid status transition from " + current + " to " + requested);
    }

    private void restoreStock(Order order) {
        List<Product> products = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK && product.getStockQuantity() > 0) {
                product.setStatus(ProductStatus.ACTIVE);
            }
            products.add(product);
        }
        productRepository.saveAll(products);
    }

    private void processRefundIfNeeded(Order order) {
        if (order.getPaymentMethod() == PaymentMethod.WALLET && order.getPaymentStatus() == PaymentStatus.PAID) {
            walletService.refundToWallet(order.getAccount().getId(), order.getTotalAmount(), "Refund for order #" + order.getId());
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
    }

    private OrderResponse updateWalletPaymentStatus(Order order, PaymentStatus requested) {
        if (requested == null || requested == order.getPaymentStatus()) {
            return toDetailResponse(getOrderWithItems(order.getId()));
        }
        if (requested == PaymentStatus.REFUNDED && order.getPaymentStatus() == PaymentStatus.PAID) {
            walletService.refundToWallet(order.getAccount().getId(), order.getTotalAmount(), "Refund for order #" + order.getId());
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            return toDetailResponse(getOrderWithItems(orderRepository.save(order).getId()));
        }
        throw new BadRequestException("PAYMENT_STATUS_INVALID", "Wallet payments can only be refunded from PAID status");
    }

    private Map<Long, Integer> mergeQuantities(List<OrderItemRequest> items) {
        Map<Long, Integer> quantities = new HashMap<>();
        for (OrderItemRequest item : items) {
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private OrderResponse toDetailResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .accountId(order.getAccount().getId())
                .customerName(order.getCustomerName())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderSummaryResponse toSummaryResponse(Order order) {
        return OrderSummaryResponse.builder()
                .id(order.getId())
                .accountId(order.getAccount().getId())
                .customerName(order.getCustomerName())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    private String formatAddress(Address address) {
        StringBuilder builder = new StringBuilder();
        builder.append(address.getAddressLine());
        appendSegment(builder, address.getWard());
        appendSegment(builder, address.getDistrict());
        appendSegment(builder, address.getCity());
        return builder.toString();
    }

    private void appendSegment(StringBuilder builder, String segment) {
        if (segment == null || segment.isBlank()) {
            return;
        }
        builder.append(", ").append(segment.trim());
    }
}
