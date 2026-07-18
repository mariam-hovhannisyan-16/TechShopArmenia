package am.techshop.common.exception;

import lombok.Getter;

@Getter
public class TechShopException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public TechShopException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}