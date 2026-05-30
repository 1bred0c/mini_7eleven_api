package congtuong.dev.mini_7eleven.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

	@NotBlank(message = "Email is required")
	@Email(message = "Email is invalid")
	private String email;

	@NotBlank(message = "Password is required")
	private String password;
}
