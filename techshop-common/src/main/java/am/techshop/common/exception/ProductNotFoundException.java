package am.techshop.common.exception;

import java.io.Serial;

public class ProductNotFoundException extends TechShopException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(Long id) {
        super("Product not found with id: %s".formatted(id), 404);
    }
}
