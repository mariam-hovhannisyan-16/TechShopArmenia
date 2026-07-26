package am.techshop.chat.controller;

import am.techshop.chat.security.ChatIdentity;
import am.techshop.chat.security.ChatIdentityResolver;
import am.techshop.chat.service.ChatService;
import am.techshop.common.dto.request.SendMessageRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.ConversationResponse;
import am.techshop.common.dto.response.MessageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat/conversations")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private static final String GUEST_SESSION_HEADER = "X-Guest-Session-Id";

    private final ChatService chatService;
    private final ChatIdentityResolver chatIdentityResolver;

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            Authentication authentication,
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId) {
        ChatIdentity identity = chatIdentityResolver.resolve(authentication, guestSessionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(chatService.getOrCreateConversation(identity)));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable @Positive Long id,
            Authentication authentication,
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId) {
        ChatIdentity identity = chatIdentityResolver.resolve(authentication, guestSessionId);
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessages(identity, id)));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable @Positive Long id,
            @RequestBody @Valid SendMessageRequest request,
            Authentication authentication,
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId) {
        ChatIdentity identity = chatIdentityResolver.resolve(authentication, guestSessionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Message sent", chatService.sendMessage(identity, id, request)));
    }
}
