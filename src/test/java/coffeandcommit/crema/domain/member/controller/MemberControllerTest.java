package coffeandcommit.crema.domain.member.controller;

import coffeandcommit.crema.domain.member.dto.request.MemberUpgradeRequest;
import coffeandcommit.crema.domain.member.dto.response.MemberPublicResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberUpgradeResponse;
import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.domain.member.service.MemberProfileService;
import coffeandcommit.crema.domain.member.service.MemberService;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberController 완전한 단위 테스트")
class MemberControllerTest {

    @Mock
    private MemberService memberService;

    @Mock
    private MemberProfileService memberProfileService;

    @InjectMocks
    private MemberController memberController;

    private MemberResponse testMemberResponse;
    private MemberPublicResponse testMemberPublicResponse;
    private MemberUpgradeResponse testMemberUpgradeResponse;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        testMemberResponse = MemberResponse.builder()
                .id("testId")
                .nickname("testUser")
                .role(MemberRole.ROOKIE)
                .email("test@example.com")
                .point(1000)
                .profileImageUrl("https://example.com/profile.jpg")
                .description("테스트 사용자입니다")
                .provider("google")
                .createdAt(LocalDateTime.now())
                .build();

        testMemberPublicResponse = MemberPublicResponse.builder()
                .id("testId")
                .nickname("testUser")
                .role(MemberRole.ROOKIE)
                .profileImageUrl("https://example.com/profile.jpg")
                .description("테스트 사용자입니다")
                .build();

        testMemberUpgradeResponse = MemberUpgradeResponse.builder()
                .companyName("테스트회사")
                .isCompanyNamePublic(true)
                .jobPosition("개발자")
                .isCurrent(true)
                .workingStart(LocalDate.of(2022, 1, 1))
                .workingPeriod("2년 3개월")
                .certificationPdfUrl("https://example.com/cert.pdf")
                .build();

        testUserDetails = new User("testId", "password", Collections.emptyList());
    }

    @Nested
    @DisplayName("회원 정보 조회")
    class GetMemberTests {

        @Test
        @DisplayName("성공: ID로 회원 정보 조회")
        void getMemberById_Success() {
            // given
            given(memberService.getMemberById("testId")).willReturn(testMemberPublicResponse);

            // when
            ApiResponse<MemberPublicResponse> response = memberController.getMemberById("testId");

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult().getId()).isEqualTo("testId");
            assertThat(response.getResult().getNickname()).isEqualTo("testUser");

            verify(memberService).getMemberById("testId");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원 ID 조회")
        void getMemberById_NotFound() {
            // given
            given(memberService.getMemberById("invalidId"))
                    .willThrow(new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> memberController.getMemberById("invalidId"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberService).getMemberById("invalidId");
        }

        @Test
        @DisplayName("성공: 닉네임으로 회원 정보 조회")
        void getMemberByNickname_Success() {
            // given
            given(memberService.getMemberByNickname("testUser")).willReturn(testMemberPublicResponse);

            // when
            ApiResponse<MemberPublicResponse> response = memberController.getMemberByNickname("testUser");

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult().getNickname()).isEqualTo("testUser");

            verify(memberService).getMemberByNickname("testUser");
        }

        @Test
        @DisplayName("성공: 내 정보 조회")
        void getMyInfo_Success() {
            // given
            given(memberService.getMyInfo("testId")).willReturn(testMemberResponse);

            // when
            ApiResponse<MemberResponse> response = memberController.getMyInfo(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult().getId()).isEqualTo("testId");
            assertThat(response.getResult().getNickname()).isEqualTo("testUser");
            assertThat(response.getResult().getEmail()).isEqualTo("test@example.com");
            assertThat(response.getResult().getPoint()).isEqualTo(1000);

            verify(memberService).getMyInfo("testId");
        }
    }

    @Nested
    @DisplayName("프로필 업데이트")
    class ProfileUpdateTests {

        @Test
        @DisplayName("성공: 프로필 정보 업데이트 - 모든 필드")
        void updateMyProfileInfo_AllFields_Success() {
            // given
            MemberResponse updatedResponse = testMemberResponse.toBuilder()
                    .nickname("newNickname")
                    .description("새로운 설명")
                    .email("new@example.com")
                    .build();

            given(memberService.updateMemberProfileInfo("testId", "newNickname", "새로운 설명", "new@example.com"))
                    .willReturn(updatedResponse);

            // when
            ApiResponse<MemberResponse> response = memberController.updateMyProfileInfo(
                    "newNickname", "새로운 설명", "new@example.com", testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult().getNickname()).isEqualTo("newNickname");
            assertThat(response.getResult().getDescription()).isEqualTo("새로운 설명");
            assertThat(response.getResult().getEmail()).isEqualTo("new@example.com");

            verify(memberService).updateMemberProfileInfo("testId", "newNickname", "새로운 설명", "new@example.com");
        }

        @Test
        @DisplayName("성공: 프로필 정보 업데이트 - 닉네임만")
        void updateMyProfileInfo_NicknameOnly_Success() {
            // given
            MemberResponse updatedResponse = testMemberResponse.toBuilder()
                    .nickname("newNickname")
                    .build();

            given(memberService.updateMemberProfileInfo("testId", "newNickname", null, null))
                    .willReturn(updatedResponse);

            // when
            ApiResponse<MemberResponse> response = memberController.updateMyProfileInfo(
                    "newNickname", null, null, testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult().getNickname()).isEqualTo("newNickname");

            verify(memberService).updateMemberProfileInfo("testId", "newNickname", null, null);
        }

        @Test
        @DisplayName("실패: 중복된 닉네임으로 업데이트")
        void updateMyProfileInfo_DuplicateNickname() {
            // given
            given(memberService.updateMemberProfileInfo("testId", "duplicateNickname", null, null))
                    .willThrow(new BaseException(ErrorStatus.NICKNAME_DUPLICATED));

            // when & then
            assertThatThrownBy(() -> memberController.updateMyProfileInfo(
                    "duplicateNickname", null, null, testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.NICKNAME_DUPLICATED);

            verify(memberService).updateMemberProfileInfo("testId", "duplicateNickname", null, null);
        }

        @Test
        @DisplayName("실패: 잘못된 닉네임 형식")
        void updateMyProfileInfo_InvalidNicknameFormat() {
            // given
            given(memberService.updateMemberProfileInfo("testId", "a", null, null))
                    .willThrow(new BaseException(ErrorStatus.INVALID_NICKNAME_FORMAT));

            // when & then
            assertThatThrownBy(() -> memberController.updateMyProfileInfo(
                    "a", null, null, testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.INVALID_NICKNAME_FORMAT);
        }

        @Test
        @DisplayName("성공: 프로필 이미지 업데이트")
        void updateMyProfileImage_Success() {
            // given
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "test image content".getBytes());

            MemberResponse updatedResponse = testMemberResponse.toBuilder()
                    .profileImageUrl("https://example.com/new-profile.jpg")
                    .build();

            given(memberProfileService.updateMemberProfileImage("testId", imageFile))
                    .willReturn(updatedResponse);

            // when
            ApiResponse<MemberResponse> response = memberController.updateMyProfileImage(
                    imageFile, testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult().getProfileImageUrl()).isEqualTo("https://example.com/new-profile.jpg");

            verify(memberProfileService).updateMemberProfileImage("testId", imageFile);
        }

        @Test
        @DisplayName("실패: 잘못된 이미지 파일 형식")
        void updateMyProfileImage_InvalidFileFormat() {
            // given
            MockMultipartFile invalidFile = new MockMultipartFile(
                    "image", "test.txt", "text/plain", "invalid content".getBytes());

            given(memberProfileService.updateMemberProfileImage("testId", invalidFile))
                    .willThrow(new BaseException(ErrorStatus.INVALID_FILE_FORMAT));

            // when & then
            assertThatThrownBy(() -> memberController.updateMyProfileImage(
                    invalidFile, testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.INVALID_FILE_FORMAT);
        }
    }

    @Nested
    @DisplayName("가이드 업그레이드")
    class GuideUpgradeTests {

        @Test
        @DisplayName("성공: 가이드 업그레이드")
        void upgradeToGuide_Success() {
            // given
            MockMultipartFile pdfFile = new MockMultipartFile(
                    "certificationPdf", "cert.pdf", "application/pdf", "test pdf content".getBytes());

            given(memberService.upgradeToGuide(eq("testId"), any(MemberUpgradeRequest.class), eq(pdfFile)))
                    .willReturn(testMemberUpgradeResponse);

            // when
            ApiResponse<MemberUpgradeResponse> response = memberController.upgradeToGuide(
                    "테스트회사", true, "개발자", true,
                    LocalDate.of(2022, 1, 1), null, pdfFile, testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult().getCompanyName()).isEqualTo("테스트회사");
            assertThat(response.getResult().getJobPosition()).isEqualTo("개발자");

            verify(memberService).upgradeToGuide(eq("testId"), any(MemberUpgradeRequest.class), eq(pdfFile));
        }

        @Test
        @DisplayName("실패: 이미 가이드인 회원")
        void upgradeToGuide_AlreadyGuide() {
            // given
            MockMultipartFile pdfFile = new MockMultipartFile(
                    "certificationPdf", "cert.pdf", "application/pdf", "test pdf content".getBytes());

            given(memberService.upgradeToGuide(eq("testId"), any(MemberUpgradeRequest.class), eq(pdfFile)))
                    .willThrow(new BaseException(ErrorStatus.ALREADY_GUIDE));

            // when & then
            assertThatThrownBy(() -> memberController.upgradeToGuide(
                    "테스트회사", true, "개발자", true,
                    LocalDate.of(2022, 1, 1), null, pdfFile, testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.ALREADY_GUIDE);
        }

        @Test
        @DisplayName("실패: 필수 파일 누락")
        void upgradeToGuide_MissingFile() {
            // given
            given(memberService.upgradeToGuide(eq("testId"), any(MemberUpgradeRequest.class), eq(null)))
                    .willThrow(new BaseException(ErrorStatus.FILE_REQUIRED));

            // when & then
            assertThatThrownBy(() -> memberController.upgradeToGuide(
                    "테스트회사", true, "개발자", true,
                    LocalDate.of(2022, 1, 1), null, null, testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.FILE_REQUIRED);
        }

        @Test
        @DisplayName("성공: 가이드 업그레이드 정보 조회")
        void getUpgradeInfo_Success() {
            // given
            given(memberService.getUpgradeInfo("testId")).willReturn(testMemberUpgradeResponse);

            // when
            ApiResponse<MemberUpgradeResponse> response = memberController.getUpgradeInfo(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult().getCompanyName()).isEqualTo("테스트회사");
            assertThat(response.getResult().getJobPosition()).isEqualTo("개발자");

            verify(memberService).getUpgradeInfo("testId");
        }

        @Test
        @DisplayName("실패: 가이드가 아닌 회원의 업그레이드 정보 조회")
        void getUpgradeInfo_Forbidden() {
            // given
            given(memberService.getUpgradeInfo("testId"))
                    .willThrow(new BaseException(ErrorStatus.FORBIDDEN));

            // when & then
            assertThatThrownBy(() -> memberController.getUpgradeInfo(testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.FORBIDDEN);

            verify(memberService).getUpgradeInfo("testId");
        }
    }

    @Nested
    @DisplayName("회원 관리")
    class MemberManagementTests {

        @Test
        @DisplayName("성공: 회원 탈퇴")
        void deleteMember_Success() {
            // given
            doNothing().when(memberService).deleteMember("testId");

            // when
            ApiResponse<Void> response = memberController.deleteMember(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNull();

            verify(memberService).deleteMember("testId");
        }

        @Test
        @DisplayName("성공: 닉네임 중복 확인 - 사용 가능")
        void checkNicknameAvailability_Available() {
            // given
            given(memberService.isNicknameAvailable("availableNickname")).willReturn(true);

            // when
            ApiResponse<Boolean> response = memberController.checkNicknameAvailability("availableNickname");

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isTrue();

            verify(memberService).isNicknameAvailable("availableNickname");
        }

        @Test
        @DisplayName("성공: 닉네임 중복 확인 - 사용 불가")
        void checkNicknameAvailability_NotAvailable() {
            // given
            given(memberService.isNicknameAvailable("duplicateNickname")).willReturn(false);

            // when
            ApiResponse<Boolean> response = memberController.checkNicknameAvailability("duplicateNickname");

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isFalse();

            verify(memberService).isNicknameAvailable("duplicateNickname");
        }

        @Test
        @DisplayName("경계값: 빈 닉네임 중복 확인")
        void checkNicknameAvailability_EmptyNickname() {
            // given
            given(memberService.isNicknameAvailable("")).willReturn(false);

            // when
            ApiResponse<Boolean> response = memberController.checkNicknameAvailability("");

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isFalse();

            verify(memberService).isNicknameAvailable("");
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 상황")
    class EdgeCaseTests {

        @Test
        @DisplayName("경계값: 닉네임 최소 길이")
        void updateProfile_NicknameMinLength() {
            // given
            given(memberService.updateMemberProfileInfo("testId", "ab", null, null))
                    .willReturn(testMemberResponse);

            // when
            ApiResponse<MemberResponse> response = memberController.updateMyProfileInfo(
                    "ab", null, null, testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();

            verify(memberService).updateMemberProfileInfo("testId", "ab", null, null);
        }

        @Test
        @DisplayName("경계값: 닉네임 최대 길이")
        void updateProfile_NicknameMaxLength() {
            // given
            String maxLengthNickname = "a".repeat(32);
            given(memberService.updateMemberProfileInfo("testId", maxLengthNickname, null, null))
                    .willReturn(testMemberResponse);

            // when
            ApiResponse<MemberResponse> response = memberController.updateMyProfileInfo(
                    maxLengthNickname, null, null, testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();

            verify(memberService).updateMemberProfileInfo("testId", maxLengthNickname, null, null);
        }

        @Test
        @DisplayName("실패: 닉네임 길이 초과")
        void updateProfile_NicknameTooLong() {
            // given
            String tooLongNickname = "a".repeat(33);
            given(memberService.updateMemberProfileInfo("testId", tooLongNickname, null, null))
                    .willThrow(new BaseException(ErrorStatus.INVALID_NICKNAME_FORMAT));

            // when & then
            assertThatThrownBy(() -> memberController.updateMyProfileInfo(
                    tooLongNickname, null, null, testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.INVALID_NICKNAME_FORMAT);
        }

        @Test
        @DisplayName("실패: 잘못된 이메일 형식")
        void updateProfile_InvalidEmailFormat() {
            // given
            given(memberService.updateMemberProfileInfo("testId", null, null, "invalid-email"))
                    .willThrow(new BaseException(ErrorStatus.INVALID_EMAIL_FORMAT));

            // when & then
            assertThatThrownBy(() -> memberController.updateMyProfileInfo(
                    null, null, "invalid-email", testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.INVALID_EMAIL_FORMAT);
        }

        @Test
        @DisplayName("실패: 파일 크기 초과")
        void updateProfileImage_FileSizeExceeded() {
            // given
            MockMultipartFile largeFile = new MockMultipartFile(
                    "image", "large.jpg", "image/jpeg", new byte[1024 * 1024 * 10]); // 10MB

            given(memberProfileService.updateMemberProfileImage("testId", largeFile))
                    .willThrow(new BaseException(ErrorStatus.FILE_SIZE_EXCEEDED));

            // when & then
            assertThatThrownBy(() -> memberController.updateMyProfileImage(
                    largeFile, testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.FILE_SIZE_EXCEEDED);
        }
    }
}