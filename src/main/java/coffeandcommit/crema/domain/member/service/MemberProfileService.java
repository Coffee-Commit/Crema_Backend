package coffeandcommit.crema.domain.member.service;

import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.mapper.MemberMapper;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.file.FileService;
import coffeandcommit.crema.global.storage.StorageService;
import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.validation.FileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final FileService fileService;
    private final StorageService storageService;

    /**
     * 회원 프로필 이미지 등록/업데이트
     */
    @Transactional
    public MemberResponse updateMemberProfileImage(String id, MultipartFile imageFile) {
        Member member = memberRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

        try {
            // 기존 프로필 이미지가 있다면 삭제
            if (member.getProfileImageUrl() != null) {
                try {
                    storageService.deleteFile(member.getProfileImageUrl());
                    log.info("Old profile image deleted for member: {}", id);
                } catch (Exception e) {
                    log.warn("Failed to delete old profile image for member: {} - {}", id, e.getMessage());
                    // 기존 파일 삭제 실패해도 계속 진행
                }
            }

            // 새 프로필 이미지 업로드 (FileService 사용)
            FileUploadResponse uploadResponse = fileService.uploadFile(
                    imageFile,
                    FileType.PROFILE_IMAGE,
                    "profile-images",
                    id
            );

            // DB 업데이트
            member.updateProfile(null, null, uploadResponse.getFileUrl(), null);
            Member savedMember = memberRepository.save(member);

            log.info("Member profile image updated: {}, new imageUrl: {}", id, uploadResponse.getFileUrl());
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
            try {
                storageService.deleteFile(member.getProfileImageUrl());
                log.info("Profile image deleted for member: {}", memberId);
            } catch (Exception e) {
                log.warn("Failed to delete profile image for member: {} - {}", memberId, e.getMessage());
                // 이미지 삭제 실패해도 예외를 던지지 않음 (회원 탈퇴는 계속 진행)
            }

            // DB에서 프로필 이미지 URL 제거
            member.updateProfile(null, null, null, null);
            memberRepository.save(member);
        }
    }
}