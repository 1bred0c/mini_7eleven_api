package congtuong.dev.mini_7eleven.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
	private String tokenType;
	private String accessToken;
	private String refreshToken;
}
