package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.ProductCreateRequest;
import congtuong.dev.mini_7eleven.dto.ProductResponse;
import congtuong.dev.mini_7eleven.enums.ProductStatus;
import congtuong.dev.mini_7eleven.exception.BadRequestException;
import congtuong.dev.mini_7eleven.exception.NotFoundException;
import congtuong.dev.mini_7eleven.pojo.Category;
import congtuong.dev.mini_7eleven.pojo.Product;
import congtuong.dev.mini_7eleven.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void shouldCreateProductSuccessfully() {
        Category category = buildCategory(5L, "Drinks");
        when(categoryService.getEntity(5L)).thenReturn(category);

        Product saved = buildProduct(10L, "Coke", BigDecimal.valueOf(1.5), 10, ProductStatus.ACTIVE, category);
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("  Coke ")
                .description("Cola")
                .price(BigDecimal.valueOf(1.5))
                .stockQuantity(10)
                .imageUrl("img")
                .categoryId(5L)
                .build();

        ProductResponse response = productService.create(request);

        assertEquals(10L, response.getId());
        assertEquals("Coke", response.getName());
        assertEquals(ProductStatus.ACTIVE, response.getStatus());
        assertEquals(5L, response.getCategory().getId());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("Coke", captor.getValue().getName());
        assertEquals(ProductStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void shouldRejectProductCreationWhenCategoryNotFound() {
        when(categoryService.getEntity(5L)).thenThrow(new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));

        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Coke")
                .price(BigDecimal.ONE)
                .stockQuantity(1)
                .categoryId(5L)
                .build();

        assertThrows(NotFoundException.class, () -> productService.create(request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.getById(99L));
    }

    @Test
    void shouldDeactivateProductSuccessfully() {
        Category category = buildCategory(3L, "Snacks");
        Product product = buildProduct(7L, "Chips", BigDecimal.valueOf(2.0), 5, ProductStatus.ACTIVE, category);

        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = productService.updateStatus(7L, ProductStatus.INACTIVE);

        assertEquals(ProductStatus.INACTIVE, response.getStatus());
    }

    @Test
    void shouldUpdateStockAndMarkOutOfStock() {
        Category category = buildCategory(3L, "Snacks");
        Product product = buildProduct(8L, "Crackers", BigDecimal.valueOf(3.0), 2, ProductStatus.ACTIVE, category);

        when(productRepository.findById(8L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = productService.updateStock(8L, 0);

        assertEquals(0, response.getStockQuantity());
        assertEquals(ProductStatus.OUT_OF_STOCK, response.getStatus());
    }

    @Test
    void shouldRejectOutOfStockStatusWhenStockIsPositive() {
        Category category = buildCategory(2L, "Drinks");
        when(categoryService.getEntity(2L)).thenReturn(category);

        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Tea")
                .price(BigDecimal.valueOf(1.0))
                .stockQuantity(5)
                .status(ProductStatus.OUT_OF_STOCK)
                .categoryId(2L)
                .build();

        assertThrows(BadRequestException.class, () -> productService.create(request));
    }

    private Category buildCategory(Long id, String name) {
        Category category = Category.builder()
                .name(name)
                .description("Category")
                .build();
        category.setId(id);
        return category;
    }

    private Product buildProduct(Long id, String name, BigDecimal price, int stock, ProductStatus status, Category category) {
        Product product = Product.builder()
                .name(name)
                .description("Desc")
                .price(price)
                .stockQuantity(stock)
                .imageUrl("img")
                .status(status)
                .category(category)
                .build();
        product.setId(id);
        return product;
    }
}

