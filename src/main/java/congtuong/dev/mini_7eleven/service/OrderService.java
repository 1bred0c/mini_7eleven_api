package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.OrderCreateRequest;
import congtuong.dev.mini_7eleven.dto.OrderResponse;
import congtuong.dev.mini_7eleven.dto.OrderSummaryResponse;
import congtuong.dev.mini_7eleven.enums.OrderStatus;
import congtuong.dev.mini_7eleven.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse create(String email, OrderCreateRequest request);

    OrderResponse getById(Long id);

    OrderResponse getByIdForUser(String email, Long id);

    Page<OrderSummaryResponse> getMyOrders(String email, Pageable pageable);

    Page<OrderSummaryResponse> search(OrderStatus status, PaymentStatus paymentStatus, Long accountId, Pageable pageable);

    OrderResponse updateStatus(Long id, OrderStatus status);

    OrderResponse updatePaymentStatus(Long id, PaymentStatus paymentStatus);

    OrderResponse cancelByUser(String email, Long id);
}
