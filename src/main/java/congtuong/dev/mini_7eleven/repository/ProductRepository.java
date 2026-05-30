package congtuong.dev.mini_7eleven.repository;

import congtuong.dev.mini_7eleven.enums.ProductStatus;
import congtuong.dev.mini_7eleven.pojo.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import java.math.BigDecimal;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where (:status is null or p.status = :status)
              and (:categoryId is null or p.category.id = :categoryId)
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
              and (:minStock is null or p.stockQuantity >= :minStock)
              and (:maxStock is null or p.stockQuantity <= :maxStock)
              and (
                   :keyword is null
                   or lower(cast(p.name as string)) like concat('%', lower(cast(:keyword as string)), '%')
                   or lower(cast(p.description as string)) like concat('%', lower(cast(:keyword as string)), '%')
              )
            """)
    Page<Product> search(
            @Param("keyword") String keyword,
            @Param("status") ProductStatus status,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minStock") Integer minStock,
            @Param("maxStock") Integer maxStock,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    java.util.Optional<Product> findById(Long id);
}
