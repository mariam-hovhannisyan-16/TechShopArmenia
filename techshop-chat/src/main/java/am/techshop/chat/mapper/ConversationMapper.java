package am.techshop.chat.mapper;

import am.techshop.chat.entity.Conversation;
import am.techshop.chat.security.ChatIdentity;
import am.techshop.common.dto.response.ConversationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversationMapper {
    ConversationResponse toResponse(Conversation conversation);

    @Mapping(target = "escalated", source = "escalated")
    ConversationResponse toResponse(Conversation conversation, boolean escalated);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Conversation toEntity(ChatIdentity identity);
}
