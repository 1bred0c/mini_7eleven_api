package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.CategoryRequest;
import congtuong.dev.mini_7eleven.dto.CategoryResponse;
import congtuong.dev.mini_7eleven.exception.NotFoundException;
import congtuong.dev.mini_7eleven.pojo.Category;
import congtuong.dev.mini_7eleven.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void shouldCreateCategorySuccessfully() {
        CategoryRequest request = CategoryRequest.builder()
                .name("  Drinks ")
                .description("Beverages")
                .build();

        Category saved = buildCategory("Drinks");
        saved.setDescription("Beverages");
        saved.setId(1L);

        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.create(request);

        assertEquals(1L, response.getId());
        assertEquals("Drinks", response.getName());
        assertEquals("Beverages", response.getDescription());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertEquals("Drinks", captor.getValue().getName());
    }

    @Test
    void shouldUpdateCategorySuccessfully() {
        Category existing = buildCategory("Snacks");
        existing.setId(2L);

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(existing)).thenReturn(existing);

        CategoryResponse response = categoryService.update(2L, CategoryRequest.builder()
                .name("  Healthy Snacks ")
                .description("Updated")
                .build());

        assertEquals(2L, response.getId());
        assertEquals("Healthy Snacks", response.getName());
        assertEquals("Updated", response.getDescription());
    }

    @Test
    void shouldGetCategoryDetailSuccessfully() {
        Category existing = buildCategory("Frozen");
        existing.setId(3L);

        when(categoryRepository.findById(3L)).thenReturn(Optional.of(existing));

        CategoryResponse response = categoryService.getById(3L);

        assertEquals(3L, response.getId());
        assertEquals("Frozen", response.getName());
    }

    @Test
    void shouldThrowExceptionWhenCategoryNotFound() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> categoryService.getById(9L));
    }

    @Test
    void shouldDeleteCategorySuccessfully() {
        Category existing = buildCategory("Old");
        existing.setId(4L);

        when(categoryRepository.findById(4L)).thenReturn(Optional.of(existing));

        categoryService.delete(4L);

        verify(categoryRepository).delete(existing);
    }

    private Category buildCategory(String name) {
        return Category.builder()
                .name(name)
                .description("Category")
                .build();
    }
}


