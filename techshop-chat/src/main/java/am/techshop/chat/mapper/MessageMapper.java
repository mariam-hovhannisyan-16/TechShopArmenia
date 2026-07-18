package am.techshop.chat.mapper;

import am.techshop.chat.entity.Message;
import am.techshop.common.dto.response.MessageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    MessageResponse toResponse(Message message);
}
