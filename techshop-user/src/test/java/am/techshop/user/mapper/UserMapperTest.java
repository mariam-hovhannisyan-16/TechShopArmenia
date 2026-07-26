package am.techshop.user.mapper;

import am.techshop.common.dto.request.RegisterRequest;
import am.techshop.common.enums.UserRole;
import am.techshop.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Test
    void toEntity_UsesResolvedRoleNotRequestRole() {

        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", null);

        User user = mapper.toEntity(request, "encoded", UserRole.CUSTOMER, "token-123", LocalDateTime.now().plusHours(24));

        assertEquals(UserRole.CUSTOMER, user.getRole());
        assertEquals("Mariam", user.getName());
        assertEquals("mariam@test.com", user.getEmail());
        assertEquals("encoded", user.getPassword());
        assertEquals("token-123", user.getVerificationToken());
        assertNull(user.getId());
    }

    @Test
    void toEntity_WhenRequestSpecifiesAdmin_ResolvedRoleStillWins() {

        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", UserRole.ADMIN);

        User user = mapper.toEntity(request, "encoded", UserRole.CUSTOMER, "token-123", LocalDateTime.now().plusHours(24));

        assertEquals(UserRole.CUSTOMER, user.getRole());
    }
}
