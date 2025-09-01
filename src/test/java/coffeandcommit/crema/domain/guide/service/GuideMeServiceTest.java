package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.guide.dto.request.GuideJobFieldRequestDTO;
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
    private GuideJobField guideJobField;

    @BeforeEach
    void setUp() {
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

        guideJobField = GuideJobField.builder()
                .id(1L)
                .guide(guide)
                .jobName(JobNameType.DESIGN)
                .build();
    }

    @Test
    @DisplayName("getGuideMeProfile 성공 테스트")
    void getGuideMeProfile_Success() {
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideJobFieldRepository.findByGuide(guide)).thenReturn(Optional.of(guideJobField));

        GuideProfileResponseDTO result = guideMeService.getGuideMeProfile(memberId);

        assertNotNull(result);
        assertEquals(guide.getId(), result.getGuideId());
        assertEquals(memberId, result.getMemberId());
        assertEquals(member.getNickname(), result.getNickname());
        assertEquals(member.getProfileImageUrl(), result.getProfileImageUrl());
        assertEquals(guide.getCompanyName(), result.getCompanyName());
        assertEquals(guide.getWorkingStart(), result.getWorkingStart());
        assertEquals(guide.getWorkingEnd(), result.getWorkingEnd());
        assertTrue(result.getWorkingPeriodYears() >= 3);
        assertEquals(guide.isOpened(), result.isOpened());

        assertNotNull(result.getGuideJobField());
        assertEquals(guide.getId(), result.getGuideJobField().getGuideId());
        assertEquals(guideJobField.getJobName(), result.getGuideJobField().getJobName());

        verify(guideRepository).findByMember_Id(memberId);
        verify(guideJobFieldRepository).findByGuide(guide);
    }

    @Test
    @DisplayName("getGuideMeProfile 가이드 없음 테스트")
    void getGuideMeProfile_GuideNotFound() {
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.getGuideMeProfile(memberId)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideJobFieldRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("getGuideMeProfile 가이드 직무 분야 없음 테스트")
    void getGuideMeProfile_GuideJobFieldNotFound() {
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideJobFieldRepository.findByGuide(guide)).thenReturn(Optional.empty());

        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.getGuideMeProfile(memberId)
        );

        assertEquals(ErrorStatus.GUIDE_JOB_FIELD_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideJobFieldRepository).findByGuide(guide);
    }

    @Test
    @DisplayName("getGuideMeProfile 근무 기간 계산 테스트 - 종료일 있음")
    void getGuideMeProfile_WithWorkingEndDate() {
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
                .jobName(JobNameType.DESIGN)
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guideWithEndDate));
        when(guideJobFieldRepository.findByGuide(guideWithEndDate)).thenReturn(Optional.of(jobFieldWithEndDate));

        GuideProfileResponseDTO result = guideMeService.getGuideMeProfile(memberId);

        assertNotNull(result);
        assertEquals(4, result.getWorkingPeriodYears());
        assertEquals(workingStart, result.getWorkingStart());
        assertEquals(workingEnd, result.getWorkingEnd());
    }

    @Test
    @DisplayName("getGuideMeProfile 근무 시작일 없음 테스트")
    void getGuideMeProfile_NoWorkingStartDate() {
        Guide guideNoStartDate = guide.toBuilder()
                .workingStart(null)
                .workingEnd(null)
                .build();

        GuideJobField jobFieldNoStartDate = GuideJobField.builder()
                .id(1L)
                .guide(guideNoStartDate)
                .jobName(JobNameType.DESIGN)
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guideNoStartDate));
        when(guideJobFieldRepository.findByGuide(guideNoStartDate)).thenReturn(Optional.of(jobFieldNoStartDate));

        GuideProfileResponseDTO result = guideMeService.getGuideMeProfile(memberId);

        assertNotNull(result);
        assertEquals(0, result.getWorkingPeriodYears());
    }

    @Test
    @DisplayName("registerGuideJobField 새로운 직무 분야 등록 성공 테스트")
    void registerGuideJobField_CreateNewJobField_Success() {
        GuideJobFieldRequestDTO requestDTO = GuideJobFieldRequestDTO.builder()
                .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideJobFieldRepository.findByGuide(guide)).thenReturn(Optional.empty());
        when(guideJobFieldRepository.save(any(GuideJobField.class))).thenAnswer(invocation -> {
            GuideJobField saved = invocation.getArgument(0);
            return GuideJobField.builder()
                    .id(1L)
                    .guide(saved.getGuide())
                    .jobName(saved.getJobName())
                    .build();
        });

        var result = guideMeService.registerGuideJobField(memberId, requestDTO);

        assertNotNull(result);
        assertEquals(guide.getId(), result.getGuideId());
        assertEquals(JobNameType.IT_DEVELOPMENT_DATA, result.getJobName());

        verify(guideRepository).findByMember_Id(memberId);
        verify(guideJobFieldRepository).findByGuide(guide);
        verify(guideJobFieldRepository).save(any(GuideJobField.class));
    }

    @Test
    @DisplayName("registerGuideJobField 기존 직무 분야 업데이트 성공 테스트")
    void registerGuideJobField_UpdateExistingJobField_Success() {
        GuideJobFieldRequestDTO requestDTO = GuideJobFieldRequestDTO.builder()
                .jobName(JobNameType.MARKETING_PR)
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideJobFieldRepository.findByGuide(guide)).thenReturn(Optional.of(guideJobField));
        when(guideJobFieldRepository.save(any(GuideJobField.class))).thenAnswer(invocation -> {
            GuideJobField saved = invocation.getArgument(0);
            return GuideJobField.builder()
                    .id(saved.getId())
                    .guide(saved.getGuide())
                    .jobName(saved.getJobName())
                    .build();
        });

        var result = guideMeService.registerGuideJobField(memberId, requestDTO);

        assertNotNull(result);
        assertEquals(guide.getId(), result.getGuideId());
        assertEquals(JobNameType.MARKETING_PR, result.getJobName());

        verify(guideRepository).findByMember_Id(memberId);
        verify(guideJobFieldRepository).findByGuide(guide);
        verify(guideJobFieldRepository).save(any(GuideJobField.class));
    }

    @Test
    @DisplayName("registerGuideJobField 가이드 없음 테스트")
    void registerGuideJobField_GuideNotFound() {
        GuideJobFieldRequestDTO requestDTO = GuideJobFieldRequestDTO.builder()
                .jobName(JobNameType.DESIGN)
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideJobField(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideJobFieldRepository, never()).findByGuide(any());
        verify(guideJobFieldRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerGuideJobField 유효하지 않은 직무 분야 테스트 - null 값")
    void registerGuideJobField_InvalidJobField() {
        GuideJobFieldRequestDTO requestDTO = GuideJobFieldRequestDTO.builder()
                .jobName(null)
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));

        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideJobField(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.INVALID_JOB_FIELD, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideJobFieldRepository, never()).findByGuide(any());
        verify(guideJobFieldRepository, never()).save(any());
    }
}
