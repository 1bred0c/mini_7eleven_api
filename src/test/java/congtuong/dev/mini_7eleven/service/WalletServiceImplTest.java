package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.WalletResponse;
import congtuong.dev.mini_7eleven.dto.WalletTopUpRequest;
import congtuong.dev.mini_7eleven.enums.Role;
import congtuong.dev.mini_7eleven.enums.WalletTransactionType;
import congtuong.dev.mini_7eleven.exception.BadRequestException;
import congtuong.dev.mini_7eleven.pojo.Account;
import congtuong.dev.mini_7eleven.pojo.Wallet;
import congtuong.dev.mini_7eleven.pojo.WalletTransaction;
import congtuong.dev.mini_7eleven.repository.WalletRepository;
import congtuong.dev.mini_7eleven.repository.WalletTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void shouldCreateWalletSuccessfullyIfMissing() {
        Account account = buildAccount(1L, "user@example.com");
        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(walletRepository.findByAccountId(1L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId(100L);
            return wallet;
        });

        WalletResponse response = walletService.openWallet("user@example.com");

        assertEquals(100L, response.getId());
        assertEquals(BigDecimal.ZERO, response.getBalance());
    }

    @Test
    void shouldGetWalletByCurrentAccount() {
        Account account = buildAccount(2L, "user@example.com");
        Wallet wallet = buildWallet(200L, account, BigDecimal.valueOf(25));

        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(walletRepository.findByAccountId(2L)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getMyWallet("user@example.com");

        assertEquals(200L, response.getId());
        assertEquals(BigDecimal.valueOf(25), response.getBalance());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void shouldTopUpWalletSuccessfully() {
        Account account = buildAccount(3L, "user@example.com");
        Wallet wallet = buildWallet(300L, account, BigDecimal.valueOf(100));

        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(walletRepository.findByAccountId(3L)).thenReturn(Optional.of(wallet));

        WalletTopUpRequest request = WalletTopUpRequest.builder()
                .amount(BigDecimal.valueOf(50))
                .description("  Top up ")
                .build();

        WalletResponse response = walletService.topUp("user@example.com", request);

        assertEquals(BigDecimal.valueOf(150), response.getBalance());

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        WalletTransaction transaction = captor.getValue();
        assertEquals(WalletTransactionType.TOP_UP, transaction.getType());
        assertEquals(BigDecimal.valueOf(100), transaction.getBalanceBefore());
        assertEquals(BigDecimal.valueOf(150), transaction.getBalanceAfter());
        assertEquals("Top up", transaction.getDescription());
    }

    @Test
    void shouldRejectTopUpAmountLessThanOrEqualToZero() {
        Account account = buildAccount(4L, "user@example.com");
        Wallet wallet = buildWallet(400L, account, BigDecimal.valueOf(10));

        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(walletRepository.findByAccountId(4L)).thenReturn(Optional.of(wallet));

        WalletTopUpRequest request = WalletTopUpRequest.builder()
                .amount(BigDecimal.ZERO)
                .build();

        assertThrows(BadRequestException.class, () -> walletService.topUp("user@example.com", request));
    }

    @Test
    void shouldPayByWalletSuccessfully() {
        Wallet wallet = buildWallet(500L, buildAccount(5L, "user@example.com"), BigDecimal.valueOf(80));
        when(walletRepository.findByAccountId(5L)).thenReturn(Optional.of(wallet));

        walletService.payForOrder(5L, BigDecimal.valueOf(30), "Order #1");

        assertEquals(BigDecimal.valueOf(50), wallet.getBalance());

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        WalletTransaction transaction = captor.getValue();
        assertEquals(WalletTransactionType.PAYMENT, transaction.getType());
        assertEquals(BigDecimal.valueOf(80), transaction.getBalanceBefore());
        assertEquals(BigDecimal.valueOf(50), transaction.getBalanceAfter());
    }

    @Test
    void shouldRejectWalletPaymentWhenBalanceIsInsufficient() {
        Wallet wallet = buildWallet(600L, buildAccount(6L, "user@example.com"), BigDecimal.valueOf(10));
        when(walletRepository.findByAccountId(6L)).thenReturn(Optional.of(wallet));

        assertThrows(BadRequestException.class,
                () -> walletService.payForOrder(6L, BigDecimal.valueOf(20), "Order #2"));
    }

    @Test
    void shouldRefundToWalletSuccessfully() {
        Wallet wallet = buildWallet(700L, buildAccount(7L, "user@example.com"), BigDecimal.valueOf(40));
        when(walletRepository.findByAccountId(7L)).thenReturn(Optional.of(wallet));

        walletService.refundToWallet(7L, BigDecimal.valueOf(15), "Refund");

        assertEquals(BigDecimal.valueOf(55), wallet.getBalance());

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        WalletTransaction transaction = captor.getValue();
        assertEquals(WalletTransactionType.REFUND, transaction.getType());
        assertEquals(BigDecimal.valueOf(40), transaction.getBalanceBefore());
        assertEquals(BigDecimal.valueOf(55), transaction.getBalanceAfter());
    }

    private Account buildAccount(Long id, String email) {
        Account account = Account.builder()
                .fullName("User")
                .email(email)
                .passwordHash("hash")
                .role(Role.USER)
                .build();
        account.setId(id);
        return account;
    }

    private Wallet buildWallet(Long id, Account account, BigDecimal balance) {
        Wallet wallet = Wallet.builder()
                .account(account)
                .balance(balance)
                .build();
        wallet.setId(id);
        return wallet;
    }
}

