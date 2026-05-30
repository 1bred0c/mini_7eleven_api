package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.OrderCreateRequest;
import congtuong.dev.mini_7eleven.dto.OrderItemRequest;
import congtuong.dev.mini_7eleven.dto.OrderResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private WalletService walletService;

    @Mock
    private AddressService addressService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldCreateOrderAndReduceStockSuccessfully() {
        Account account = buildAccount(10L);
        Address address = buildAddress(account);

        Product product1 = buildProduct(1L, "Coke", BigDecimal.valueOf(10), 5, ProductStatus.ACTIVE);
        Product product2 = buildProduct(2L, "Chips", BigDecimal.valueOf(20), 3, ProductStatus.ACTIVE);

        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(addressService.getOwnedAddress("user@example.com", 100L)).thenReturn(address);
        when(productRepository.findAllById(any())).thenReturn(List.of(product1, product2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(50L);
            }
            return order;
        });

        OrderCreateRequest request = OrderCreateRequest.builder()
                .addressId(100L)
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .items(List.of(
                        OrderItemRequest.builder().productId(1L).quantity(2).build(),
                        OrderItemRequest.builder().productId(1L).quantity(1).build(),
                        OrderItemRequest.builder().productId(2L).quantity(1).build()
                ))
                .build();

        OrderResponse response = orderService.create("user@example.com", request);

        assertEquals(BigDecimal.valueOf(50), response.getTotalAmount());
        assertEquals(2, response.getItems().size());
        assertEquals("Coke", response.getItems().get(0).getProductName());
        assertEquals(BigDecimal.valueOf(10), response.getItems().get(0).getUnitPrice());
        assertEquals(50L, response.getId());

        assertEquals(2, product1.getStockQuantity());
        assertEquals(2, product2.getStockQuantity());

        ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void shouldCreateOrderWithWalletPaymentAndSetPaidStatus() {
        Account account = buildAccount(11L);
        Address address = buildAddress(account);
        Product product = buildProduct(3L, "Milk", BigDecimal.valueOf(15), 2, ProductStatus.ACTIVE);

        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(addressService.getOwnedAddress("user@example.com", 200L)).thenReturn(address);
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(60L);
            }
            return order;
        });

        OrderCreateRequest request = OrderCreateRequest.builder()
                .addressId(200L)
                .paymentMethod(PaymentMethod.WALLET)
                .items(List.of(OrderItemRequest.builder().productId(3L).quantity(1).build()))
                .build();

        OrderResponse response = orderService.create("user@example.com", request);

        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
        verify(walletService).payForOrder(11L, BigDecimal.valueOf(15), "Order #60");

        InOrder inOrder = inOrder(orderRepository, walletService);
        inOrder.verify(orderRepository).save(any(Order.class));
        inOrder.verify(walletService).payForOrder(11L, BigDecimal.valueOf(15), "Order #60");
        inOrder.verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldRejectOrderWhenProductNotFound() {
        Account account = buildAccount(12L);
        Address address = buildAddress(account);

        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(addressService.getOwnedAddress("user@example.com", 100L)).thenReturn(address);
        when(productRepository.findAllById(any())).thenReturn(List.of(buildProduct(1L, "Coke", BigDecimal.ONE, 5, ProductStatus.ACTIVE)));

        OrderCreateRequest request = OrderCreateRequest.builder()
                .addressId(100L)
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .items(List.of(
                        OrderItemRequest.builder().productId(1L).quantity(1).build(),
                        OrderItemRequest.builder().productId(2L).quantity(1).build()
                ))
                .build();

        assertThrows(NotFoundException.class, () -> orderService.create("user@example.com", request));
    }

    @Test
    void shouldRejectOrderWhenProductIsInactive() {
        Account account = buildAccount(13L);
        Address address = buildAddress(account);
        Product product = buildProduct(5L, "Candy", BigDecimal.valueOf(2), 5, ProductStatus.INACTIVE);

        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(addressService.getOwnedAddress("user@example.com", 100L)).thenReturn(address);
        when(productRepository.findAllById(any())).thenReturn(List.of(product));

        OrderCreateRequest request = OrderCreateRequest.builder()
                .addressId(100L)
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .items(List.of(OrderItemRequest.builder().productId(5L).quantity(1).build()))
                .build();

        assertThrows(BadRequestException.class, () -> orderService.create("user@example.com", request));
    }

    @Test
    void shouldRejectOrderWhenStockIsInsufficient() {
        Account account = buildAccount(14L);
        Address address = buildAddress(account);
        Product product = buildProduct(6L, "Juice", BigDecimal.valueOf(3), 1, ProductStatus.ACTIVE);

        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(addressService.getOwnedAddress("user@example.com", 100L)).thenReturn(address);
        when(productRepository.findAllById(any())).thenReturn(List.of(product));

        OrderCreateRequest request = OrderCreateRequest.builder()
                .addressId(100L)
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .items(List.of(OrderItemRequest.builder().productId(6L).quantity(2).build()))
                .build();

        assertThrows(BadRequestException.class, () -> orderService.create("user@example.com", request));
    }

    @Test
    void shouldRejectUserAccessToAnotherUsersOrder() {
        Account account = buildAccount(20L);
        Order order = buildOrder(70L, buildAccount(21L), List.of(buildOrderItem(1L, "Item", BigDecimal.ONE, 1)));

        when(orderRepository.findWithItemsById(70L)).thenReturn(Optional.of(order));
        when(accountService.getByEmail("user@example.com")).thenReturn(account);

        assertThrows(ForbiddenException.class, () -> orderService.getByIdForUser("user@example.com", 70L));
    }

    @Test
    void shouldUpdateOrderStatusSuccessfully() {
        Account account = buildAccount(30L);
        Order order = buildOrder(80L, account, List.of(buildOrderItem(1L, "Item", BigDecimal.ONE, 1)));
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findWithItemsById(80L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.updateStatus(80L, OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
    }

    private Account buildAccount(Long id) {
        Account account = Account.builder()
                .fullName("User")
                .email("user@example.com")
                .passwordHash("hash")
                .role(congtuong.dev.mini_7eleven.enums.Role.USER)
                .build();
        account.setId(id);
        return account;
    }

    private Address buildAddress(Account account) {
        Address address = Address.builder()
                .account(account)
                .receiverName("Customer")
                .phoneNumber("123")
                .addressLine("123 Main St")
                .ward("Ward")
                .district("District")
                .city("City")
                .isDefault(true)
                .build();
        address.setId(99L);
        return address;
    }

    private Product buildProduct(Long id, String name, BigDecimal price, int stock, ProductStatus status) {
        Product product = Product.builder()
                .name(name)
                .description("Desc")
                .price(price)
                .stockQuantity(stock)
                .status(status)
                .category(Category.builder().name("Category").build())
                .build();
        product.setId(id);
        return product;
    }

    private Order buildOrder(Long id, Account account, List<OrderItem> items) {
        Order order = Order.builder()
                .account(account)
                .customerName("Customer")
                .phoneNumber("123")
                .address("123 Main St")
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .paymentStatus(PaymentStatus.UNPAID)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.TEN)
                .items(items)
                .build();
        order.setId(id);
        items.forEach(item -> item.setOrder(order));
        return order;
    }

    private OrderItem buildOrderItem(Long productId, String name, BigDecimal price, int quantity) {
        Product product = buildProduct(productId, name, price, 10, ProductStatus.ACTIVE);
        OrderItem item = OrderItem.builder()
                .product(product)
                .productName(name)
                .unitPrice(price)
                .quantity(quantity)
                .subtotal(price.multiply(BigDecimal.valueOf(quantity)))
                .build();
        item.setId(productId + 1000);
        return item;
    }
}

