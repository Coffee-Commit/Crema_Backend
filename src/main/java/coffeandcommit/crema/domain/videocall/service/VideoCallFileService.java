package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.videocall.dto.request.SharedFileUploadRequest;
import coffeandcommit.crema.domain.videocall.dto.response.SharedFileListResponse;
import coffeandcommit.crema.domain.videocall.dto.response.SharedFileResponse;
import coffeandcommit.crema.domain.videocall.entity.VideoCallSharedFile;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.domain.videocall.entity.Participant;
import coffeandcommit.crema.domain.videocall.repository.VideoCallSharedFileRepository;
import coffeandcommit.crema.domain.videocall.repository.VideoSessionRepository;
import coffeandcommit.crema.global.file.FileService;
import coffeandcommit.crema.global.validation.FileType;
import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileService fileService;
    
    /**
     * 세션의 공유 파일 목록 조회
     */
    public SharedFileListResponse getSharedFiles(String sessionId, UserDetails userDetails) {
        String username = userDetails.getUsername();
        log.info("사용자 {}가 세션 {}의 공유 파일 목록을 조회합니다", username, sessionId);
        
        // 사용자 정보 조회
        Member member = memberRepository.findByIdAndIsDeletedFalse(username)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));
        
        // 세션 존재 및 권한 확인
        VideoSession videoSession = validateSessionAccess(sessionId, member);
        
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
    public SharedFileResponse addSharedFile(String sessionId, SharedFileUploadRequest request, UserDetails userDetails) {
        String username = userDetails.getUsername();
        log.info("사용자 {}가 세션 {}에 공유 파일을 등록합니다: {}", username, sessionId, request.getFileName());
        
        // 사용자 정보 조회
        Member member = memberRepository.findByIdAndIsDeletedFalse(username)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));
        
        // 세션 존재 및 권한 확인
        VideoSession videoSession = validateSessionAccess(sessionId, member);
        
        // 중복 등록 확인
        if (sharedFileRepository.existsByVideoSessionAndImageKey(videoSession, request.getImageKey())) {
            log.error("이미 해당 세션에 등록된 파일입니다: {}", request.getImageKey());
            throw new BaseException(ErrorStatus.FILE_ALREADY_EXISTS);
        }
        
        // 공유 파일 생성 및 저장
        VideoCallSharedFile sharedFile = VideoCallSharedFile.builder()
                .videoSession(videoSession)
                .imageKey(request.getImageKey())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .contentType(request.getContentType())
                .uploadedByUserId(username)
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
    public void deleteSharedFile(String sessionId, String imageKey, UserDetails userDetails) {
        String username = userDetails.getUsername();
        log.info("사용자 {}가 세션 {}에서 공유 파일을 삭제합니다: {}", username, sessionId, imageKey);
        
        // 사용자 정보 조회
        Member member = memberRepository.findByIdAndIsDeletedFalse(username)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));
        
        // 세션 존재 및 권한 확인
        VideoSession videoSession = validateSessionAccess(sessionId, member);
        
        // 공유 파일 조회
        VideoCallSharedFile sharedFile = sharedFileRepository.findByVideoSessionAndImageKey(videoSession, imageKey)
                .orElseThrow(() -> new BaseException(ErrorStatus.FILE_NOT_FOUND));
        
        // 파일 업로드자만 삭제 가능
        if (!sharedFile.getUploadedByUserId().equals(username)) {
            log.error("파일 삭제 권한이 없습니다. 업로드자: {}, 요청자: {}", sharedFile.getUploadedByUserId(), username);
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }
        
        // DB에서 삭제
        sharedFileRepository.delete(sharedFile);
        
        log.info("공유 파일이 성공적으로 삭제되었습니다: {}", imageKey);
    }

    /**
     * 파일 업로드 및 세션에 공유 파일 등록
     */
    @Transactional
    public SharedFileResponse uploadAndAddSharedFile(String sessionId, MultipartFile file, UserDetails userDetails) {
        String username = userDetails.getUsername();
        String originalFilename = file.getOriginalFilename();
        log.info("사용자 {}가 세션 {}에 파일을 업로드하고 공유 파일로 등록합니다: {}", username, sessionId, originalFilename);

        // 파일이 비어있는지 확인
        if (file.isEmpty()) {
            log.error("업로드된 파일이 비어있습니다");
            throw new BaseException(ErrorStatus.FILE_REQUIRED);
        }

        // 사용자 정보 조회
        Member member = memberRepository.findByIdAndIsDeletedFalse(username)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

        // 세션 존재 및 권한 확인
        VideoSession videoSession = validateSessionAccess(sessionId, member);

        try {
            // 파일 타입 결정
            FileType fileType = determineFileType(file);

            // 파일 업로드
            FileUploadResponse uploadResponse = fileService.uploadFile(file, fileType, "shared-materials", username);

            // 중복 등록 확인
            String imageKey = uploadResponse.getFileKey();
            if (sharedFileRepository.existsByVideoSessionAndImageKey(videoSession, imageKey)) {
                log.error("이미 해당 세션에 등록된 파일입니다: {}", imageKey);
                throw new BaseException(ErrorStatus.FILE_ALREADY_EXISTS);
            }

            // 공유 파일 생성 및 저장
            VideoCallSharedFile sharedFile = VideoCallSharedFile.builder()
                    .videoSession(videoSession)
                    .imageKey(imageKey)
                    .fileName(originalFilename != null ? originalFilename : "unknown")
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .uploadedByUserId(username)
                    .uploadedByName(member.getNickname())
                    .build();

            VideoCallSharedFile savedFile = sharedFileRepository.save(sharedFile);

            log.info("파일 업로드 및 공유 파일 등록이 성공했습니다: {} (ID: {}, imageKey: {})",
                    originalFilename, savedFile.getId(), imageKey);

            return SharedFileResponse.from(savedFile);

        } catch (Exception e) {
            log.error("파일 업로드 중 오류 발생: {}", e.getMessage(), e);
            throw new BaseException(ErrorStatus.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 파일 타입 결정
     */
    private FileType determineFileType(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                return FileType.IMAGE;
            } else if (contentType.equals("application/pdf")) {
                return FileType.PDF;
            }
        }

        // Content-Type으로 판단되지 않으면 파일 확장자로 판단
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.endsWith(".pdf")) {
                return FileType.PDF;
            } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg") ||
                      lowerFileName.endsWith(".png") || lowerFileName.endsWith(".gif") ||
                      lowerFileName.endsWith(".webp")) {
                return FileType.IMAGE;
            }
        }

        // 기본값은 IMAGE로 설정 (가장 관대한 검증)
        return FileType.IMAGE;
    }

    /**
     * 세션 존재 및 사용자 접근 권한 확인
     */
    private VideoSession validateSessionAccess(String sessionId, Member member) {
        VideoSession videoSession = videoSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BaseException(ErrorStatus.SESSION_NOT_FOUND));
        
        // 세션이 활성 상태인지 확인
        if (!videoSession.getIsActive()) {
            log.error("비활성 상태의 세션입니다: {}", sessionId);
            throw new BaseException(ErrorStatus.SESSION_NOT_ACTIVE);
        }
        
        // 세션 참가자인지 확인
        if (!isSessionParticipant(videoSession, member)) {
            log.error("세션 접근 권한이 없습니다. 세션: {}, 사용자: {}", sessionId, member.getId());
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }
        
        return videoSession;
    }
    
    /**
     * 세션 참가자인지 확인
     */
    private boolean isSessionParticipant(VideoSession videoSession, Member member) {
        if (videoSession.getParticipants() == null || videoSession.getParticipants().isEmpty()) {
            return false;
        }
        
        // VideoSession의 participants 리스트를 순회하며 멤버 비교
        return videoSession.getParticipants().stream()
                .anyMatch(participant -> participant.getMember() != null && 
                         participant.getMember().getId().equals(member.getId()));
    }
}