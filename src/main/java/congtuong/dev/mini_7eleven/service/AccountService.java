package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.pojo.Account;

import java.util.Optional;

public interface AccountService {

    Account create(Account account);

    Optional<Account> findByEmail(String email);

    Account getByEmail(String email);

    Optional<Account> findById(Long id);

    Account getById(Long id);

    boolean existsByEmail(String email);
}

