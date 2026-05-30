package congtuong.dev.mini_7eleven.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }

    public UnauthorizedException(String message) {
        this("UNAUTHORIZED", message);
    }
}

