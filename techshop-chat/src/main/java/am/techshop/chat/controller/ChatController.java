package am.techshop.chat.controller;

import am.techshop.chat.security.ChatIdentity;
import am.techshop.chat.service.ChatService;
import am.techshop.common.dto.request.SendMessageRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.ConversationResponse;
import am.techshop.common.dto.response.MessageResponse;
import am.techshop.common.exception.TechShopException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private static final String GUEST_SESSION_HEADER = "X-Guest-Session-Id";

    private final ChatService chatService;

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            Authentication authentication,
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId) {
        ChatIdentity identity = resolveIdentity(authentication, guestSessionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(chatService.getOrCreateConversation(identity)));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listConversations() {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getAllConversations()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable @Positive Long id,
            Authentication authentication,
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId) {
        ChatIdentity identity = resolveIdentity(authentication, guestSessionId);
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessages(identity, id)));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable @Positive Long id,
            @RequestBody @Valid SendMessageRequest request,
            Authentication authentication,
            @RequestHeader(value = GUEST_SESSION_HEADER, required = false) String guestSessionId) {
        ChatIdentity identity = resolveIdentity(authentication, guestSessionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Message sent", chatService.sendMessage(identity, id, request)));
    }

    private ChatIdentity resolveIdentity(Authentication authentication, String guestSessionId) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);
            return new ChatIdentity(userId, null, isAdmin);
        }
        if (guestSessionId != null && !guestSessionId.isBlank()) {
            return new ChatIdentity(null, guestSessionId, false);
        }
        throw new TechShopException("Authentication or X-Guest-Session-Id header is required", 400);
    }
}
