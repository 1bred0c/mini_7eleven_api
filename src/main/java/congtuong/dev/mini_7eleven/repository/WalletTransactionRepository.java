package congtuong.dev.mini_7eleven.repository;

import congtuong.dev.mini_7eleven.pojo.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletId(Long walletId, Pageable pageable);
}

