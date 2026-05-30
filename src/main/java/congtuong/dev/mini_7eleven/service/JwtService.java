package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.pojo.Account;

public interface JwtService {

    String generateAccessToken(Account account);

    String extractEmail(String token);

    boolean isTokenValid(String token);
}
