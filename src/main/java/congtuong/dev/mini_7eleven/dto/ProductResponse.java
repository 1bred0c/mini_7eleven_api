package congtuong.dev.mini_7eleven.dto;

import congtuong.dev.mini_7eleven.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private ProductStatus status;
    private CategorySummary category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

