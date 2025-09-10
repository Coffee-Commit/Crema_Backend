package coffeandcommit.crema.domain.member.service;

import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.domain.member.mapper.MemberMapper;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.file.FileService;
import coffeandcommit.crema.global.storage.StorageService;
import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.validation.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberProfileService 완전한 단위 테스트")
class MemberProfileServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private FileService fileService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private MemberProfileService memberProfileService;

    private Member testMember;
    private Member testMemberWithProfileImage;
    private MemberResponse testMemberResponse;
    private MultipartFile testImageFile;
    private FileUploadResponse testFileUploadResponse;

    @BeforeEach
    void setUp() {
        // Member 설정 (프로필 이미지 없음)
        testMember = Member.builder()
                .id("testMemberId")
                .nickname("테스트멤버")
                .role(MemberRole.ROOKIE)
                .email("test@example.com")
                .point(1000)
                .profileImageUrl(null)
                .description("테스트 멤버입니다")
                .provider("google")
                .providerId("google_123")
                .isDeleted(false)
                .build();

        // Member 설정 (기존 프로필 이미지 있음)
        testMemberWithProfileImage = Member.builder()
                .id("testMemberId")
                .nickname("테스트멤버")
                .role(MemberRole.ROOKIE)
                .email("test@example.com")
                .point(1000)
                .profileImageUrl("https://example.com/old-profile.jpg")
                .description("테스트 멤버입니다")
                .provider("google")
                .providerId("google_123")
                .isDeleted(false)
                .build();

        // MemberResponse 설정
        testMemberResponse = MemberResponse.builder()
                .id("testMemberId")
                .nickname("테스트멤버")
                .role(MemberRole.ROOKIE)
                .email("test@example.com")
                .point(1000)
                .profileImageUrl("https://example.com/new-profile.jpg")
                .description("테스트 멤버입니다")
                .provider("google")
                .createdAt(LocalDateTime.now())
                .build();

        // MockMultipartFile 설정
        testImageFile = new MockMultipartFile(
                "profileImage",
                "test-profile.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // FileUploadResponse 설정
        testFileUploadResponse = FileUploadResponse.builder()
                .fileUrl("https://example.com/new-profile.jpg")
                .fileKey("profile-images/testMemberId/test-profile.jpg")
                .build();
    }

    @Nested
    @DisplayName("프로필 이미지 업데이트")
    class UpdateProfileImageTests {

        @Test
        @DisplayName("성공: 새로운 프로필 이미지 등록 (기존 이미지 없음)")
        void updateMemberProfileImage_NewImage_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(fileService.uploadFile(eq(testImageFile), eq(FileType.PROFILE_IMAGE), eq("profile-images"), eq("testMemberId")))
                    .willReturn(testFileUploadResponse);

            Member updatedMember = testMember.toBuilder()
                    .profileImageUrl(testFileUploadResponse.getFileUrl())
                    .build();
            given(memberRepository.save(any(Member.class)))
                    .willReturn(updatedMember);
            given(memberMapper.memberToMemberResponse(updatedMember))
                    .willReturn(testMemberResponse);

            // when
            MemberResponse result = memberProfileService.updateMemberProfileImage("testMemberId", testImageFile);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getProfileImageUrl()).isEqualTo("https://example.com/new-profile.jpg");

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(fileService).uploadFile(testImageFile, FileType.PROFILE_IMAGE, "profile-images", "testMemberId");
            verify(memberRepository).save(any(Member.class));
            verify(memberMapper).memberToMemberResponse(any(Member.class));

            // 기존 이미지가 없으므로 삭제 호출되지 않음
            verify(storageService, never()).deleteFile(anyString());
        }

        @Test
        @DisplayName("성공: 기존 프로필 이미지 교체")
        void updateMemberProfileImage_ReplaceExisting_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMemberWithProfileImage));
            doNothing().when(storageService).deleteFile("https://example.com/old-profile.jpg");
            given(fileService.uploadFile(eq(testImageFile), eq(FileType.PROFILE_IMAGE), eq("profile-images"), eq("testMemberId")))
                    .willReturn(testFileUploadResponse);

            Member updatedMember = testMemberWithProfileImage.toBuilder()
                    .profileImageUrl(testFileUploadResponse.getFileUrl())
                    .build();
            given(memberRepository.save(any(Member.class)))
                    .willReturn(updatedMember);
            given(memberMapper.memberToMemberResponse(updatedMember))
                    .willReturn(testMemberResponse);

            // when
            MemberResponse result = memberProfileService.updateMemberProfileImage("testMemberId", testImageFile);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getProfileImageUrl()).isEqualTo("https://example.com/new-profile.jpg");

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(storageService).deleteFile("https://example.com/old-profile.jpg");
            verify(fileService).uploadFile(testImageFile, FileType.PROFILE_IMAGE, "profile-images", "testMemberId");
            verify(memberRepository).save(any(Member.class));
            verify(memberMapper).memberToMemberResponse(any(Member.class));
        }

        @Test
        @DisplayName("성공: 기존 이미지 삭제 실패해도 새 이미지 업로드 계속 진행")
        void updateMemberProfileImage_OldImageDeleteFailed_ContinueUpload() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMemberWithProfileImage));
            doThrow(new RuntimeException("Delete failed")).when(storageService).deleteFile("https://example.com/old-profile.jpg");
            given(fileService.uploadFile(eq(testImageFile), eq(FileType.PROFILE_IMAGE), eq("profile-images"), eq("testMemberId")))
                    .willReturn(testFileUploadResponse);

            Member updatedMember = testMemberWithProfileImage.toBuilder()
                    .profileImageUrl(testFileUploadResponse.getFileUrl())
                    .build();
            given(memberRepository.save(any(Member.class)))
                    .willReturn(updatedMember);
            given(memberMapper.memberToMemberResponse(updatedMember))
                    .willReturn(testMemberResponse);

            // when
            MemberResponse result = memberProfileService.updateMemberProfileImage("testMemberId", testImageFile);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getProfileImageUrl()).isEqualTo("https://example.com/new-profile.jpg");

            verify(storageService).deleteFile("https://example.com/old-profile.jpg");
            verify(fileService).uploadFile(testImageFile, FileType.PROFILE_IMAGE, "profile-images", "testMemberId");
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버 ID로 프로필 이미지 업데이트")
        void updateMemberProfileImage_MemberNotFound() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("invalidMemberId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberProfileService.updateMemberProfileImage("invalidMemberId", testImageFile))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberRepository).findByIdAndIsDeletedFalse("invalidMemberId");
            verify(fileService, never()).uploadFile(any(), any(), any(), any());
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 파일 업로드 중 오류 발생")
        void updateMemberProfileImage_FileUploadFailed() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(fileService.uploadFile(eq(testImageFile), eq(FileType.PROFILE_IMAGE), eq("profile-images"), eq("testMemberId")))
                    .willThrow(new BaseException(ErrorStatus.FILE_UPLOAD_FAILED));

            // when & then
            assertThatThrownBy(() -> memberProfileService.updateMemberProfileImage("testMemberId", testImageFile))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.FILE_UPLOAD_FAILED);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(fileService).uploadFile(testImageFile, FileType.PROFILE_IMAGE, "profile-images", "testMemberId");
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: null 이미지 파일로 업데이트 시도")
        void updateMemberProfileImage_NullImageFile() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(fileService.uploadFile(eq(null), eq(FileType.PROFILE_IMAGE), eq("profile-images"), eq("testMemberId")))
                    .willThrow(new BaseException(ErrorStatus.FILE_REQUIRED));

            // when & then
            assertThatThrownBy(() -> memberProfileService.updateMemberProfileImage("testMemberId", null))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.FILE_REQUIRED);

            verify(fileService).uploadFile(null, FileType.PROFILE_IMAGE, "profile-images", "testMemberId");
        }
    }

    @Nested
    @DisplayName("프로필 이미지 삭제")
    class DeleteProfileImageTests {

        @Test
        @DisplayName("성공: 프로필 이미지 삭제")
        void deleteProfileImage_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMemberWithProfileImage));
            doNothing().when(storageService).deleteFile("https://example.com/old-profile.jpg");

            // when
            memberProfileService.deleteProfileImage("testMemberId");

            // then
            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(storageService).deleteFile("https://example.com/old-profile.jpg");
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("성공: 프로필 이미지가 없는 멤버의 삭제 요청 (No-op)")
        void deleteProfileImage_NoExistingImage() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember)); // 프로필 이미지 없음

            // when
            memberProfileService.deleteProfileImage("testMemberId");

            // then
            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(storageService, never()).deleteFile(anyString()); // 삭제할 이미지가 없으므로 호출되지 않음
            verify(memberRepository, never()).save(any(Member.class)); // 프로필 이미지가 없으므로 저장하지 않음
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버 ID로 프로필 이미지 삭제")
        void deleteProfileImage_MemberNotFound() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("invalidMemberId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberProfileService.deleteProfileImage("invalidMemberId"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberRepository).findByIdAndIsDeletedFalse("invalidMemberId");
            verify(storageService, never()).deleteFile(anyString());
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("성공: 스토리지에서 파일 삭제 실패해도 DB는 업데이트")
        void deleteProfileImage_StorageDeleteFailed_ContinueDbUpdate() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMemberWithProfileImage));
            doThrow(new RuntimeException("Storage delete failed")).when(storageService).deleteFile("https://example.com/old-profile.jpg");

            // when
            memberProfileService.deleteProfileImage("testMemberId");

            // then
            verify(storageService).deleteFile("https://example.com/old-profile.jpg");
            verify(memberRepository).save(any(Member.class));
        }
    }
}