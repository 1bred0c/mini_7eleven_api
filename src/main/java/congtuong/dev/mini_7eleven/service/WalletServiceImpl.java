package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.WalletResponse;
import congtuong.dev.mini_7eleven.dto.WalletTopUpRequest;
import congtuong.dev.mini_7eleven.dto.WalletTransactionResponse;
import congtuong.dev.mini_7eleven.enums.WalletTransactionType;
import congtuong.dev.mini_7eleven.exception.BadRequestException;
import congtuong.dev.mini_7eleven.pojo.Account;
import congtuong.dev.mini_7eleven.pojo.Wallet;
import congtuong.dev.mini_7eleven.pojo.WalletTransaction;
import congtuong.dev.mini_7eleven.repository.WalletRepository;
import congtuong.dev.mini_7eleven.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AccountService accountService;

    @Override
    @Transactional
    public WalletResponse openWallet(String email) {
        Account account = accountService.getByEmail(email);
        Wallet wallet = getOrCreateWallet(account);
        return toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(String email) {
        Account account = accountService.getByEmail(email);
        Wallet wallet = getOrCreateWallet(account);
        return toResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse topUp(String email, WalletTopUpRequest request) {
        Account account = accountService.getByEmail(email);
        Wallet wallet = getOrCreateWallet(account);
        BigDecimal amount = request.getAmount();
        ensurePositiveAmount(amount);

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        wallet.setBalance(after);

        walletRepository.save(wallet);
        createTransaction(wallet, WalletTransactionType.TOP_UP, amount, before, after, request.getDescription());
        return toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getMyTransactions(String email, Pageable pageable) {
        Account account = accountService.getByEmail(email);
        Wallet wallet = getOrCreateWallet(account);
        return walletTransactionRepository.findByWalletId(wallet.getId(), pageable)
                .map(this::toTransactionResponse);
    }

    @Override
    @Transactional
    public void payForOrder(Long accountId, BigDecimal amount, String description) {
        Wallet wallet = getWalletByAccountId(accountId);
        ensurePositiveAmount(amount);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("WALLET_INSUFFICIENT_BALANCE", "Insufficient wallet balance");
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.subtract(amount);
        wallet.setBalance(after);

        walletRepository.save(wallet);
        createTransaction(wallet, WalletTransactionType.PAYMENT, amount, before, after, description);
    }

    @Override
    @Transactional
    public void refundToWallet(Long accountId, BigDecimal amount, String description) {
        Wallet wallet = getWalletByAccountId(accountId);
        ensurePositiveAmount(amount);

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        wallet.setBalance(after);

        walletRepository.save(wallet);
        createTransaction(wallet, WalletTransactionType.REFUND, amount, before, after, description);
    }

    private Wallet getWalletByAccountId(Long accountId) {
        return walletRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BadRequestException("WALLET_NOT_FOUND", "Wallet not found"));
    }

    private Wallet getOrCreateWallet(Account account) {
        return walletRepository.findByAccountId(account.getId())
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .account(account)
                        .balance(BigDecimal.ZERO)
                        .build()));
    }

    private void ensurePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("WALLET_INVALID_AMOUNT", "Amount must be greater than 0");
        }
    }

    private void createTransaction(Wallet wallet,
                                   WalletTransactionType type,
                                   BigDecimal amount,
                                   BigDecimal before,
                                   BigDecimal after,
                                   String description) {
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .description(normalizeDescription(description))
                .build();
        walletTransactionRepository.save(transaction);
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .accountId(wallet.getAccount().getId())
                .balance(wallet.getBalance())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction transaction) {
        return WalletTransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
