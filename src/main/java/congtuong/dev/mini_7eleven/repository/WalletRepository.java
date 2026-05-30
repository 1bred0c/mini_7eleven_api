package congtuong.dev.mini_7eleven.repository;

import congtuong.dev.mini_7eleven.pojo.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByAccountId(Long accountId);
}

