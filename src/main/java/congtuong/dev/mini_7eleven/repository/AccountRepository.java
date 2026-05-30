package congtuong.dev.mini_7eleven.repository;

import congtuong.dev.mini_7eleven.pojo.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByEmail(String email);

	boolean existsByEmail(String email);
}
