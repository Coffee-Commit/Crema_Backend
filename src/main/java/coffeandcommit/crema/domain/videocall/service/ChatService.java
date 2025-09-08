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
import coffeandcommit.crema.domain.reservation.enums.Status;
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

    @Transactional
public void saveChatHistory(String sessionId, ChatHistorySaveRequest request, String username) {
    try {
        // 권한 검증
        validateSavePermission(sessionId, username);
        
        VideoSession videoSession = videoSessionRepository.findBySessionId(sessionId)
                .orElseThrow(SessionNotFoundException::new);

        String chatMessagesJson = objectMapper.writeValueAsString(request.getMessages());
        
        // 멱등성 체크 - 동일한 내용이면 건너뛰기
        SessionChatLog existingChatLog = sessionChatLogRepository.findBySessionId(sessionId)
                .orElse(null);

        if (existingChatLog != null) {
            // 동일한 내용인지 확인 (간단한 메시지 개수 및 크기 비교)
            if (existingChatLog.getTotalMessages() != null && 
                existingChatLog.getTotalMessages().equals(request.getMessages().size()) &&
                existingChatLog.getChatMessages() != null &&
                existingChatLog.getChatMessages().length() == chatMessagesJson.length()) {
                log.info("채팅 기록 중복 저장 방지: sessionId={}, messageCount={}", 
                        sessionId, request.getMessages().size());
                return; // 멱등성 - 동일한 내용은 재저장하지 않음
            }
            
            existingChatLog.updateChatHistoryWithMetadata(
                chatMessagesJson, 
                request.getMessages().size(), 
                request.getSessionEndTime(),
                username
            );
            sessionChatLogRepository.save(existingChatLog);
            log.info("채팅 기록 업데이트 완료: sessionId={}, messageCount={}, savedBy={}", 
                    sessionId, request.getMessages().size(), username);
        } else {
            SessionChatLog chatLog = SessionChatLog.builder()
                    .sessionId(sessionId)
                    .chatMessages(chatMessagesJson)
                    .totalMessages(request.getMessages().size())
                    .sessionStartTime(request.getSessionStartTime())
                    .sessionEndTime(request.getSessionEndTime())
                    .savedBy(username)
                    .videoSession(videoSession)
                    .build();
                    
            sessionChatLogRepository.save(chatLog);
            log.info("채팅 기록 저장 완료: sessionId={}, messageCount={}, savedBy={}", 
                    sessionId, request.getMessages().size(), username);
        }

        // 연관된 예약의 상태를 COMPLETED로 변경 (별도 메서드로 분리)
        completeRelatedReservation(videoSession, sessionId);
                
    } catch (SecurityException e) {
        log.error("채팅 저장 권한 없음: sessionId={}, username={}", sessionId, username);
        throw e;
    } catch (JsonProcessingException e) {
        log.error("채팅 메시지 JSON 변환 실패: sessionId={}", sessionId, e);
        throw new ChatSaveFailedException();
    } catch (Exception e) {
        log.error("채팅 기록 저장 실패: sessionId={}", sessionId, e);
        throw new ChatSaveFailedException();
    }
}

    @Transactional(readOnly = true)
public ChatHistoryResponse getChatHistory(String sessionId, String username) {
    try {
        // 권한 검증
        validateReadPermission(sessionId, username);
        
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
                
    } catch (SecurityException e) {
        log.error("채팅 조회 권한 없음: sessionId={}, username={}", sessionId, username);
        throw e;
    } catch (JsonProcessingException e) {
        log.error("채팅 메시지 JSON 파싱 실패: sessionId={}", sessionId, e);
        throw new ChatNotFoundException();
    } catch (Exception e) {
        log.error("채팅 기록 조회 실패: sessionId={}", sessionId, e);
        throw new ChatNotFoundException();
    }
}

    /**
     * 사용자가 해당 세션에 접근할 권한이 있는지 확인
     * @param sessionId 세션 ID
     * @param username 사용자명
     * @return 권한이 있으면 true, 없으면 false
     */
    @Transactional(readOnly = true)
    public boolean hasSessionAccess(String sessionId, String username) {
        try {
            VideoSession videoSession = videoSessionRepository.findBySessionId(sessionId)
                    .orElse(null);
            
            if (videoSession == null) {
                log.debug("세션을 찾을 수 없음: sessionId={}", sessionId);
                return false;
            }
            
            // 해당 세션에 참여한 적이 있는 사용자인지 확인
            boolean hasParticipated = videoSession.getParticipants().stream()
                    .anyMatch(participant -> username.equals(participant.getUsername()));
            
            if (hasParticipated) {
                log.debug("세션 접근 권한 확인됨: sessionId={}, username={}", sessionId, username);
                return true;
            }
            
            log.debug("세션 접근 권한 없음: sessionId={}, username={}", sessionId, username);
            return false;
            
        } catch (Exception e) {
            log.error("세션 접근 권한 확인 실패: sessionId={}, username={}", sessionId, username, e);
            return false;
        }
    }
    
    /**
     * 채팅 기록 저장 권한 검증 (세션 참여자만 가능)
     */
    public void validateSavePermission(String sessionId, String username) {
        if (!hasSessionAccess(sessionId, username)) {
            log.error("채팅 저장 권한 없음: sessionId={}, username={}", sessionId, username);
            throw new SecurityException("해당 세션의 채팅을 저장할 권한이 없습니다.");
        }
    }
    
    /**
     * 채팅 기록 조회 권한 검증 (세션 참여자만 가능)
     */
    public void validateReadPermission(String sessionId, String username) {
        if (!hasSessionAccess(sessionId, username)) {
            log.error("채팅 조회 권한 없음: sessionId={}, username={}", sessionId, username);
            throw new SecurityException("해당 세션의 채팅을 조회할 권한이 없습니다.");
        }
    }

    
    /**
     * 연관된 예약의 상태를 COMPLETED로 변경하는 별도 메서드
     */
    private void completeRelatedReservation(VideoSession videoSession, String sessionId) {
        try {
            if (videoSession.getReservation() != null) {
                if (videoSession.getReservation().getStatus() != Status.COMPLETED) {
                    videoSession.getReservation().completeReservation();
                    log.info("예약 상태를 COMPLETED로 변경: reservationId={}, sessionId={}", 
                            videoSession.getReservation().getId(), sessionId);
                } else {
                    log.debug("예약이 이미 완료 상태입니다: reservationId={}, sessionId={}", 
                            videoSession.getReservation().getId(), sessionId);
                }
            } else {
                log.warn("세션에 연결된 예약이 없습니다: sessionId={}", sessionId);
            }
        } catch (Exception e) {
            log.error("예약 상태 변경 실패: sessionId={}, error={}", sessionId, e.getMessage());
            // 예약 상태 변경 실패가 채팅 저장을 방해하지 않도록 예외를 삼킴
        }
    }

}