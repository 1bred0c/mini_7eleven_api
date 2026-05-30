 package congtuong.dev.mini_7eleven.controller;

import congtuong.dev.mini_7eleven.dto.WalletResponse;
import congtuong.dev.mini_7eleven.dto.WalletTopUpRequest;
import congtuong.dev.mini_7eleven.dto.WalletTransactionResponse;
import congtuong.dev.mini_7eleven.service.WalletService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/me/open")
    @PreAuthorize("hasRole('USER')")
    public WalletResponse openWallet(@AuthenticationPrincipal UserDetails principal) {
        return walletService.openWallet(principal.getUsername());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public WalletResponse getMyWallet(@AuthenticationPrincipal UserDetails principal) {
        return walletService.getMyWallet(principal.getUsername());
    }

    @PostMapping("/me/top-up")
    @PreAuthorize("hasRole('USER')")
    public WalletResponse topUp(@AuthenticationPrincipal UserDetails principal,
                                @Valid @RequestBody WalletTopUpRequest request) {
        return walletService.topUp(principal.getUsername(), request);
    }

    @GetMapping("/me/transactions")
    @PreAuthorize("hasRole('USER')")
    public Page<WalletTransactionResponse> getMyTransactions(@AuthenticationPrincipal UserDetails principal,
                                                             @PageableDefault(size = 20) Pageable pageable) {
        return walletService.getMyTransactions(principal.getUsername(), pageable);
    }
}
