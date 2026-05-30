package congtuong.dev.mini_7eleven.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "Receiver name is required")
    @Size(max = 100, message = "Receiver name must be at most 100 characters")
    private String receiverName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phoneNumber;

    @NotBlank(message = "Address line is required")
    @Size(max = 255, message = "Address line must be at most 255 characters")
    private String addressLine;

    @Size(max = 100, message = "Ward must be at most 100 characters")
    private String ward;

    @Size(max = 100, message = "District must be at most 100 characters")
    private String district;

    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    private Boolean isDefault;
}

