package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.videocall.dto.request.ChatMessageRequest;
import coffeandcommit.crema.domain.videocall.dto.response.ChatMessageResponse;
import coffeandcommit.crema.domain.videocall.entity.ChatMessage;
import coffeandcommit.crema.domain.videocall.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageResponse saveMessage(ChatMessageRequest request) {
        log.info("채팅 메시지 저장: sessionId={}, username={}", request.getSessionId(), request.getUsername());
        
        ChatMessage chatMessage = ChatMessage.builder()
                .sessionId(request.getSessionId())
                .username(request.getUsername())
                .message(request.getMessage())
                .build();
        
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        
        log.info("채팅 메시지 저장 완료: id={}", savedMessage.getId());
        
        return ChatMessageResponse.from(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesBySessionId(String sessionId) {
        log.info("세션 채팅 메시지 조회: sessionId={}", sessionId);
        
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        log.info("세션 채팅 메시지 조회 완료: sessionId={}, count={}", sessionId, messages.size());
        
        return messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getAllMessages() {
        log.info("모든 채팅 메시지 조회");
        
        List<ChatMessage> messages = chatMessageRepository.findAllByOrderByCreatedAtDesc();
        
        log.info("모든 채팅 메시지 조회 완료: count={}", messages.size());
        
        return messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }
}