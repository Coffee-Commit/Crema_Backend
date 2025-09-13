package coffeandcommit.crema.domain.member.service;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.repository.GuideRepository;
import coffeandcommit.crema.domain.member.dto.request.MemberUpgradeRequest;
import coffeandcommit.crema.domain.member.dto.response.MemberPublicResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberUpgradeResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.domain.member.mapper.MemberMapper;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.file.FileService;
import coffeandcommit.crema.global.storage.StorageService;
import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.validation.FileType;
import coffeandcommit.crema.global.validation.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 완전한 단위 테스트")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private GuideRepository guideRepository;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private MemberProfileService memberProfileService;

    @Mock
    private FileValidator fileValidator;

    @Mock
    private StorageService storageService;

    @Mock
    private FileService fileService;

    @InjectMocks
    private MemberService memberService;

    private Member testMember;
    private Member testGuide;
    private Guide testGuideEntity;
    private MemberResponse testMemberResponse;
    private MemberPublicResponse testMemberPublicResponse;
    private MemberUpgradeResponse testMemberUpgradeResponse;
    private MemberUpgradeRequest testUpgradeRequest;
    private MockMultipartFile testPdfFile;

    @BeforeEach
    void setUp() {
        // 테스트용 Member (ROOKIE)
        testMember = Member.builder()
                .id("testId")
                .nickname("testUser")
                .role(MemberRole.ROOKIE)
                .email("test@example.com")
                .point(1000)
                .profileImageUrl("https://example.com/profile.jpg")
                .description("테스트 사용자입니다")
                .provider("google")
                .isDeleted(false)
                .build();

        // 테스트용 Member (GUIDE)
        testGuide = Member.builder()
                .id("guideId")
                .nickname("guideUser")
                .role(MemberRole.GUIDE)
                .email("guide@example.com")
                .point(2000)
                .isDeleted(false)
                .build();

        // 테스트용 Guide Entity
        testGuideEntity = Guide.builder()
                .id(1L)
                .member(testGuide)
                .companyName("테스트회사")
                .jobPosition("개발자")
                .workingStart(LocalDate.of(2022, 1, 1))
                .isCurrent(true)
                .isCompanyNamePublic(true)
                .certificationImageUrl("certification-pdfs/guideId_cert.pdf")
                .build();

        // 테스트용 응답 DTO
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
                .workingPeriod("2022.01 ~ 재직중")
                .certificationPdfUrl("https://storage.googleapis.com/bucket/file?X-Goog-Signature=...")
                .build();

        // 테스트용 업그레이드 요청
        testUpgradeRequest = MemberUpgradeRequest.builder()
                .companyName("테스트회사")
                .isCompanyNamePublic(true)
                .jobPosition("개발자")
                .isCurrent(true)
                .workingStart(LocalDate.of(2022, 1, 1))
                .workingEnd(null)
                .build();

        // 테스트용 PDF 파일
        testPdfFile = new MockMultipartFile(
                "certificationPdf", "cert.pdf", "application/pdf", "test pdf content".getBytes());
    }

    @Nested
    @DisplayName("회원 정보 조회")
    class GetMemberTests {

        @Test
        @DisplayName("성공: 내 정보 조회")
        void getMyInfo_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberMapper.memberToMemberResponse(testMember))
                    .willReturn(testMemberResponse);

            // when
            MemberResponse result = memberService.getMyInfo("testId");

            // then
            assertThat(result).isEqualTo(testMemberResponse);
            verify(memberRepository).findByIdAndIsDeletedFalse("testId");
            verify(memberMapper).memberToMemberResponse(testMember);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원 조회")
        void getMyInfo_MemberNotFound() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("invalidId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.getMyInfo("invalidId"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("성공: ID로 회원 정보 조회 (타인용)")
        void getMemberById_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberMapper.memberToMemberPublicResponse(testMember))
                    .willReturn(testMemberPublicResponse);

            // when
            MemberPublicResponse result = memberService.getMemberById("testId");

            // then
            assertThat(result).isEqualTo(testMemberPublicResponse);
            verify(memberRepository).findByIdAndIsDeletedFalse("testId");
            verify(memberMapper).memberToMemberPublicResponse(testMember);
        }

        @Test
        @DisplayName("성공: 닉네임으로 회원 정보 조회")
        void getMemberByNickname_Success() {
            // given
            given(memberRepository.findByNicknameAndIsDeletedFalse("testUser"))
                    .willReturn(Optional.of(testMember));
            given(memberMapper.memberToMemberPublicResponse(testMember))
                    .willReturn(testMemberPublicResponse);

            // when
            MemberPublicResponse result = memberService.getMemberByNickname("testUser");

            // then
            assertThat(result).isEqualTo(testMemberPublicResponse);
            verify(memberRepository).findByNicknameAndIsDeletedFalse("testUser");
        }

        @Test
        @DisplayName("실패: 닉네임으로 회원 조회 시 회원 없음")
        void getMemberByNickname_MemberNotFound() {
            // given
            given(memberRepository.findByNicknameAndIsDeletedFalse("invalidNickname"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.getMemberByNickname("invalidNickname"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("프로필 업데이트")
    class ProfileUpdateTests {

        @Test
        @DisplayName("성공: 프로필 정보 업데이트 - 모든 필드")
        void updateMemberProfileInfo_AllFields_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.existsByNicknameAndIsDeletedFalse("newNickname"))
                    .willReturn(false);
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);
            given(memberMapper.memberToMemberResponse(testMember))
                    .willReturn(testMemberResponse);

            // when
            MemberResponse result = memberService.updateMemberProfileInfo(
                    "testId", "newNickname", "새로운 설명", "new@example.com");

            // then
            assertThat(result).isEqualTo(testMemberResponse);
            verify(memberRepository).findByIdAndIsDeletedFalse("testId");
            verify(memberRepository).existsByNicknameAndIsDeletedFalse("newNickname");
            verify(memberRepository).save(testMember);
        }

        @Test
        @DisplayName("성공: 프로필 정보 업데이트 - 닉네임만")
        void updateMemberProfileInfo_NicknameOnly_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.existsByNicknameAndIsDeletedFalse("newNickname"))
                    .willReturn(false);
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);
            given(memberMapper.memberToMemberResponse(testMember))
                    .willReturn(testMemberResponse);

            // when
            MemberResponse result = memberService.updateMemberProfileInfo(
                    "testId", "newNickname", null, null);

            // then
            assertThat(result).isEqualTo(testMemberResponse);
            verify(memberRepository).save(testMember);
        }

        @Test
        @DisplayName("실패: 중복된 닉네임으로 업데이트")
        void updateMemberProfileInfo_DuplicateNickname() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.existsByNicknameAndIsDeletedFalse("duplicateNickname"))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.updateMemberProfileInfo(
                    "testId", "duplicateNickname", null, null))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.NICKNAME_DUPLICATED);
        }

        @Test
        @DisplayName("실패: 회원이 존재하지 않음")
        void updateMemberProfileInfo_MemberNotFound() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("invalidId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.updateMemberProfileInfo(
                    "invalidId", "newNickname", null, null))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("가이드 업그레이드")
    class GuideUpgradeTests {

        @Test
        @DisplayName("성공: 멤버를 가이드로 업그레이드")
        void upgradeToGuide_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId")).willReturn(Optional.of(testMember));
            given(fileService.uploadFile(any(), eq(FileType.PDF), eq("certification-pdfs"), eq("testId")))
                    .willReturn(FileUploadResponse.builder()
                            .fileKey("certification-pdfs/testId_cert.pdf")
                            .fileUrl("https://storage.googleapis.com/bucket/certification-pdfs/testId_cert.pdf")
                            .build());
            given(memberRepository.save(any(Member.class))).willReturn(testGuide);
            given(guideRepository.save(any(Guide.class))).willReturn(testGuideEntity);
            given(storageService.generateViewUrl("certification-pdfs/guideId_cert.pdf"))
                    .willReturn("https://storage.googleapis.com/bucket/file?X-Goog-Signature=presigned");

            // when
            MemberUpgradeResponse result = memberService.upgradeToGuide("testId", testUpgradeRequest, testPdfFile);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCertificationPdfUrl()).contains("X-Goog-Signature"); // presigned URL 확인
            verify(storageService).generateViewUrl(any(String.class));
        }

        @Test
        @DisplayName("실패: 이미 가이드인 회원")
        void upgradeToGuide_AlreadyGuide() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testGuide)); // 이미 GUIDE 역할

            // when & then
            assertThatThrownBy(() -> memberService.upgradeToGuide("testId", testUpgradeRequest, testPdfFile))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.ALREADY_GUIDE);
        }

        @Test
        @DisplayName("실패: 이미 Guide 엔티티가 존재")
        void upgradeToGuide_GuideEntityExists() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(guideRepository.findByMember_Id("testId"))
                    .willReturn(Optional.of(testGuideEntity));

            // when & then
            assertThatThrownBy(() -> memberService.upgradeToGuide("testId", testUpgradeRequest, testPdfFile))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.ALREADY_GUIDE);
        }

        @Test
        @DisplayName("실패: 재직중인 경우 근무 종료일 입력")
        void upgradeToGuide_WorkingEndNotAllowedWhenCurrent() {
            // given
            MemberUpgradeRequest invalidRequest = MemberUpgradeRequest.builder()
                    .companyName("테스트회사")
                    .isCompanyNamePublic(true)
                    .jobPosition("개발자")
                    .isCurrent(true)
                    .workingStart(LocalDate.of(2022, 1, 1))
                    .workingEnd(LocalDate.of(2023, 12, 31)) // 재직중인데 종료일 있음
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(guideRepository.findByMember_Id("testId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.upgradeToGuide("testId", invalidRequest, testPdfFile))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.WORKING_END_NOT_ALLOWED_WHEN_CURRENT);
        }

        @Test
        @DisplayName("실패: 재직중이 아닌 경우 근무 종료일 누락")
        void upgradeToGuide_WorkingEndRequiredWhenNotCurrent() {
            // given
            MemberUpgradeRequest invalidRequest = MemberUpgradeRequest.builder()
                    .companyName("테스트회사")
                    .isCompanyNamePublic(true)
                    .jobPosition("개발자")
                    .isCurrent(false)
                    .workingStart(LocalDate.of(2022, 1, 1))
                    .workingEnd(null) // 재직중이 아닌데 종료일 없음
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(guideRepository.findByMember_Id("testId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.upgradeToGuide("testId", invalidRequest, testPdfFile))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.WORKING_END_REQUIRED_WHEN_NOT_CURRENT);
        }

        @Test
        @DisplayName("실패: 근무 시작일이 종료일보다 늦음")
        void upgradeToGuide_InvalidWorkingPeriod() {
            // given
            MemberUpgradeRequest invalidRequest = MemberUpgradeRequest.builder()
                    .companyName("테스트회사")
                    .isCompanyNamePublic(true)
                    .jobPosition("개발자")
                    .isCurrent(false)
                    .workingStart(LocalDate.of(2023, 1, 1))
                    .workingEnd(LocalDate.of(2022, 12, 31)) // 시작일이 종료일보다 늦음
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(guideRepository.findByMember_Id("testId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.upgradeToGuide("testId", invalidRequest, testPdfFile))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.INVALID_WORKING_PERIOD);
        }

        @Test
        @DisplayName("성공: 가이드 업그레이드 정보 조회")
        void getUpgradeInfo_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("guideId")).willReturn(Optional.of(testGuide));
            given(guideRepository.findByMember_Id("guideId")).willReturn(Optional.of(testGuideEntity));
            given(storageService.generateViewUrl("certification-pdfs/guideId_cert.pdf"))
                    .willReturn("https://storage.googleapis.com/bucket/file?X-Goog-Signature=presigned");

            // when
            MemberUpgradeResponse result = memberService.getUpgradeInfo("guideId");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCertificationPdfUrl()).contains("X-Goog-Signature");
            verify(storageService).generateViewUrl("certification-pdfs/guideId_cert.pdf");
        }

        @Test
        @DisplayName("실패: 가이드가 아닌 회원의 업그레이드 정보 조회")
        void getUpgradeInfo_NotGuide() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember)); // ROOKIE 역할

            // when & then
            assertThatThrownBy(() -> memberService.getUpgradeInfo("testId"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("회원 관리")
    class MemberManagementTests {

        @Test
        @DisplayName("성공: 회원 탈퇴")
        void deleteMember_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            memberService.deleteMember("testId");

            // then
            verify(memberRepository).findByIdAndIsDeletedFalse("testId");
            verify(memberRepository).save(testMember);
        }

        @Test
        @DisplayName("성공: 프로필 이미지가 있는 회원 탈퇴")
        void deleteMember_WithProfileImage_Success() {
            // given
            Member memberWithImage = testMember.toBuilder()
                    .profileImageUrl("https://example.com/profile.jpg")
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(memberWithImage));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(memberWithImage);
            doNothing().when(memberProfileService).deleteProfileImage("testId");

            // when
            memberService.deleteMember("testId");

            // then
            verify(memberProfileService).deleteProfileImage("testId");
            verify(memberRepository).save(memberWithImage);
        }

        @Test
        @DisplayName("성공: 닉네임 중복 확인 - 사용 가능")
        void isNicknameAvailable_Available() {
            // given
            given(memberRepository.existsByNicknameAndIsDeletedFalse("availableNickname"))
                    .willReturn(false);

            // when
            boolean result = memberService.isNicknameAvailable("availableNickname");

            // then
            assertThat(result).isTrue();
            verify(memberRepository).existsByNicknameAndIsDeletedFalse("availableNickname");
        }

        @Test
        @DisplayName("성공: 닉네임 중복 확인 - 사용 불가 (중복)")
        void isNicknameAvailable_Duplicate() {
            // given
            given(memberRepository.existsByNicknameAndIsDeletedFalse("duplicateNickname"))
                    .willReturn(true);

            // when
            boolean result = memberService.isNicknameAvailable("duplicateNickname");

            // then
            assertThat(result).isFalse();
            verify(memberRepository).existsByNicknameAndIsDeletedFalse("duplicateNickname");
        }

        @Test
        @DisplayName("성공: 닉네임 중복 확인 - 사용 불가 (빈 문자열)")
        void isNicknameAvailable_Empty() {
            // when
            boolean result = memberService.isNicknameAvailable("");

            // then
            assertThat(result).isFalse();
            verifyNoInteractions(memberRepository);
        }

        @Test
        @DisplayName("성공: 닉네임 중복 확인 - 사용 불가 (null)")
        void isNicknameAvailable_Null() {
            // when
            boolean result = memberService.isNicknameAvailable(null);

            // then
            assertThat(result).isFalse();
            verifyNoInteractions(memberRepository);
        }
    }

    @Nested
    @DisplayName("포인트 관리")
    class PointManagementTests {

        @Test
        @DisplayName("성공: 회원 포인트 조회")
        void getMemberPoints_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));

            // when
            int result = memberService.getMemberPoints("testId");

            // then
            assertThat(result).isEqualTo(1000);
            verify(memberRepository).findByIdAndIsDeletedFalse("testId");
        }

        @Test
        @DisplayName("성공: 회원 포인트 추가")
        void addPoints_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            memberService.addPoints("testId", 500);

            // then
            verify(memberRepository).findByIdAndIsDeletedFalse("testId");
            verify(memberRepository).save(testMember);
        }

        @Test
        @DisplayName("성공: 회원 포인트 차감")
        void decreasePoints_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            memberService.decreasePoints("testId", 200);

            // then
            verify(memberRepository).findByIdAndIsDeletedFalse("testId");
            verify(memberRepository).save(testMember);
        }

        @Test
        @DisplayName("실패: 포인트 관련 작업에서 회원 없음")
        void pointOperations_MemberNotFound() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("invalidId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.getMemberPoints("invalidId"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            assertThatThrownBy(() -> memberService.addPoints("invalidId", 100))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            assertThatThrownBy(() -> memberService.decreasePoints("invalidId", 100))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("UserDetails 생성")
    class UserDetailsTests {

        @Test
        @DisplayName("성공: UserDetails 생성")
        void createUserDetails_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));

            // when
            CustomUserDetails result = memberService.createUserDetails("testId");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMemberId()).isEqualTo("testId");
            assertThat(result.isEnabled()).isTrue();
            verify(memberRepository).findByIdAndIsDeletedFalse("testId");
        }

        @Test
        @DisplayName("성공: 삭제된 회원의 UserDetails 생성 - 비활성화")
        void createUserDetails_DeletedMember() {
            // given
            Member deletedMember = testMember.toBuilder()
                    .isDeleted(true)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(deletedMember));

            // when
            CustomUserDetails result = memberService.createUserDetails("testId");

            // then
            assertThat(result).isNotNull();
            assertThat(result.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("실패: UserDetails 생성 시 회원 없음")
        void createUserDetails_MemberNotFound() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("invalidId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.createUserDetails("invalidId"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 상황")
    class EdgeCaseTests {

        @Test
        @DisplayName("경계값: 빈 문자열 닉네임 업데이트")
        void updateMemberProfileInfo_EmptyNickname() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));

            // when & then
            assertThatThrownBy(() -> memberService.updateMemberProfileInfo(
                    "testId", "", null, null))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.INVALID_NICKNAME_FORMAT);
        }

        @Test
        @DisplayName("경계값: null 값들로 프로필 업데이트")
        void updateMemberProfileInfo_NullValues() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);
            given(memberMapper.memberToMemberResponse(testMember))
                    .willReturn(testMemberResponse);

            // when
            MemberResponse result = memberService.updateMemberProfileInfo(
                    "testId", null, null, null);

            // then
            assertThat(result).isEqualTo(testMemberResponse);
            verify(memberRepository).save(testMember);
        }

        @Test
        @DisplayName("경계값: 동일한 닉네임으로 업데이트 (본인)")
        void updateMemberProfileInfo_SameNickname() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);
            given(memberMapper.memberToMemberResponse(testMember))
                    .willReturn(testMemberResponse);

            // when
            MemberResponse result = memberService.updateMemberProfileInfo(
                    "testId", "testUser", null, null); // 현재와 동일한 닉네임

            // then
            assertThat(result).isEqualTo(testMemberResponse);
            verify(memberRepository).save(testMember);
            // 중복 체크를 하지 않아야 함 (본인의 닉네임이므로)
            verify(memberRepository, never()).existsByNicknameAndIsDeletedFalse("testUser");
        }

        @Test
        @DisplayName("경계값: 0 포인트 추가")
        void addPoints_ZeroPoints() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            memberService.addPoints("testId", 0);

            // then
            verify(memberRepository).save(testMember);
        }

        @Test
        @DisplayName("경계값: 0 포인트 차감")
        void decreasePoints_ZeroPoints() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            memberService.decreasePoints("testId", 0);

            // then
            verify(memberRepository).save(testMember);
        }

        @Test
        @DisplayName("경계값: 최대 포인트 추가")
        void addPoints_MaxPoints() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            memberService.addPoints("testId", Integer.MAX_VALUE);

            // then
            verify(memberRepository).save(testMember);
        }

        @Test
        @DisplayName("경계값: 공백만 있는 닉네임")
        void isNicknameAvailable_WhitespaceOnly() {
            // when
            boolean result = memberService.isNicknameAvailable("   ");

            // then
            assertThat(result).isFalse();
            verifyNoInteractions(memberRepository);
        }
    }

    @Nested
    @DisplayName("파일 업로드 관련")
    class FileUploadTests {

        @Test
        @DisplayName("성공: PDF 파일 업로드와 함께 가이드 업그레이드")
        void upgradeToGuide_WithValidPdfFile() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(guideRepository.findByMember_Id("testId"))
                    .willReturn(Optional.empty());

            FileUploadResponse uploadResponse = FileUploadResponse.builder()
                    .fileKey("cert.pdf")
                    .fileUrl("https://example.com/cert.pdf")
                    .build();
            given(fileService.uploadFile(eq(testPdfFile), eq(FileType.PDF), anyString(), eq("testId")))
                    .willReturn(uploadResponse);
            given(guideRepository.save(any(Guide.class)))
                    .willReturn(testGuideEntity);
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testGuide);

            // when
            MemberUpgradeResponse result = memberService.upgradeToGuide("testId", testUpgradeRequest, testPdfFile);

            // then
            assertThat(result).isNotNull();
            verify(fileService).uploadFile(eq(testPdfFile), eq(FileType.PDF), anyString(), eq("testId"));
        }

        @Test
        @DisplayName("실패: 파일 업로드 실패 시 예외 처리")
        void upgradeToGuide_FileUploadFailure() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testId"))
                    .willReturn(Optional.of(testMember));
            given(guideRepository.findByMember_Id("testId"))
                    .willReturn(Optional.empty());
            given(fileService.uploadFile(eq(testPdfFile), eq(FileType.PDF), anyString(), eq("testId")))
                    .willThrow(new BaseException(ErrorStatus.FILE_UPLOAD_FAILED));

            // when & then
            assertThatThrownBy(() -> memberService.upgradeToGuide("testId", testUpgradeRequest, testPdfFile))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.FILE_UPLOAD_FAILED);
        }
    }
}