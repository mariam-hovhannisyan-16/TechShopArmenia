package am.techshop.chat.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
@Validated
public class AdminChatController {

    private final ChatService chatService;
    private final ChatIdentityResolver chatIdentityResolver;

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listConversations() {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getAllConversations()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable @Positive Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessages(chatIdentityResolver.resolveAdmin(authentication), id)));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable @Positive Long id,
            @RequestBody @Valid SendMessageRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Message sent", chatService.sendMessage(chatIdentityResolver.resolveAdmin(authentication), id, request)));
    }
}
