package congtuong.dev.mini_7eleven.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }

    public NotFoundException(String message) {
        this("NOT_FOUND", message);
    }
}

