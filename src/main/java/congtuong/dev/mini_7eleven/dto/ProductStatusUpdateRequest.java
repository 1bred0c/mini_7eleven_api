package congtuong.dev.mini_7eleven.dto;

import congtuong.dev.mini_7eleven.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ProductStatus status;
}

