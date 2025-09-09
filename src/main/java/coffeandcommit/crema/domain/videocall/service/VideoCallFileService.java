package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.videocall.dto.request.SharedFileUploadRequest;
import coffeandcommit.crema.domain.videocall.dto.response.SharedFileListResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SharedFileResponse;
import coffeandcommit.crema.domain.videocall.entity.VideoCallSharedFile;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.repository.VideoCallSharedFileRepository;
import coffeandcommit.crema.domain.videocall.repository.VideoSessionRepository;
import coffeandcommit.crema.global.storage.StorageService;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VideoCallFileService {
    
    private final VideoCallSharedFileRepository sharedFileRepository;
    private final VideoSessionRepository videoSessionRepository;
    private final MemberRepository memberRepository;
    private final StorageService storageService;
    
    /**
     * 세션의 공유 파일 목록 조회
     */
    public SharedFileListResponse getSharedFiles(String sessionId, String userId) {
        log.info("사용자 {}가 세션 {}의 공유 파일 목록을 조회합니다", userId, sessionId);
        
        // 세션 존재 및 권한 확인
        VideoSession videoSession = validateSessionAccess(sessionId, userId);
        
        // 공유 파일 목록 조회
        List<VideoCallSharedFile> sharedFiles = sharedFileRepository.findByVideoSessionOrderByUploadedAtDesc(videoSession);
        
        // DTO 변환
        List<SharedFileResponse> fileResponses = sharedFiles.stream()
                .map(SharedFileResponse::from)
                .collect(Collectors.toList());
        
        long totalCount = sharedFiles.size();
        
        log.info("세션 {}의 공유 파일 {}개를 조회했습니다", sessionId, totalCount);
        
        return SharedFileListResponse.of(fileResponses, totalCount, sessionId);
    }
    
    /**
     * 세션에 공유 파일 등록
     */
    @Transactional
    public SharedFileResponse addSharedFile(String sessionId, SharedFileUploadRequest request, String userId) {
        log.info("사용자 {}가 세션 {}에 공유 파일을 등록합니다: {}", userId, sessionId, request.getFileName());
        
        // 세션 존재 및 권한 확인
        VideoSession videoSession = validateSessionAccess(sessionId, userId);
        
        // 중복 등록 확인
        if (sharedFileRepository.existsByVideoSessionAndImageKey(videoSession, request.getImageKey())) {
            log.error("이미 해당 세션에 등록된 파일입니다: {}", request.getImageKey());
            throw new BaseException(ErrorStatus.FILE_ALREADY_EXISTS);
        }
        
        // 사용자 정보 조회
        Member member = memberRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));
        
        // 공유 파일 생성 및 저장
        VideoCallSharedFile sharedFile = VideoCallSharedFile.builder()
                .videoSession(videoSession)
                .imageKey(request.getImageKey())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .contentType(request.getContentType())
                .uploadedByUserId(userId)
                .uploadedByName(member.getNickname())
                .build();
        
        VideoCallSharedFile savedFile = sharedFileRepository.save(sharedFile);
        
        log.info("공유 파일이 성공적으로 등록되었습니다: {} (ID: {})", request.getFileName(), savedFile.getId());
        
        return SharedFileResponse.from(savedFile);
    }
    
    /**
     * 공유 파일 삭제
     */
    @Transactional
    public void deleteSharedFile(String sessionId, String imageKey, String userId) {
        log.info("사용자 {}가 세션 {}에서 공유 파일을 삭제합니다: {}", userId, sessionId, imageKey);
        
        // 세션 존재 및 권한 확인
        VideoSession videoSession = validateSessionAccess(sessionId, userId);
        
        // 공유 파일 조회
        VideoCallSharedFile sharedFile = sharedFileRepository.findByVideoSessionAndImageKey(videoSession, imageKey)
                .orElseThrow(() -> new BaseException(ErrorStatus.FILE_NOT_FOUND));
        
        // 파일 업로드자만 삭제 가능
        if (!sharedFile.getUploadedByUserId().equals(userId)) {
            log.error("파일 삭제 권한이 없습니다. 업로드자: {}, 요청자: {}", sharedFile.getUploadedByUserId(), userId);
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }
        
        // DB에서 삭제
        sharedFileRepository.delete(sharedFile);
        
        log.info("공유 파일이 성공적으로 삭제되었습니다: {}", imageKey);
    }
    
    /**
     * 세션 존재 및 사용자 접근 권한 확인
     */
    private VideoSession validateSessionAccess(String sessionId, String userId) {
        VideoSession videoSession = videoSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BaseException(ErrorStatus.SESSION_NOT_FOUND));
        
        // 세션이 활성 상태인지 확인
        if (!videoSession.getIsActive()) {
            log.error("비활성 상태의 세션입니다: {}", sessionId);
            throw new BaseException(ErrorStatus.SESSION_NOT_ACTIVE);
        }
        
        // 세션 참가자인지 확인
        if (!isSessionParticipant(videoSession, userId)) {
            log.error("세션 접근 권한이 없습니다. 세션: {}, 사용자: {}", sessionId, userId);
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }
        
        return videoSession;
    }
    
    /**
     * 세션 참가자인지 확인
     */
    private boolean isSessionParticipant(VideoSession videoSession, String userId) {
        if (videoSession.getReservation() == null) {
            return false;
        }
        
        Reservation reservation = videoSession.getReservation();
        
        // 가이드이거나 예약한 멤버인지 확인
        return reservation.getGuide().getMember().getId().equals(userId) ||
               reservation.getMember().getId().equals(userId);
    }
}