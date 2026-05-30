package congtuong.dev.mini_7eleven.dto;

import congtuong.dev.mini_7eleven.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequest {

    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity must be >= 0")
    private Integer stockQuantity;

    @Size(max = 2048, message = "Image URL must be at most 2048 characters")
    private String imageUrl;

    private ProductStatus status;

    private Long categoryId;
}

