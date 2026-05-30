package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.ProductCreateRequest;
import congtuong.dev.mini_7eleven.dto.ProductResponse;
import congtuong.dev.mini_7eleven.dto.ProductUpdateRequest;
import congtuong.dev.mini_7eleven.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    ProductResponse create(ProductCreateRequest request);

    ProductResponse update(Long id, ProductUpdateRequest request);

    ProductResponse updateStatus(Long id, ProductStatus status);

    ProductResponse updateStock(Long id, Integer stockQuantity);

    void delete(Long id);

    ProductResponse getById(Long id);

    Page<ProductResponse> search(
            String keyword,
            ProductStatus status,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minStock,
            Integer maxStock,
            Pageable pageable
    );
}

