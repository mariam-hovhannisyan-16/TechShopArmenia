package am.techshop.common.exception;

import java.io.Serial;

public class UserNotFoundException extends TechShopException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserNotFoundException(Long id) {
        super("User not found with id: %s".formatted(id), 404);
    }

    private UserNotFoundException(String message) {
        super(message, 404);
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User not found with email: %s".formatted(email));
    }
}