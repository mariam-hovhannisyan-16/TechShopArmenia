package am.techshop.common.exception;

public class ProductNotFoundException extends TechShopException {

    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(String message) {
        super(message, 404);
    }
}
