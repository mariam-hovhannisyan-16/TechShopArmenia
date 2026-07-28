package am.techshop.chat.dto;

import am.techshop.common.dto.response.MessageResponse;

public record SendMessageResult(MessageResponse message, MessageResponse botReply) {
}
