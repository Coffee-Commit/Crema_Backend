package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.request.ChatMessageRequest;
import coffeandcommit.crema.domain.videocall.dto.response.ChatMessageResponse;
import coffeandcommit.crema.domain.videocall.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "채팅", description = "화상통화 채팅 API")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/messages")
    @Operation(summary = "채팅 메시지 저장", description = "채팅 메시지를 데이터베이스에 저장합니다.")
    public ResponseEntity<ChatMessageResponse> saveMessage(
            @Valid @RequestBody ChatMessageRequest request) {
        
        log.info("채팅 메시지 저장 요청: sessionId={}, username={}", request.getSessionId(), request.getUsername());
        
        ChatMessageResponse response = chatService.saveMessage(request);
        
        log.info("채팅 메시지 저장 완료: id={}", response.getId());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "세션 채팅 메시지 조회", description = "특정 세션의 모든 채팅 메시지를 조회합니다.")
    public ResponseEntity<List<ChatMessageResponse>> getMessagesBySessionId(
            @PathVariable String sessionId) {
        
        log.info("세션 채팅 메시지 조회 요청: sessionId={}", sessionId);
        
        List<ChatMessageResponse> messages = chatService.getMessagesBySessionId(sessionId);
        
        log.info("세션 채팅 메시지 조회 완료: sessionId={}, count={}", sessionId, messages.size());
        
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/messages/all")
    @Operation(summary = "모든 채팅 메시지 조회", description = "데이터베이스에 저장된 모든 채팅 메시지를 조회합니다. (디버깅용)")
    public ResponseEntity<List<ChatMessageResponse>> getAllMessages() {
        
        log.info("모든 채팅 메시지 조회 요청");
        
        List<ChatMessageResponse> messages = chatService.getAllMessages();
        
        log.info("모든 채팅 메시지 조회 완료: count={}", messages.size());
        
        return ResponseEntity.ok(messages);
    }
}