package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.videocall.dto.ChatMessageDto;
import coffeandcommit.crema.domain.videocall.dto.request.ChatHistorySaveRequest;
import coffeandcommit.crema.domain.videocall.dto.response.ChatHistoryResponse;
import coffeandcommit.crema.domain.videocall.entity.SessionChatLog;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.exception.ChatNotFoundException;
import coffeandcommit.crema.domain.videocall.exception.ChatSaveFailedException;
import coffeandcommit.crema.domain.videocall.exception.SessionNotFoundException;
import coffeandcommit.crema.domain.videocall.repository.SessionChatLogRepository;
import coffeandcommit.crema.domain.videocall.repository.VideoSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final SessionChatLogRepository sessionChatLogRepository;
    private final VideoSessionRepository videoSessionRepository;
    private final ObjectMapper objectMapper;

    public void saveChatHistory(String sessionId, ChatHistorySaveRequest request) {
        try {
            VideoSession videoSession = videoSessionRepository.findBySessionId(sessionId)
                    .orElseThrow(SessionNotFoundException::new);

            String chatMessagesJson = objectMapper.writeValueAsString(request.getMessages());
            
            SessionChatLog existingChatLog = sessionChatLogRepository.findBySessionId(sessionId)
                    .orElse(null);

            if (existingChatLog != null) {
                existingChatLog.updateChatHistory(
                    chatMessagesJson, 
                    request.getMessages().size(), 
                    request.getSessionEndTime()
                );
                sessionChatLogRepository.save(existingChatLog);
            } else {
                SessionChatLog chatLog = SessionChatLog.builder()
                        .sessionId(sessionId)
                        .chatMessages(chatMessagesJson)
                        .totalMessages(request.getMessages().size())
                        .sessionStartTime(request.getSessionStartTime())
                        .sessionEndTime(request.getSessionEndTime())
                        .videoSession(videoSession)
                        .build();
                        
                sessionChatLogRepository.save(chatLog);
            }

            log.info("채팅 기록 저장 완료: sessionId={}, messageCount={}", 
                    sessionId, request.getMessages().size());
                    
        } catch (JsonProcessingException e) {
            log.error("채팅 메시지 JSON 변환 실패: sessionId={}", sessionId, e);
            throw new ChatSaveFailedException();
        } catch (Exception e) {
            log.error("채팅 기록 저장 실패: sessionId={}", sessionId, e);
            throw new ChatSaveFailedException();
        }
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(String sessionId) {
        try {
            SessionChatLog chatLog = sessionChatLogRepository.findBySessionId(sessionId)
                    .orElseThrow(ChatNotFoundException::new);

            List<ChatMessageDto> messages = objectMapper.readValue(
                chatLog.getChatMessages(), 
                new TypeReference<List<ChatMessageDto>>() {}
            );

            return ChatHistoryResponse.builder()
                    .sessionId(sessionId)
                    .messages(messages)
                    .totalMessages(chatLog.getTotalMessages())
                    .sessionStartTime(chatLog.getSessionStartTime())
                    .sessionEndTime(chatLog.getSessionEndTime())
                    .createdAt(chatLog.getCreatedAt())
                    .build();
                    
        } catch (JsonProcessingException e) {
            log.error("채팅 메시지 JSON 파싱 실패: sessionId={}", sessionId, e);
            throw new ChatNotFoundException();
        } catch (Exception e) {
            log.error("채팅 기록 조회 실패: sessionId={}", sessionId, e);
            throw new ChatNotFoundException();
        }
    }
}