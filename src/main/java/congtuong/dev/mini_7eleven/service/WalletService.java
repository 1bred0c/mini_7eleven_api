package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.WalletResponse;
import congtuong.dev.mini_7eleven.dto.WalletTopUpRequest;
import congtuong.dev.mini_7eleven.dto.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface WalletService {

    WalletResponse openWallet(String email);

    WalletResponse getMyWallet(String email);

    WalletResponse topUp(String email, WalletTopUpRequest request);

    Page<WalletTransactionResponse> getMyTransactions(String email, Pageable pageable);

    void payForOrder(Long accountId, BigDecimal amount, String description);

    void refundToWallet(Long accountId, BigDecimal amount, String description);
}
