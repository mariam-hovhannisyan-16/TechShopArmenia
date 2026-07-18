package am.techshop.notification.exception;

import am.techshop.common.dto.response.ErrorResponse;
import am.techshop.common.exception.TechShopException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NotificationExceptionHandler {

    @ExceptionHandler(TechShopException.class)
    public ResponseEntity<ErrorResponse> handleTechShopException(TechShopException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ErrorResponse.of("Error", ex.getMessage(), ex.getStatusCode()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("Internal Error", ex.getMessage(), 500));
    }
}