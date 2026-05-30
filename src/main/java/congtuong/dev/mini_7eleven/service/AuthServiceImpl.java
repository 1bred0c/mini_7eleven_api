package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.*;
import congtuong.dev.mini_7eleven.enums.Role;
import congtuong.dev.mini_7eleven.exception.ConflictException;
import congtuong.dev.mini_7eleven.exception.UnauthorizedException;
import congtuong.dev.mini_7eleven.pojo.Account;
import congtuong.dev.mini_7eleven.pojo.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final AccountService accountService;
	private final RefreshTokenService refreshTokenService;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public LoginResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.getEmail());

		if (accountService.existsByEmail(email)) {
			throw new ConflictException("AUTH_EMAIL_EXISTS", "Email already exists");
		}

		Account account = Account.builder()
				.fullName(request.getFullName())
				.email(email)
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.role(Role.USER)
				.build();

		Account saved = accountService.create(account);

		String accessToken = jwtService.generateAccessToken(saved);
		String refreshToken = refreshTokenService.issueRefreshToken(saved.getId());

		return LoginResponse.builder()
				.tokenType("Bearer")
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.build();
	}

	@Override
	@Transactional
	public LoginResponse login(LoginRequest request) {
		String email = normalizeEmail(request.getEmail());

		Account account = accountService.findByEmail(email)
				.orElseThrow(() -> new UnauthorizedException("AUTH_INVALID_CREDENTIALS", "Invalid credentials"));

		if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
			throw new UnauthorizedException("AUTH_INVALID_CREDENTIALS", "Invalid credentials");
		}

		String accessToken = jwtService.generateAccessToken(account);
		String refreshToken = refreshTokenService.issueRefreshToken(account.getId());

		return LoginResponse.builder()
				.tokenType("Bearer")
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.build();
	}

	@Override
	@Transactional
	public LoginResponse refresh(RefreshTokenRequest request) {
		RefreshToken oldToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());

		// rotate refresh token
		refreshTokenService.revokeRefreshToken(request.getRefreshToken());

		Account account = oldToken.getAccount();

		String accessToken = jwtService.generateAccessToken(account);
		String newRefreshToken = refreshTokenService.issueRefreshToken(account.getId());

		return LoginResponse.builder()
				.tokenType("Bearer")
				.accessToken(accessToken)
				.refreshToken(newRefreshToken)
				.build();
	}

	@Override
	@Transactional
	public void logout(RefreshTokenRequest request) {
		refreshTokenService.revokeRefreshToken(request.getRefreshToken());
	}

	@Override
	@Transactional(readOnly = true)
	public MeResponse getMe(String email) {
		Account account = accountService.getByEmail(email);
		return MeResponse.builder()
				.id(account.getId())
				.fullName(account.getFullName())
				.email(account.getEmail())
				.role(account.getRole())
				.createdAt(account.getCreatedAt())
				.build();
	}

	private String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase();
	}
}
