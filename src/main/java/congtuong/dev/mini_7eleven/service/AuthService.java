package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.*;

public interface AuthService {

	LoginResponse register(RegisterRequest request);

	LoginResponse login(LoginRequest request);

	LoginResponse refresh(RefreshTokenRequest request);

	void logout(RefreshTokenRequest request);

	MeResponse getMe(String email);
}
