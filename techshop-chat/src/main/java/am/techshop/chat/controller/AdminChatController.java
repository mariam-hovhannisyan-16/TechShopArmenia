package am.techshop.chat.controller;

import am.techshop.chat.security.ChatIdentityResolver;
import am.techshop.chat.service.ChatService;
import am.techshop.common.dto.request.SendMessageRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.ConversationResponse;
import am.techshop.common.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Chat", description = "Admin endpoints for managing support conversations")
@SecurityRequirement(name = "bearerAuth")
public class AdminChatController {

    private final ChatService chatService;
    private final ChatIdentityResolver chatIdentityResolver;

    @GetMapping("/conversations")
    @Operation(summary = "List all support conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listConversations() {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getAllConversations()));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "List the messages in any conversation")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @Parameter(description = "ID of the conversation", required = true)
            @PathVariable @Positive Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessages(chatIdentityResolver.resolveAdmin(authentication), id)));
    }

    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "Reply to any conversation as support")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Parameter(description = "ID of the conversation", required = true)
            @PathVariable @Positive Long id,
            @RequestBody @Valid SendMessageRequest request,
            Authentication authentication) {
        MessageResponse saved = chatService.sendMessage(chatIdentityResolver.resolveAdmin(authentication), id, request).message();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Message sent", saved));
    }
}
