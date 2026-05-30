package congtuong.dev.mini_7eleven.repository;

import congtuong.dev.mini_7eleven.enums.OrderStatus;
import congtuong.dev.mini_7eleven.enums.PaymentStatus;
import congtuong.dev.mini_7eleven.pojo.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "account")
    Page<Order> findByAccountId(Long accountId, Pageable pageable);

    @EntityGraph(attributePaths = "account")
    @Query("""
            select o from Order o
            where (:status is null or o.status = :status)
              and (:paymentStatus is null or o.paymentStatus = :paymentStatus)
              and (:accountId is null or o.account.id = :accountId)
            """)
    Page<Order> search(
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("accountId") Long accountId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"account", "items", "items.product"})
    java.util.Optional<Order> findWithItemsById(Long id);
}
