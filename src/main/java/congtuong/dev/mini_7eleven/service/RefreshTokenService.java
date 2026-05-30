package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.pojo.RefreshToken;

public interface RefreshTokenService {

    String issueRefreshToken(Long accountId);

    RefreshToken validateRefreshToken(String rawRefreshToken);

    void revokeRefreshToken(String rawRefreshToken);
}
