package congtuong.dev.mini_7eleven.exception;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String code;
    private String path;

    /**
     * Field -> message, only present for validation errors.
     */
    private Map<String, String> errors;
}

