package am.techshop.common.exception;

public class UserNotFoundException extends TechShopException {

    public UserNotFoundException(Long id) {
        super("User not found with id: " + id, 404);
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }

    private UserNotFoundException(String message) {
        super(message, 404);
    }
}