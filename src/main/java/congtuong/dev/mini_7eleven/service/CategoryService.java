package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.CategoryRequest;
import congtuong.dev.mini_7eleven.dto.CategoryResponse;
import congtuong.dev.mini_7eleven.pojo.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    CategoryResponse getById(Long id);

    Page<CategoryResponse> getAll(Pageable pageable);

    Category getEntity(Long id);
}

