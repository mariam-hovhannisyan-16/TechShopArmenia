package am.techshop.user.mapper;

import am.techshop.common.dto.response.UserResponse;
import am.techshop.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}