package am.techshop.user.mapper;

import am.techshop.common.dto.request.RegisterRequest;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.UserRole;
import am.techshop.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "role", source = "resolvedRole")
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "resetToken", ignore = true)
    @Mapping(target = "resetTokenExpiresAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest request, String encodedPassword, UserRole resolvedRole,
                  String verificationToken, LocalDateTime verificationTokenExpiresAt);
}