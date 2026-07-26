package am.techshop.common.exception;

import lombok.Getter;

import java.io.Serial;

@Getter
public class TechShopException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public TechShopException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}