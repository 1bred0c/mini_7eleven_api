package congtuong.dev.mini_7eleven.security;

import congtuong.dev.mini_7eleven.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        String normalized = email.trim().toLowerCase();
        return accountRepository.findByEmail(normalized)
                .map(AccountUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }
}
