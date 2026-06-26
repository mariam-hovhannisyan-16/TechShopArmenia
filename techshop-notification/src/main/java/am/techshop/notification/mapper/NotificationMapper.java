package am.techshop.notification.mapper;

import am.techshop.common.dto.response.NotificationResponse;
import am.techshop.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}