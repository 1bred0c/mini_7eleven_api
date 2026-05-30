package congtuong.dev.mini_7eleven.controller;

import congtuong.dev.mini_7eleven.dto.*;
import congtuong.dev.mini_7eleven.enums.OrderStatus;
import congtuong.dev.mini_7eleven.enums.PaymentStatus;
import congtuong.dev.mini_7eleven.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public OrderResponse create(@AuthenticationPrincipal UserDetails principal,
                                @Valid @RequestBody OrderCreateRequest request) {
        return orderService.create(principal.getUsername(), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public Page<OrderSummaryResponse> getMyOrders(@AuthenticationPrincipal UserDetails principal,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return orderService.getMyOrders(principal.getUsername(), pageable);
    }

    @GetMapping("/my/{id}")
    @PreAuthorize("hasRole('USER')")
    public OrderResponse getMyOrder(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return orderService.getByIdForUser(principal.getUsername(), id);
    }

    @PatchMapping("/my/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public OrderResponse cancelMyOrder(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return orderService.cancelByUser(principal.getUsername(), id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderSummaryResponse> search(@RequestParam(required = false) OrderStatus status,
                                      @RequestParam(required = false) PaymentStatus paymentStatus,
                                      @RequestParam(required = false) Long accountId,
                                      @PageableDefault(size = 20) Pageable pageable) {
        return orderService.search(status, paymentStatus, accountId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(id, request.getStatus());
    }

    @PatchMapping("/{id}/payment-status")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse updatePaymentStatus(@PathVariable Long id,
                                             @Valid @RequestBody OrderPaymentStatusUpdateRequest request) {
        return orderService.updatePaymentStatus(id, request.getPaymentStatus());
    }
}
