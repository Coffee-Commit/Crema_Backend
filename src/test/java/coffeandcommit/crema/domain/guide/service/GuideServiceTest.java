package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.globalTag.entity.JobField;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.JobType;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GuideServiceTest {

    @InjectMocks
    private GuideService guideService;

    @Mock
    private GuideRepository guideRepository;

    @Mock
    private GuideJobFieldRepository guideJobFieldRepository;

    private Member member1;
    private Member member2;
    private Guide guide1;
    private Guide guide2;
    private JobField jobField;
    private GuideJobField guideJobField;

    @BeforeEach
    void setUp() {
        // Create test members
        member1 = Member.builder()
                .id("member1")
                .build();

        member2 = Member.builder()
                .id("member2")
                .build();

        // Create test guides
        guide1 = Guide.builder()
                .id(1L)
                .member(member1)
                .isOpened(true)
                .title("Guide 1")
                .isApproved(true)
                .build();

        guide2 = Guide.builder()
                .id(2L)
                .member(member2)
                .isOpened(false)  // Private guide
                .title("Guide 2")
                .isApproved(true)
                .build();

        // Create test job field
        jobField = JobField.builder()
                .id(1L)
                .jobType(JobType.DEV_ENGINEERING)
                .jobName(JobNameType.BACKEND)
                .build();

        // Create test guide job field
        guideJobField = GuideJobField.builder()
                .id(1L)
                .guide(guide1)
                .jobField(jobField)
                .build();
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 성공")
    void getGuideJobField_Success() {
        // Arrange
        when(guideRepository.findByMember_Id("member1")).thenReturn(Optional.of(guide1));
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(guideJobFieldRepository.findByGuide(guide1)).thenReturn(Optional.of(guideJobField));

        // Act
        GuideJobFieldResponseDTO result = guideService.getGuideJobField(1L, "member1");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getGuideId());
        assertEquals(1L, result.getGuideJobFieldId());
        assertEquals(JobType.DEV_ENGINEERING, result.getJobFieldDTO().getJobType());
        assertEquals(JobNameType.BACKEND, result.getJobFieldDTO().getJobName());

        // Verify
        verify(guideRepository).findByMember_Id("member1");
        verify(guideRepository).findById(1L);
        verify(guideJobFieldRepository).findByGuide(guide1);
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 로그인한 사용자의 가이드를 찾을 수 없음")
    void getGuideJobField_LoggedInGuideNotFound() {
        // Arrange
        when(guideRepository.findByMember_Id("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideJobField(1L, "nonexistent");
        });
        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // Verify
        verify(guideRepository).findByMember_Id("nonexistent");
        verify(guideRepository, never()).findById(anyLong());
        verify(guideJobFieldRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 대상 가이드를 찾을 수 없음")
    void getGuideJobField_TargetGuideNotFound() {
        // Arrange
        when(guideRepository.findByMember_Id("member1")).thenReturn(Optional.of(guide1));
        when(guideRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideJobField(999L, "member1");
        });
        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // Verify
        verify(guideRepository).findByMember_Id("member1");
        verify(guideRepository).findById(999L);
        verify(guideJobFieldRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 비공개 가이드에 대한 접근 금지")
    void getGuideJobField_ForbiddenAccessToPrivateGuide() {
        // Arrange
        when(guideRepository.findByMember_Id("member1")).thenReturn(Optional.of(guide1));
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideJobField(2L, "member1");
        });
        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());

        // Verify
        verify(guideRepository).findByMember_Id("member1");
        verify(guideRepository).findById(2L);
        verify(guideJobFieldRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 가이드 직무 분야를 찾을 수 없음")
    void getGuideJobField_GuideJobFieldNotFound() {
        // Arrange
        when(guideRepository.findByMember_Id("member1")).thenReturn(Optional.of(guide1));
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(guideJobFieldRepository.findByGuide(guide1)).thenReturn(Optional.empty());

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideJobField(1L, "member1");
        });
        assertEquals(ErrorStatus.GUIDE_JOB_FIELD_NOT_FOUND, exception.getErrorCode());

        // Verify
        verify(guideRepository).findByMember_Id("member1");
        verify(guideRepository).findById(1L);
        verify(guideJobFieldRepository).findByGuide(guide1);
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 소유자는 비공개 가이드에 접근 가능")
    void getGuideJobField_OwnerCanAccessPrivateGuide() {
        // Arrange
        GuideJobField privateGuideJobField = GuideJobField.builder()
                .id(2L)
                .guide(guide2)
                .jobField(jobField)
                .build();

        when(guideRepository.findByMember_Id("member2")).thenReturn(Optional.of(guide2));
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));
        when(guideJobFieldRepository.findByGuide(guide2)).thenReturn(Optional.of(privateGuideJobField));

        // Act
        GuideJobFieldResponseDTO result = guideService.getGuideJobField(2L, "member2");

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getGuideId());
        assertEquals(2L, result.getGuideJobFieldId());
        assertEquals(JobType.DEV_ENGINEERING, result.getJobFieldDTO().getJobType());
        assertEquals(JobNameType.BACKEND, result.getJobFieldDTO().getJobName());

        // Verify
        verify(guideRepository).findByMember_Id("member2");
        verify(guideRepository).findById(2L);
        verify(guideJobFieldRepository).findByGuide(guide2);
    }
}
