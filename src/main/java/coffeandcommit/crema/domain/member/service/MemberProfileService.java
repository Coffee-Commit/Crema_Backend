package coffeandcommit.crema.domain.member.service;

import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.mapper.MemberMapper;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.global.AWS.service.ImageService;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final ImageService imageService;

    // 프로필 이미지 전용 설정
    private static final List<String> ALLOWED_PROFILE_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png"
    );
    private static final long MAX_PROFILE_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int MIN_PROFILE_IMAGE_DIMENSION = 100; // 최소 100x100
    private static final int MAX_PROFILE_IMAGE_DIMENSION = 1024; // 최대 1024x1024

    /**
     * 회원 프로필 이미지 등록/업데이트
     */
    @Transactional
    public MemberResponse updateMemberProfileImage(String id, MultipartFile imageFile) {
        Member member = memberRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

        try {
            // 프로필 이미지 전용 검증
            validateProfileImageFile(imageFile);

            // 기존 프로필 이미지가 있다면 삭제
            if (member.getProfileImageUrl() != null) {
                // URL에서 S3 키 추출하여 삭제
                String oldImageKey = extractS3KeyFromUrl(member.getProfileImageUrl());
                if (oldImageKey != null) {
                    imageService.deleteImage(oldImageKey);
                    log.info("Old profile image deleted for member: {}, imageKey: {}", id, oldImageKey);
                }
            }

            // 새 프로필 이미지 업로드
            var uploadResponse = imageService.uploadProfileImage(imageFile, id);

            // DB 업데이트
            member.updateProfile(null, null, uploadResponse.getImageUrl());
            Member savedMember = memberRepository.save(member);

            log.info("Member profile image updated: {}, new imageKey: {}", id, uploadResponse.getImageKey());
            return memberMapper.memberToMemberResponse(savedMember);

        } catch (BaseException e) {
            throw e; // BaseException은 그대로 전파
        } catch (Exception e) {
            log.error("Failed to update profile image for member: {} - {}", id, e.getMessage());
            throw new BaseException(ErrorStatus.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 회원의 프로필 이미지 삭제 (회원 탈퇴 시 사용)
     */
    @Transactional
    public void deleteProfileImage(String memberId) {
        Member member = memberRepository.findByIdAndIsDeletedFalse(memberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

        if (member.getProfileImageUrl() != null) {
            String imageKey = extractS3KeyFromUrl(member.getProfileImageUrl());
            if (imageKey != null) {
                try {
                    imageService.deleteImage(imageKey);
                    log.info("Profile image deleted for member: {}, imageKey: {}", memberId, imageKey);
                } catch (Exception e) {
                    log.warn("Failed to delete profile image for member: {} - {}", memberId, e.getMessage());
                    // 이미지 삭제 실패해도 예외를 던지지 않음 (회원 탈퇴는 계속 진행)
                }
            }

            // DB에서 프로필 이미지 URL 제거
            member.updateProfile(null, null, null);
            memberRepository.save(member);
        }
    }

    /**
     * 프로필 이미지 전용 검증
     */
    private void validateProfileImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(ErrorStatus.INVALID_FILE_FORMAT);
        }

        // 1. 파일 크기 검증 (2MB 제한)
        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new BaseException(ErrorStatus.FILE_SIZE_EXCEEDED);
        }

        // 2. MIME 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PROFILE_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BaseException(ErrorStatus.INVALID_FILE_FORMAT);
        }

        // 3. 파일명 및 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BaseException(ErrorStatus.INVALID_FILE_FORMAT);
        }

        String extension = getFileExtension(originalFilename);
        if (!isValidProfileImageExtension(extension)) {
            throw new BaseException(ErrorStatus.INVALID_FILE_FORMAT);
        }

        // 4. 파일 시그니처 검증 (Magic Number)
        try {
            byte[] fileBytes = file.getBytes();
            if (!isValidImageFileSignature(fileBytes)) {
                throw new BaseException(ErrorStatus.INVALID_FILE_FORMAT);
            }
        } catch (Exception e) {
            log.error("Failed to read file bytes for signature validation: {}", e.getMessage());
            throw new BaseException(ErrorStatus.INVALID_FILE_FORMAT);
        }

        log.debug("Profile image validation passed for file: {}, size: {}, type: {}",
                originalFilename, file.getSize(), contentType);
    }

    /**
     * 파일 시그니처 검증 (Magic Number)
     */
    private boolean isValidImageFileSignature(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 4) {
            return false;
        }

        // JPEG 시그니처: FF D8 FF
        if (fileBytes.length >= 3 &&
                fileBytes[0] == (byte) 0xFF &&
                fileBytes[1] == (byte) 0xD8 &&
                fileBytes[2] == (byte) 0xFF) {
            return true;
        }

        // PNG 시그니처: 89 50 4E 47 0D 0A 1A 0A
        if (fileBytes.length >= 8 &&
                fileBytes[0] == (byte) 0x89 &&
                fileBytes[1] == (byte) 0x50 &&
                fileBytes[2] == (byte) 0x4E &&
                fileBytes[3] == (byte) 0x47 &&
                fileBytes[4] == (byte) 0x0D &&
                fileBytes[5] == (byte) 0x0A &&
                fileBytes[6] == (byte) 0x1A &&
                fileBytes[7] == (byte) 0x0A) {
            return true;
        }

        return false;
    }

    /**
     * 프로필 이미지 허용 확장자 검증
     */
    private boolean isValidProfileImageExtension(String extension) {
        if (extension == null) {
            return false;
        }

        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png");
        return allowedExtensions.contains(extension.toLowerCase());
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 이미지 URL에서 S3 키 추출
     */
    private String extractS3KeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        try {
            // URL 형태: https://bucket.s3.region.amazonaws.com/folder/filename
            // 또는: https://bucket.s3.amazonaws.com/folder/filename
            if (imageUrl.contains(".amazonaws.com/")) {
                int keyStartIndex = imageUrl.indexOf(".amazonaws.com/") + 15;
                if (keyStartIndex < imageUrl.length()) {
                    return imageUrl.substring(keyStartIndex);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract S3 key from URL: {}", imageUrl);
            return null;
        }
    }
}