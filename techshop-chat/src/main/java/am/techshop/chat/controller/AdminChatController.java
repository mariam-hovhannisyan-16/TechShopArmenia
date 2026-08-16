package am.techshop.chat.controller;

import am.techshop.chat.service.ChatService;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.ConversationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
@Tag(name = "Admin Chat", description = "Admin endpoints for managing support conversations")
@SecurityRequirement(name = "bearerAuth")
public class AdminChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    @Operation(summary = "List all support conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listConversations() {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getAllConversations()));
    }
}
