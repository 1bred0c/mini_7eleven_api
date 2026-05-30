package congtuong.dev.mini_7eleven.repository;

import congtuong.dev.mini_7eleven.pojo.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}

