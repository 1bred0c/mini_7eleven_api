package congtuong.dev.mini_7eleven.repository;

import congtuong.dev.mini_7eleven.pojo.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}

