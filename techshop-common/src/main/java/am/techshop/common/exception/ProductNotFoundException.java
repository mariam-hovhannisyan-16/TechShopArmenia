package am.techshop.common.exception;

public class ProductNotFoundException extends TechShopException {

        public ProductNotFoundException(String message) {
            super(message, 404);
        }
    }