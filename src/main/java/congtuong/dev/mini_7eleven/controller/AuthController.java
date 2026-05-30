package congtuong.dev.mini_7eleven.controller;

import congtuong.dev.mini_7eleven.dto.*;
import congtuong.dev.mini_7eleven.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return authService.refresh(request);
	}

	@PostMapping("/logout")
	public void logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
	}

	@GetMapping("/me")
	@SecurityRequirement(name = "bearerAuth")
	public MeResponse getMe(@AuthenticationPrincipal UserDetails principal) {
		return authService.getMe(principal.getUsername());
	}
}
