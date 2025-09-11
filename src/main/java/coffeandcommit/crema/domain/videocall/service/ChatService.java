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
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
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
    private final ReservationRepository reservationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
public void saveChatHistory(String sessionId, ChatHistorySaveRequest request, String username) {
    try {
        // 권한 검증
        validateSavePermission(sessionId, username);
        
        VideoSession videoSession = videoSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("채팅 저장용 세션 ID: " + sessionId + "를 찾을 수 없습니다"));

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
                
    } catch (IllegalArgumentException e) {
        log.error("잘못된 입력 값: sessionId={}, username={}, error={}", sessionId, username, e.getMessage());
        throw e;
    } catch (SecurityException e) {
        log.error("채팅 저장 권한 없음: sessionId={}, username={}", sessionId, username);
        throw e;
    } catch (JsonProcessingException e) {
        log.error("채팅 메시지 JSON 변환 실패: sessionId={}", sessionId, e);
        throw new ChatSaveFailedException("채팅 메시지 JSON 직렬화 실패 - 세션 ID: " + sessionId + ", 메시지 수: " + request.getMessages().size());
    } catch (SessionNotFoundException | ChatNotFoundException e) {
        log.error("데이터를 찾을 수 없음: sessionId={}, error={}", sessionId, e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("채팅 기록 저장 실패: sessionId={}", sessionId, e);
        throw new ChatSaveFailedException("채팅 기록 저장 중 예상치 못한 오류 - 세션 ID: " + sessionId + ", 사용자: " + username + ", 원인: " + e.getMessage());
    }
}

    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(String reservationId, String username) {
    try {
        // reservationId 유효성 검증
        Long parsedReservationId;
        try {
            parsedReservationId = Long.valueOf(reservationId);
        } catch (NumberFormatException e) {
            log.error("잘못된 예약 ID 형식: reservationId={}", reservationId);
            throw new ChatNotFoundException("잘못된 예약 ID 형식입니다.");
        }
        
        // Reservation 조회
        Reservation reservation = reservationRepository.findById(parsedReservationId)
                .orElseThrow(() -> new ChatNotFoundException("채팅 기록을 조회할 수 없습니다. 예약을 찾을 수 없습니다."));
        
        // 권한 검증 - Reservation의 member로 확인
        validateReservationReadPermission(reservation, username);
        
        // VideoSession에서 sessionId 추출
        VideoSession videoSession = reservation.getVideoSession();
        if (videoSession == null) {
            log.error("예약에 연결된 화상통화 세션이 없음: reservationId={}", reservation.getId());
            throw new SessionNotFoundException("해당 예약에 연결된 화상통화 세션이 없습니다.");
        }
        
        if (videoSession.getSessionId() == null || videoSession.getSessionId().trim().isEmpty()) {
            log.error("비어있는 세션 ID: reservationId={}", reservation.getId());
            throw new SessionNotFoundException("유효하지 않은 세션 ID입니다.");
        }
        
        String sessionId = videoSession.getSessionId();
        
        SessionChatLog chatLog = sessionChatLogRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ChatNotFoundException("세션 " + sessionId + "의 채팅 기록을 찾을 수 없습니다"));

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
        log.error("채팅 조회 권한 없음: reservationId={}, username={}", reservationId, username);
        throw e;
    } catch (JsonProcessingException e) {
        log.error("채팅 메시지 JSON 파싱 실패: reservationId={}", reservationId, e);
        throw new ChatNotFoundException("예약 ID " + reservationId + "의 채팅 메시지 JSON 파싱 실패: " + e.getMessage());
    } catch (Exception e) {
        log.error("채팅 기록 조회 실패: reservationId={}", reservationId, e);
        throw new ChatNotFoundException("예약 ID " + reservationId + "의 채팅 기록 조회 실패: " + e.getMessage());
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
            // null 체크 후 participants 검사
            if (videoSession.getParticipants() == null) {
                log.debug("세션에 참여자 정보가 없음: sessionId={}", sessionId);
                return false;
            }
            
            boolean hasParticipated = videoSession.getParticipants().stream()
                    .filter(participant -> participant != null && participant.getUsername() != null)
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
     * 채팅 기록 저장 권한 검증 (세션 참여자 + 예약 멤버 교차검증)
     */
    public void validateSavePermission(String sessionId, String username) {
        // 기존 세션 참여자 검증
        if (!hasSessionAccess(sessionId, username)) {
            log.error("채팅 저장 권한 없음 (세션 참여자 아님): sessionId={}, username={}", sessionId, username);
            throw new SecurityException("해당 세션의 채팅을 저장할 권한이 없습니다.");
        }
        
        // 교차검증: 세션이 요청자의 예약에 속하는지 확인
        validateSessionToReservationAccess(sessionId, username);
    }
    
    /**
     * 세션-예약-사용자 교차검증
     */
    private void validateSessionToReservationAccess(String sessionId, String username) {
        try {
            VideoSession videoSession = videoSessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new SessionNotFoundException("세션을 찾을 수 없습니다."));
            
            Reservation reservation = videoSession.getReservation();
            if (reservation == null) {
                log.warn("세션에 연결된 예약이 없습니다: sessionId={}", sessionId);
                return; // 예약이 없는 경우는 허용 (기존 세션 참여자 검증으로 충분)
            }
            
            // null 체크 후 Member ID 비교
            if (reservation.getMember() == null || reservation.getMember().getId() == null) {
                log.error("예약에 연결된 멤버 정보가 없습니다: sessionId={}, reservationId={}", 
                        sessionId, reservation.getId());
                throw new SecurityException("해당 세션에 대한 접근 권한이 없습니다.");
            }
            
            if (!reservation.getMember().getId().equals(username)) {
                log.error("교차검증 실패 - 세션이 요청자의 예약에 속하지 않음: sessionId={}, username={}, reservationMember={}", 
                        sessionId, username, reservation.getMember().getId());
                throw new SecurityException("해당 세션에 대한 접근 권한이 없습니다.");
            }
            
            log.debug("교차검증 성공: sessionId={}, username={}, reservationId={}", 
                    sessionId, username, reservation.getId());
            
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("교차검증 중 오류 발생: sessionId={}, username={}", sessionId, username, e);
            throw new SecurityException("권한 검증 중 오류가 발생했습니다.");
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
     * 예약 기반 채팅 기록 조회 권한 검증 (예약 멤버만 가능)
     */
    public void validateReservationReadPermission(Reservation reservation, String username) {
        // null 체크
        if (reservation == null) {
            log.error("예약 정보가 null입니다: username={}", username);
            throw new SecurityException("예약 정보를 찾을 수 없습니다.");
        }
        
        if (reservation.getMember() == null || reservation.getMember().getId() == null) {
            log.error("예약에 연결된 멤버 정보가 없습니다: reservationId={}", reservation.getId());
            throw new SecurityException("해당 예약의 채팅을 조회할 권한이 없습니다.");
        }
        
        if (!reservation.getMember().getId().equals(username)) {
            log.error("채팅 조회 권한 없음: reservationId={}, username={}, reservationMember={}", 
                    reservation.getId(), username, reservation.getMember().getId());
            throw new SecurityException("해당 예약의 채팅을 조회할 권한이 없습니다.");
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