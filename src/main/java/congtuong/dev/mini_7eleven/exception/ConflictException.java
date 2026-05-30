package congtuong.dev.mini_7eleven.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }

    public ConflictException(String message) {
        this("CONFLICT", message);
    }
}

