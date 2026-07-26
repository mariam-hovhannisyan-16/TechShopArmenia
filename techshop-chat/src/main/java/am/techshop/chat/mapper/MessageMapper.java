package am.techshop.chat.mapper;

import am.techshop.chat.entity.Message;
import am.techshop.common.dto.request.SendMessageRequest;
import am.techshop.common.dto.response.MessageResponse;
import am.techshop.common.enums.MessageSender;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    MessageResponse toResponse(Message message);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "text", source = "request.text")
    @Mapping(target = "read", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Message toEntity(Long conversationId, MessageSender sender, SendMessageRequest request);
}
