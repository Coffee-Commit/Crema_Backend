package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.domain.globalTag.enums.ChatTopicType;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.guide.dto.response.GuideChatTopicResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideChatTopic;
import coffeandcommit.crema.domain.guide.entity.GuideJobField;
import coffeandcommit.crema.domain.guide.repository.GuideChatTopicRepository;
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

import java.util.Arrays;
import java.util.List;
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

    @Mock
    private GuideChatTopicRepository guideChatTopicRepository;

    private Member member1;
    private Member member2;
    private Guide guide1;
    private Guide guide2;
    private GuideJobField guideJobField;
    private ChatTopic chatTopic1;
    private ChatTopic chatTopic2;
    private GuideChatTopic guideChatTopic1;
    private GuideChatTopic guideChatTopic2;

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

        guideJobField = GuideJobField.builder()
                .id(1L)
                .guide(guide1)
                .jobName(JobNameType.DESIGN)
                .build();

        chatTopic1 = ChatTopic.builder()
                .id(1L)
                .chatTopic(ChatTopicType.CAREER)
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        chatTopic2 = ChatTopic.builder()
                .id(2L)
                .chatTopic(ChatTopicType.CAREER)
                .topicName(TopicNameType.JOB_CHANGE)
                .build();

        guideChatTopic1 = GuideChatTopic.builder()
                .id(1L)
                .guide(guide1)
                .chatTopic(chatTopic1)
                .build();

        guideChatTopic2 = GuideChatTopic.builder()
                .id(2L)
                .guide(guide1)
                .chatTopic(chatTopic2)
                .build();
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 성공")
    void getGuideJobField_Success() {
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(guideJobFieldRepository.findByGuide(guide1)).thenReturn(Optional.of(guideJobField));

        GuideJobFieldResponseDTO result = guideService.getGuideJobField(1L, "member1");

        assertNotNull(result);
        assertEquals(1L, result.getGuideId());
        assertEquals(JobNameType.DESIGN, result.getJobName());

        verify(guideRepository).findById(1L);
        verify(guideJobFieldRepository).findByGuide(guide1);
        // Since guide1 is public (isOpened=true), findByMember_Id should not be called
        verify(guideRepository, never()).findByMember_Id(anyString());
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 비공개 가이드에 대한 접근 금지 (로그인 사용자)")
    void getGuideJobField_LoggedInGuideNotFound() {
        // Arrange
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2)); // guide2 is private

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideJobField(2L, "nonexistent");
        });
        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());

        // Verify
        verify(guideRepository).findById(2L);
        verify(guideJobFieldRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 대상 가이드를 찾을 수 없음")
    void getGuideJobField_TargetGuideNotFound() {
        // Arrange
        when(guideRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideJobField(999L, "member1");
        });
        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // Verify
        verify(guideRepository).findById(999L);
        verify(guideRepository, never()).findByMember_Id(anyString());
        verify(guideJobFieldRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 비공개 가이드에 대한 접근 금지")
    void getGuideJobField_ForbiddenAccessToPrivateGuide() {
        // Arrange
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideJobField(2L, "member1");
        });
        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());

        // Verify
        verify(guideRepository).findById(2L);
        verify(guideJobFieldRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 가이드 직무 분야를 찾을 수 없음")
    void getGuideJobField_GuideJobFieldNotFound() {
        // Arrange
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(guideJobFieldRepository.findByGuide(guide1)).thenReturn(Optional.empty());

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideJobField(1L, "member1");
        });
        assertEquals(ErrorStatus.GUIDE_JOB_FIELD_NOT_FOUND, exception.getErrorCode());

        // Verify
        verify(guideRepository).findById(1L);
        verify(guideJobFieldRepository).findByGuide(guide1);
        // Since guide1 is public (isOpened=true), findByMember_Id should not be called
        verify(guideRepository, never()).findByMember_Id(anyString());
    }

    @Test
    @DisplayName("가이드 직무 분야 조회 - 소유자는 비공개 가이드에 접근 가능")
    void getGuideJobField_OwnerCanAccessPrivateGuide() {
        GuideJobField privateGuideJobField = GuideJobField.builder()
                .id(2L)
                .guide(guide2)
                .jobName(JobNameType.MARKETING_PR)
                .build();

        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));
        when(guideJobFieldRepository.findByGuide(guide2)).thenReturn(Optional.of(privateGuideJobField));

        GuideJobFieldResponseDTO result = guideService.getGuideJobField(2L, "member2");

        assertNotNull(result);
        assertEquals(2L, result.getGuideId());
        assertEquals(JobNameType.MARKETING_PR, result.getJobName());

        verify(guideRepository).findById(2L);
        verify(guideJobFieldRepository).findByGuide(guide2);
    }

    @Test
    @DisplayName("가이드 채팅 주제 조회 - 성공")
    void getGuideChatTopics_Success() {
        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(guideChatTopicRepository.findAllByGuideWithJoin(guide1)).thenReturn(Arrays.asList(guideChatTopic1, guideChatTopic2));

        // 테스트 실행
        List<GuideChatTopicResponseDTO> result = guideService.getGuideChatTopics(1L, "member1");

        // 검증
        assertNotNull(result);
        assertEquals(2, result.size());

        // 첫 번째 주제 검증
        assertEquals(1L, result.get(0).getId());
        assertEquals(guide1.getId(), result.get(0).getGuideId());
        assertEquals(ChatTopicType.CAREER, result.get(0).getTopic().getChatTopic());
        assertEquals(TopicNameType.CAREER_CHANGE, result.get(0).getTopic().getTopicName());

        // 두 번째 주제 검증
        assertEquals(2L, result.get(1).getId());
        assertEquals(guide1.getId(), result.get(1).getGuideId());
        assertEquals(ChatTopicType.CAREER, result.get(1).getTopic().getChatTopic());
        assertEquals(TopicNameType.JOB_CHANGE, result.get(1).getTopic().getTopicName());

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(guideChatTopicRepository).findAllByGuideWithJoin(guide1);
    }

    @Test
    @DisplayName("가이드 채팅 주제 조회 - 비공개 가이드에 대한 접근 금지 (로그인 사용자)")
    void getGuideChatTopics_LoggedInGuideNotFound() {
        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide2)); // guide2는 비공개 가이드

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideChatTopics(1L, "nonexistent");
        });

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(guideChatTopicRepository, never()).findAllByGuideWithJoin(any());
    }

    @Test
    @DisplayName("가이드 채팅 주제 조회 - 대상 가이드를 찾을 수 없음")
    void getGuideChatTopics_TargetGuideNotFound() {
        // Mock 설정
        when(guideRepository.findById(999L)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideChatTopics(999L, "member1");
        });

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(999L);
        verify(guideRepository, never()).findByMember_Id(anyString());
        verify(guideChatTopicRepository, never()).findAllByGuideWithJoin(any());
    }

    @Test
    @DisplayName("가이드 채팅 주제 조회 - 비공개 가이드에 대한 접근 금지")
    void getGuideChatTopics_ForbiddenAccessToPrivateGuide() {
        // Mock 설정
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideChatTopics(2L, "member1");
        });

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(2L);
        verify(guideChatTopicRepository, never()).findAllByGuideWithJoin(any());
    }

    @Test
    @DisplayName("가이드 채팅 주제 조회 - 소유자는 비공개 가이드에 접근 가능")
    void getGuideChatTopics_OwnerCanAccessPrivateGuide() {
        // 비공개 가이드의 채팅 주제 설정
        GuideChatTopic privateGuideChatTopic = GuideChatTopic.builder()
                .id(3L)
                .guide(guide2)
                .chatTopic(chatTopic1)
                .build();

        // Mock 설정
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));
        when(guideChatTopicRepository.findAllByGuideWithJoin(guide2)).thenReturn(List.of(privateGuideChatTopic));

        // 테스트 실행
        List<GuideChatTopicResponseDTO> result = guideService.getGuideChatTopics(2L, "member2");

        // 검증
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals(guide2.getId(), result.get(0).getGuideId());
        assertEquals(ChatTopicType.CAREER, result.get(0).getTopic().getChatTopic());
        assertEquals(TopicNameType.CAREER_CHANGE, result.get(0).getTopic().getTopicName());

        // 메서드 호출 검증
        verify(guideRepository).findById(2L);
        verify(guideChatTopicRepository).findAllByGuideWithJoin(guide2);
    }

    @Test
    @DisplayName("가이드 채팅 주제 조회 - 주제가 없는 경우 빈 리스트 반환")
    void getGuideChatTopics_EmptyTopics() {
        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(guideChatTopicRepository.findAllByGuideWithJoin(guide1)).thenReturn(List.of());

        // 테스트 실행
        List<GuideChatTopicResponseDTO> result = guideService.getGuideChatTopics(1L, "member1");

        // 검증
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // 메서드 호출 검증
        verify(guideRepository, never()).findByMember_Id(anyString());
        verify(guideRepository).findById(1L);
        verify(guideChatTopicRepository).findAllByGuideWithJoin(guide1);
    }
}
