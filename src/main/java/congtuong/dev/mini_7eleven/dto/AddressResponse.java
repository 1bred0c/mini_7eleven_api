package congtuong.dev.mini_7eleven.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    private Long id;
    private Long accountId;
    private String receiverName;
    private String phoneNumber;
    private String addressLine;
    private String ward;
    private String district;
    private String city;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

