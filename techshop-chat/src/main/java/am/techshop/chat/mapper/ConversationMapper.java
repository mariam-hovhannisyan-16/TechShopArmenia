package am.techshop.chat.mapper;

import am.techshop.chat.entity.Conversation;
import am.techshop.common.dto.response.ConversationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationMapper {
    ConversationResponse toResponse(Conversation conversation);
}
