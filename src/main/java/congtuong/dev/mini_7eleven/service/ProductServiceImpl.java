package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.CategorySummary;
import congtuong.dev.mini_7eleven.dto.ProductCreateRequest;
import congtuong.dev.mini_7eleven.dto.ProductResponse;
import congtuong.dev.mini_7eleven.dto.ProductUpdateRequest;
import congtuong.dev.mini_7eleven.enums.ProductStatus;
import congtuong.dev.mini_7eleven.exception.BadRequestException;
import congtuong.dev.mini_7eleven.exception.NotFoundException;
import congtuong.dev.mini_7eleven.pojo.Category;
import congtuong.dev.mini_7eleven.pojo.Product;
import congtuong.dev.mini_7eleven.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    @Override
    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        Category category = categoryService.getEntity(request.getCategoryId());
        ProductStatus status = resolveStatus(null, request.getStatus(), request.getStockQuantity());

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .status(status)
                .category(category)
                .build();

        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = getProduct(id);

        if (request.getName() != null) {
            product.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getCategoryId() != null) {
            product.setCategory(categoryService.getEntity(request.getCategoryId()));
        }

        ProductStatus status = resolveStatus(product.getStatus(), request.getStatus(), product.getStockQuantity());
        product.setStatus(status);

        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateStatus(Long id, ProductStatus status) {
        Product product = getProduct(id);
        ProductStatus resolved = resolveStatus(product.getStatus(), status, product.getStockQuantity());
        product.setStatus(resolved);
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long id, Integer stockQuantity) {
        Product product = getProduct(id);
        product.setStockQuantity(stockQuantity);
        ProductStatus resolved = resolveStatus(product.getStatus(), null, stockQuantity);
        product.setStatus(resolved);
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(getProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String keyword,
                                        ProductStatus status,
                                        Long categoryId,
                                        BigDecimal minPrice,
                                        BigDecimal maxPrice,
                                        Integer minStock,
                                        Integer maxStock,
                                        Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return productRepository.search(
                normalizedKeyword,
                status,
                categoryId,
                minPrice,
                maxPrice,
                minStock,
                maxStock,
                pageable
        ).map(this::toResponse);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));
    }

    private ProductStatus resolveStatus(ProductStatus currentStatus, ProductStatus requestedStatus, Integer stockQuantity) {
        if (stockQuantity == null) {
            return currentStatus == null ? ProductStatus.ACTIVE : currentStatus;
        }
        if (stockQuantity <= 0) {
            return ProductStatus.OUT_OF_STOCK;
        }
        if (requestedStatus == null) {
            if (currentStatus == null) {
                return ProductStatus.ACTIVE;
            }
            return currentStatus == ProductStatus.OUT_OF_STOCK ? ProductStatus.ACTIVE : currentStatus;
        }
        if (requestedStatus == ProductStatus.OUT_OF_STOCK) {
            throw new BadRequestException("PRODUCT_STATUS_INVALID", "Cannot set OUT_OF_STOCK when stockQuantity > 0");
        }
        return requestedStatus;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        CategorySummary categorySummary = CategorySummary.builder()
                .id(category.getId())
                .name(category.getName())
                .build();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus())
                .category(categorySummary)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

