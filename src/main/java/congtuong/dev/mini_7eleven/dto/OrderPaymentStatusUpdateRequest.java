package congtuong.dev.mini_7eleven.dto;

import congtuong.dev.mini_7eleven.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPaymentStatusUpdateRequest {

    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;
}

