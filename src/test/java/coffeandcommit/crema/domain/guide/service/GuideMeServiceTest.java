package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.globalTag.entity.JobField;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.JobType;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideProfileResponseDTO;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideJobField;
import coffeandcommit.crema.domain.guide.repository.GuideJobFieldRepository;
import coffeandcommit.crema.domain.guide.repository.GuideRepository;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GuideMeServiceTest {

    @Mock
    private GuideRepository guideRepository;

    @Mock
    private GuideJobFieldRepository guideJobFieldRepository;

    @InjectMocks
    private GuideMeService guideMeService;

    private String memberId;
    private Member member;
    private Guide guide;
    private JobField jobField;
    private GuideJobField guideJobField;

    @BeforeEach
    void setUp() {
        // Setup test data
        memberId = "test-member-id";

        member = Member.builder()
                .id(memberId)
                .nickname("TestUser")
                .profileImageUrl("http://example.com/profile.jpg")
                .build();

        guide = Guide.builder()
                .id(1L)
                .member(member)
                .isApproved(true)
                .description("Test description")
                .isOpened(true)
                .title("Test Guide")
                .companyName("Test Company")
                .workingStart(LocalDate.of(2020, 1, 1))
                .workingEnd(null)
                .isCurrent(true)
                .build();

        jobField = JobField.builder()
                .id(1L)
                .jobType(JobType.DEV_ENGINEERING)
                .jobName(JobNameType.BACKEND)
                .build();

        guideJobField = GuideJobField.builder()
                .id(1L)
                .guide(guide)
                .jobField(jobField)
                .build();
    }

    @Test
    @DisplayName("getGuideMeProfile 성공 테스트")
    void getGuideMeProfile_Success() {
        // Given
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideJobFieldRepository.findByGuide(guide)).thenReturn(Optional.of(guideJobField));

        // When
        GuideProfileResponseDTO result = guideMeService.getGuideMeProfile(memberId);

        // Then
        assertNotNull(result);
        assertEquals(guide.getId(), result.getGuideId());
        assertEquals(memberId, result.getMemberId());
        assertEquals(member.getNickname(), result.getNickname());
        assertEquals(member.getProfileImageUrl(), result.getProfileImageUrl());
        assertEquals(guide.getCompanyName(), result.getCompanyName());
        assertEquals(guide.getWorkingStart(), result.getWorkingStart());
        assertEquals(guide.getWorkingEnd(), result.getWorkingEnd());
        assertTrue(result.getWorkingPeriodYears() >= 3); // 2020년부터 현재까지 최소 3년
        assertEquals(guide.isOpened(), result.isOpened());

        // GuideJobFieldResponseDTO 검증
        assertNotNull(result.getGuideJobField());
        assertEquals(guide.getId(), result.getGuideJobField().getGuideId());
        assertEquals(jobField.getJobType(), result.getGuideJobField().getJobType());
        assertEquals(jobField.getJobName(), result.getGuideJobField().getJobName());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideJobFieldRepository).findByGuide(guide);
    }

    @Test
    @DisplayName("getGuideMeProfile 가이드 없음 테스트")
    void getGuideMeProfile_GuideNotFound() {
        // Given
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideMeService.getGuideMeProfile(memberId);
        });

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideJobFieldRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("getGuideMeProfile 가이드 직무 분야 없음 테스트")
    void getGuideMeProfile_GuideJobFieldNotFound() {
        // Given
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideJobFieldRepository.findByGuide(guide)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideMeService.getGuideMeProfile(memberId);
        });

        assertEquals(ErrorStatus.GUIDE_JOB_FIELD_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideJobFieldRepository).findByGuide(guide);
    }

    @Test
    @DisplayName("getGuideMeProfile 근무 기간 계산 테스트 - 종료일 있음")
    void getGuideMeProfile_WithWorkingEndDate() {
        // Given
        LocalDate workingStart = LocalDate.of(2018, 1, 1);
        LocalDate workingEnd = LocalDate.of(2022, 1, 1);

        Guide guideWithEndDate = guide.toBuilder()
                .workingStart(workingStart)
                .workingEnd(workingEnd)
                .isCurrent(false)
                .build();

        GuideJobField jobFieldWithEndDate = GuideJobField.builder()
                .id(1L)
                .guide(guideWithEndDate)
                .jobField(jobField)
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guideWithEndDate));
        when(guideJobFieldRepository.findByGuide(guideWithEndDate)).thenReturn(Optional.of(jobFieldWithEndDate));

        // When
        GuideProfileResponseDTO result = guideMeService.getGuideMeProfile(memberId);

        // Then
        assertNotNull(result);
        assertEquals(4, result.getWorkingPeriodYears()); // 2018-01-01부터 2022-01-01까지 4년
        assertEquals(workingStart, result.getWorkingStart());
        assertEquals(workingEnd, result.getWorkingEnd());
    }

    @Test
    @DisplayName("getGuideMeProfile 근무 시작일 없음 테스트")
    void getGuideMeProfile_NoWorkingStartDate() {
        // Given
        Guide guideNoStartDate = guide.toBuilder()
                .workingStart(null)
                .workingEnd(null)
                .build();

        GuideJobField jobFieldNoStartDate = GuideJobField.builder()
                .id(1L)
                .guide(guideNoStartDate)
                .jobField(jobField)
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guideNoStartDate));
        when(guideJobFieldRepository.findByGuide(guideNoStartDate)).thenReturn(Optional.of(jobFieldNoStartDate));

        // When
        GuideProfileResponseDTO result = guideMeService.getGuideMeProfile(memberId);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getWorkingPeriodYears()); // 시작일이 없으면 0년
    }
}
