package am.techshop.notification.mapper;

import am.techshop.common.dto.response.NotificationResponse;
import am.techshop.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "read", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Notification toEntity(Long userId, String message);
}