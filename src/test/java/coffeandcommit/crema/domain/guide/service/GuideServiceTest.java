package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.guide.dto.response.*;
import coffeandcommit.crema.domain.guide.entity.ExperienceDetail;
import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideChatTopic;
import coffeandcommit.crema.domain.guide.entity.GuideJobField;
import coffeandcommit.crema.domain.guide.entity.GuideSchedule;
import coffeandcommit.crema.domain.guide.entity.HashTag;
import coffeandcommit.crema.domain.guide.entity.TimeSlot;
import coffeandcommit.crema.domain.guide.enums.DayType;
import coffeandcommit.crema.domain.guide.repository.ExperienceDetailRepository;
import coffeandcommit.crema.domain.guide.repository.ExperienceGroupRepository;
import coffeandcommit.crema.domain.guide.repository.GuideChatTopicRepository;
import coffeandcommit.crema.domain.guide.repository.GuideJobFieldRepository;
import coffeandcommit.crema.domain.guide.repository.GuideRepository;
import coffeandcommit.crema.domain.guide.repository.GuideScheduleRepository;
import coffeandcommit.crema.domain.guide.repository.HashTagRepository;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.domain.review.repository.ReviewExperienceRepository;
import coffeandcommit.crema.domain.review.repository.ReviewRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    @Mock
    private HashTagRepository hashTagRepository;

    @Mock
    private GuideScheduleRepository guideScheduleRepository;

    @Mock
    private ExperienceDetailRepository experienceDetailRepository;

    @Mock
    private ExperienceGroupRepository experienceGroupRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReviewExperienceRepository reviewExperienceRepository;

    private Member member1;
    private Member member2;
    private Guide guide1;
    private Guide guide2;
    private GuideJobField guideJobField;
    private ExperienceDetail experienceDetail;
    private ChatTopic chatTopic1;
    private ChatTopic chatTopic2;
    private GuideChatTopic guideChatTopic1;
    private GuideChatTopic guideChatTopic2;
    private HashTag hashTag1;
    private HashTag hashTag2;
    private GuideSchedule guideSchedule1;
    private GuideSchedule guideSchedule2;
    private TimeSlot timeSlot1;
    private TimeSlot timeSlot2;

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
                .build();

        guide2 = Guide.builder()
                .id(2L)
                .member(member2)
                .isOpened(false)  // Private guide
                .title("Guide 2")
                .build();

        guideJobField = GuideJobField.builder()
                .id(1L)
                .guide(guide1)
                .jobName(JobNameType.DESIGN)
                .build();

        chatTopic1 = ChatTopic.builder()
                .id(1L)
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        chatTopic2 = ChatTopic.builder()
                .id(2L)
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

        hashTag1 = HashTag.builder()
                .id(1L)
                .guide(guide1)
                .hashTagName("Java")
                .build();

        hashTag2 = HashTag.builder()
                .id(2L)
                .guide(guide1)
                .hashTagName("Spring")
                .build();

        // Create test experience detail
        experienceDetail = ExperienceDetail.builder()
                .id(1L)
                .guide(guide1)
                .who("신입 개발자")
                .solution("취업 준비")
                .how("포트폴리오 작성")
                .build();

        // Create test guide schedules
        guideSchedule1 = GuideSchedule.builder()
                .id(1L)
                .guide(guide1)
                .dayOfWeek(DayType.MONDAY)
                .timeSlots(new ArrayList<>())
                .build();

        guideSchedule2 = GuideSchedule.builder()
                .id(2L)
                .guide(guide1)
                .dayOfWeek(DayType.WEDNESDAY)
                .timeSlots(new ArrayList<>())
                .build();

        // Create test time slots
        timeSlot1 = TimeSlot.builder()
                .id(1L)
                .schedule(guideSchedule1)
                .startTimeOption(java.time.LocalTime.of(9, 0))
                .endTimeOption(java.time.LocalTime.of(10, 0))
                .build();

        timeSlot2 = TimeSlot.builder()
                .id(2L)
                .schedule(guideSchedule2)
                .startTimeOption(java.time.LocalTime.of(14, 0))
                .endTimeOption(java.time.LocalTime.of(15, 0))
                .build();

        // Set up the relationship between GuideSchedule and TimeSlot
        guideSchedule1.getTimeSlots().add(timeSlot1);
        guideSchedule2.getTimeSlots().add(timeSlot2);
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
        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

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
        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

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
        assertEquals(TopicNameType.CAREER_CHANGE, result.get(0).getTopic().getTopicName());

        // 두 번째 주제 검증
        assertEquals(2L, result.get(1).getId());
        assertEquals(guide1.getId(), result.get(1).getGuideId());
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

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

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

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

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

    @Test
    @DisplayName("가이드 해시태그 조회 - 성공")
    void getGuideHashTags_Success() {
        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(hashTagRepository.findByGuide(guide1)).thenReturn(Arrays.asList(hashTag1, hashTag2));

        // 테스트 실행
        List<GuideHashTagResponseDTO> result = guideService.getGuideHashTags(1L, "member1");

        // 검증
        assertNotNull(result);
        assertEquals(2, result.size());

        // 첫 번째 해시태그 검증
        assertEquals(1L, result.get(0).getId());
        assertEquals(guide1.getId(), result.get(0).getGuideId());
        assertEquals("Java", result.get(0).getHashTagName());

        // 두 번째 해시태그 검증
        assertEquals(2L, result.get(1).getId());
        assertEquals(guide1.getId(), result.get(1).getGuideId());
        assertEquals("Spring", result.get(1).getHashTagName());

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(hashTagRepository).findByGuide(guide1);
    }

    @Test
    @DisplayName("가이드 해시태그 조회 - 대상 가이드를 찾을 수 없음")
    void getGuideHashTags_TargetGuideNotFound() {
        // Mock 설정
        when(guideRepository.findById(999L)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideHashTags(999L, "member1");
        });

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(999L);
        verify(guideRepository, never()).findByMember_Id(anyString());
        verify(hashTagRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 해시태그 조회 - 비공개 가이드에 대한 접근 금지")
    void getGuideHashTags_ForbiddenAccessToPrivateGuide() {
        // Mock 설정
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideHashTags(2L, "member1");
        });

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(2L);
        verify(hashTagRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 해시태그 조회 - 소유자는 비공개 가이드에 접근 가능")
    void getGuideHashTags_OwnerCanAccessPrivateGuide() {
        // 비공개 가이드의 해시태그 설정
        HashTag privateHashTag = HashTag.builder()
                .id(3L)
                .guide(guide2)
                .hashTagName("Private")
                .build();

        // Mock 설정
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));
        when(hashTagRepository.findByGuide(guide2)).thenReturn(List.of(privateHashTag));

        // 테스트 실행
        List<GuideHashTagResponseDTO> result = guideService.getGuideHashTags(2L, "member2");

        // 검증
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals(guide2.getId(), result.get(0).getGuideId());
        assertEquals("Private", result.get(0).getHashTagName());

        // 메서드 호출 검증
        verify(guideRepository).findById(2L);
        verify(hashTagRepository).findByGuide(guide2);
    }

    @Test
    @DisplayName("가이드 해시태그 조회 - 빈 목록")
    void getGuideHashTags_EmptyTags() {
        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(hashTagRepository.findByGuide(guide1)).thenReturn(List.of());

        // 테스트 실행
        List<GuideHashTagResponseDTO> result = guideService.getGuideHashTags(1L, "member1");

        // 검증
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(hashTagRepository).findByGuide(guide1);
    }

    @Test
    @DisplayName("가이드 스케줄 조회 - 성공")
    void getGuideSchedules_Success() {
        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(guideScheduleRepository.findByGuide(guide1)).thenReturn(Arrays.asList(guideSchedule1, guideSchedule2));

        // 테스트 실행
        GuideScheduleResponseDTO result = guideService.getGuideSchedules(1L, "member1");

        // 검증
        assertNotNull(result);
        assertEquals(guide1.getId(), result.getGuideId());
        assertEquals(2, result.getSchedules().size());

        // 첫 번째 스케줄 검증
        assertEquals(DayType.MONDAY, result.getSchedules().get(0).getDayOfWeek());
        assertEquals(1, result.getSchedules().get(0).getTimeSlots().size());
        assertTrue(result.getSchedules().get(0).getTimeSlots().get(0).getPreferredTimeRange().startsWith("09:00"));
        assertTrue(result.getSchedules().get(0).getTimeSlots().get(0).getPreferredTimeRange().contains("10:00"));

        // 두 번째 스케줄 검증
        assertEquals(DayType.WEDNESDAY, result.getSchedules().get(1).getDayOfWeek());
        assertEquals(1, result.getSchedules().get(1).getTimeSlots().size());
        assertTrue(result.getSchedules().get(1).getTimeSlots().get(0).getPreferredTimeRange().startsWith("14:00"));
        assertTrue(result.getSchedules().get(1).getTimeSlots().get(0).getPreferredTimeRange().contains("15:00"));

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(guideScheduleRepository).findByGuide(guide1);
    }

    @Test
    @DisplayName("가이드 스케줄 조회 - 대상 가이드를 찾을 수 없음")
    void getGuideSchedules_TargetGuideNotFound() {
        // Mock 설정
        when(guideRepository.findById(999L)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideSchedules(999L, "member1");
        });

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(999L);
        verify(guideRepository, never()).findByMember_Id(anyString());
        verify(guideScheduleRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 스케줄 조회 - 비공개 가이드에 대한 접근 금지")
    void getGuideSchedules_ForbiddenAccessToPrivateGuide() {
        // Mock 설정
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideSchedules(2L, "member1");
        });

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(2L);
        verify(guideScheduleRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 스케줄 조회 - 소유자는 비공개 가이드에 접근 가능")
    void getGuideSchedules_OwnerCanAccessPrivateGuide() {
        // 비공개 가이드의 스케줄 설정
        GuideSchedule privateGuideSchedule = GuideSchedule.builder()
                .id(3L)
                .guide(guide2)
                .dayOfWeek(DayType.FRIDAY)
                .build();

        TimeSlot privateTimeSlot = TimeSlot.builder()
                .id(3L)
                .schedule(privateGuideSchedule)
                .startTimeOption(java.time.LocalTime.of(16, 0))
                .endTimeOption(java.time.LocalTime.of(17, 0))
                .build();

        privateGuideSchedule.getTimeSlots().add(privateTimeSlot);

        // Mock 설정
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));
        when(guideScheduleRepository.findByGuide(guide2)).thenReturn(List.of(privateGuideSchedule));

        // 테스트 실행
        GuideScheduleResponseDTO result = guideService.getGuideSchedules(2L, "member2");

        // 검증
        assertNotNull(result);
        assertEquals(guide2.getId(), result.getGuideId());
        assertEquals(1, result.getSchedules().size());
        assertEquals(DayType.FRIDAY, result.getSchedules().get(0).getDayOfWeek());
        assertEquals(1, result.getSchedules().get(0).getTimeSlots().size());
        assertTrue(result.getSchedules().get(0).getTimeSlots().get(0).getPreferredTimeRange().startsWith("16:00"));
        assertTrue(result.getSchedules().get(0).getTimeSlots().get(0).getPreferredTimeRange().contains("17:00"));

        // 메서드 호출 검증
        verify(guideRepository).findById(2L);
        verify(guideScheduleRepository).findByGuide(guide2);
    }

    @Test
    @DisplayName("가이드 스케줄 조회 - 빈 목록")
    void getGuideSchedules_EmptySchedules() {
        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(guideScheduleRepository.findByGuide(guide1)).thenReturn(List.of());

        // 테스트 실행
        GuideScheduleResponseDTO result = guideService.getGuideSchedules(1L, "member1");

        // 검증
        assertNotNull(result);
        assertEquals(guide1.getId(), result.getGuideId());
        assertTrue(result.getSchedules().isEmpty());

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(guideScheduleRepository).findByGuide(guide1);
    }

    @Test
    @DisplayName("가이드 경험 소주제 조회 - 성공")
    void getGuideExperienceDetails_Success() {
        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(experienceDetailRepository.findByGuide(guide1)).thenReturn(Optional.of(experienceDetail));

        // 테스트 실행
        GuideExperienceDetailResponseDTO result = guideService.getGuideExperienceDetails(1L, "member1");

        // 검증
        assertNotNull(result);
        assertEquals(experienceDetail.getId(), result.getId());
        assertEquals(guide1.getId(), result.getGuideId());
        assertEquals(experienceDetail.getWho(), result.getWho());
        assertEquals(experienceDetail.getSolution(), result.getSolution());
        assertEquals(experienceDetail.getHow(), result.getHow());

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(experienceDetailRepository).findByGuide(guide1);
    }

    @Test
    @DisplayName("가이드 경험 소주제 조회 - 가이드 없음")
    void getGuideExperienceDetails_GuideNotFound() {
        // Mock 설정
        when(guideRepository.findById(999L)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideExperienceDetails(999L, null);
        });
        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(999L);
        verify(experienceDetailRepository, never()).findByGuide(any(Guide.class));
    }

    @Test
    @DisplayName("가이드 경험 소주제 조회 - 경험 소주제 없음")
    void getGuideExperienceDetails_ExperienceDetailNotFound() {
        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(experienceDetailRepository.findByGuide(guide1)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideExperienceDetails(1L, "member1");
        });
        assertEquals(ErrorStatus.EXPERIENCE_DETAIL_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(experienceDetailRepository).findByGuide(guide1);
    }

    @Test
    @DisplayName("가이드 경험 소주제 조회 - 비공개 가이드 접근 금지")
    void getGuideExperienceDetails_ForbiddenAccessToPrivateGuide() {
        // Mock 설정
        Guide privateGuide = guide2.toBuilder().isOpened(false).build();
        when(guideRepository.findById(2L)).thenReturn(Optional.of(privateGuide));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            guideService.getGuideExperienceDetails(2L, null);
        });
        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(2L);
        verify(experienceDetailRepository, never()).findByGuide(any(Guide.class));
    }

    @Test
    @DisplayName("가이드 경험 소주제 조회 - 소유자는 비공개 가이드 접근 가능")
    void getGuideExperienceDetails_OwnerCanAccessPrivateGuide() {
        // Mock 설정
        Guide privateGuide = guide1.toBuilder().isOpened(false).build();
        ExperienceDetail privateExperienceDetail = experienceDetail.toBuilder().guide(privateGuide).build();

        when(guideRepository.findById(1L)).thenReturn(Optional.of(privateGuide));
        when(experienceDetailRepository.findByGuide(privateGuide)).thenReturn(Optional.of(privateExperienceDetail));

        // 테스트 실행
        GuideExperienceDetailResponseDTO result = guideService.getGuideExperienceDetails(1L, "member1");

        // 검증
        assertNotNull(result);
        assertEquals(privateExperienceDetail.getId(), result.getId());
        assertEquals(privateGuide.getId(), result.getGuideId());
        assertEquals(privateExperienceDetail.getWho(), result.getWho());
        assertEquals(privateExperienceDetail.getSolution(), result.getSolution());
        assertEquals(privateExperienceDetail.getHow(), result.getHow());

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(experienceDetailRepository).findByGuide(privateGuide);
    }

    @Test
    @DisplayName("가이드 경험 목록 조회 - 성공")
    void getGuideExperiences_Success() {
        // 테스트 데이터 준비
        ExperienceGroup experienceGroup1 = ExperienceGroup.builder()
                .id(1L)
                .guide(guide1)
                .guideChatTopic(guideChatTopic1)
                .experienceTitle("첫 번째 경험")
                .experienceContent("첫 번째 경험 내용")
                .build();

        ExperienceGroup experienceGroup2 = ExperienceGroup.builder()
                .id(2L)
                .guide(guide1)
                .guideChatTopic(guideChatTopic2)
                .experienceTitle("두 번째 경험")
                .experienceContent("두 번째 경험 내용")
                .build();

        List<ExperienceGroup> experienceGroups = Arrays.asList(experienceGroup1, experienceGroup2);

        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(experienceGroupRepository.findByGuide(guide1)).thenReturn(experienceGroups);

        // 테스트 실행
        GuideExperienceResponseDTO result = guideService.getGuideExperiences(1L, "member1");

        // 검증
        assertNotNull(result);
        assertEquals(2, result.getGroups().size());

        // 첫 번째 경험 검증
        assertEquals(1L, result.getGroups().get(0).getId());
        assertEquals(guideChatTopic1.getId(), result.getGroups().get(0).getGuideChatTopicId());
        assertEquals("첫 번째 경험", result.getGroups().get(0).getExperienceTitle());
        assertEquals("첫 번째 경험 내용", result.getGroups().get(0).getExperienceContent());

        // 두 번째 경험 검증
        assertEquals(2L, result.getGroups().get(1).getId());
        assertEquals(guideChatTopic2.getId(), result.getGroups().get(1).getGuideChatTopicId());
        assertEquals("두 번째 경험", result.getGroups().get(1).getExperienceTitle());
        assertEquals("두 번째 경험 내용", result.getGroups().get(1).getExperienceContent());

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(experienceGroupRepository).findByGuide(guide1);
    }

    @Test
    @DisplayName("가이드 경험 목록 조회 - 가이드 없음")
    void getGuideExperiences_GuideNotFound() {
        // Mock 설정
        when(guideRepository.findById(999L)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideService.getGuideExperiences(999L, "member1")
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(999L);
        verify(experienceGroupRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 경험 목록 조회 - 비공개 가이드 접근 금지")
    void getGuideExperiences_ForbiddenAccessToPrivateGuide() {
        // Mock 설정
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2)); // guide2는 비공개 가이드

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideService.getGuideExperiences(2L, "member1")
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findById(2L);
        verify(experienceGroupRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("가이드 경험 목록 조회 - 소유자는 비공개 가이드 접근 가능")
    void getGuideExperiences_OwnerCanAccessPrivateGuide() {
        // 테스트 데이터 준비
        ExperienceGroup privateExperienceGroup = ExperienceGroup.builder()
                .id(3L)
                .guide(guide2)
                .guideChatTopic(guideChatTopic1)
                .experienceTitle("비공개 경험")
                .experienceContent("비공개 경험 내용")
                .build();

        // Mock 설정
        when(guideRepository.findById(2L)).thenReturn(Optional.of(guide2));
        when(experienceGroupRepository.findByGuide(guide2)).thenReturn(List.of(privateExperienceGroup));

        // 테스트 실행
        GuideExperienceResponseDTO result = guideService.getGuideExperiences(2L, "member2");

        // 검증
        assertNotNull(result);
        assertEquals(1, result.getGroups().size());
        assertEquals(3L, result.getGroups().get(0).getId());
        assertEquals(guideChatTopic1.getId(), result.getGroups().get(0).getGuideChatTopicId());
        assertEquals("비공개 경험", result.getGroups().get(0).getExperienceTitle());
        assertEquals("비공개 경험 내용", result.getGroups().get(0).getExperienceContent());

        // 메서드 호출 검증
        verify(guideRepository).findById(2L);
        verify(experienceGroupRepository).findByGuide(guide2);
    }

    @Test
    @DisplayName("가이드 경험 목록 조회 - 빈 목록")
    void getGuideExperiences_EmptyList() {
        // Mock 설정
        when(guideRepository.findById(1L)).thenReturn(Optional.of(guide1));
        when(experienceGroupRepository.findByGuide(guide1)).thenReturn(List.of());

        // 테스트 실행
        GuideExperienceResponseDTO result = guideService.getGuideExperiences(1L, "member1");

        // 검증
        assertNotNull(result);
        assertTrue(result.getGroups().isEmpty());

        // 메서드 호출 검증
        verify(guideRepository).findById(1L);
        verify(experienceGroupRepository).findByGuide(guide1);
    }
    @Test
    @DisplayName("getGuideCoffeeChat 성공 테스트")
    void getGuideCoffeeChat_Success() {
        // Given
        Long guideId = 1L;
        String loginMemberId = "member1";

        List<HashTag> hashTags = Arrays.asList(hashTag1, hashTag2);
        List<ExperienceGroup> experienceGroups = new ArrayList<>();

        when(guideRepository.findById(guideId)).thenReturn(Optional.of(guide1));
        when(hashTagRepository.findByGuide(guide1)).thenReturn(hashTags);
        when(reviewRepository.getAverageScoreByGuideId(guideId)).thenReturn(4.5);
        when(reviewRepository.countByGuideId(guideId)).thenReturn(10L);
        when(experienceGroupRepository.findByGuide(guide1)).thenReturn(experienceGroups);
        when(experienceDetailRepository.findByGuide(guide1)).thenReturn(Optional.of(experienceDetail));

        // When
        GuideCoffeeChatResponseDTO result = guideService.getGuideCoffeeChat(guideId, loginMemberId);

        // Then
        assertNotNull(result);
        assertEquals(guide1.getTitle(), result.getTitle());
        assertEquals(guide1.getChatDescription(), result.getChatDescription());
        assertEquals(guide1.isOpened(), result.isOpened());
        assertEquals(4.5, result.getReviewScore());
        assertEquals(10L, result.getReviewCount());
        assertNotNull(result.getTags());
        assertEquals(2, result.getTags().size());
        assertNotNull(result.getExperienceDetail());

        // Verify
        verify(guideRepository).findById(guideId);
        verify(hashTagRepository).findByGuide(guide1);
        verify(reviewRepository).getAverageScoreByGuideId(guideId);
        verify(reviewRepository).countByGuideId(guideId);
        verify(experienceGroupRepository).findByGuide(guide1);
        verify(experienceDetailRepository).findByGuide(guide1);
    }

    @Test
    @DisplayName("getGuideCoffeeChat 가이드 없음 테스트")
    void getGuideCoffeeChat_GuideNotFound() {
        // Given
        Long guideId = 999L;
        String loginMemberId = "member1";

        when(guideRepository.findById(guideId)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> 
            guideService.getGuideCoffeeChat(guideId, loginMemberId)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // Verify
        verify(guideRepository).findById(guideId);
        verifyNoInteractions(hashTagRepository, reviewRepository, experienceGroupRepository, experienceDetailRepository);
    }

    @Test
    @DisplayName("getGuideCoffeeChat 비공개 가이드 접근 금지 테스트")
    void getGuideCoffeeChat_ForbiddenAccessToPrivateGuide() {
        // Given
        Long guideId = 2L;
        String loginMemberId = "member1"; // Not the owner of guide2

        when(guideRepository.findById(guideId)).thenReturn(Optional.of(guide2)); // guide2 is private

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> 
            guideService.getGuideCoffeeChat(guideId, loginMemberId)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // Verify
        verify(guideRepository).findById(guideId);
        verifyNoInteractions(hashTagRepository, reviewRepository, experienceGroupRepository, experienceDetailRepository);
    }

    @Test
    @DisplayName("getGuideCoffeeChat 비공개 가이드 소유자 접근 성공 테스트")
    void getGuideCoffeeChat_OwnerCanAccessPrivateGuide() {
        // Given
        Long guideId = 2L;
        String loginMemberId = "member2"; // Owner of guide2

        List<HashTag> hashTags = new ArrayList<>();
        List<ExperienceGroup> experienceGroups = new ArrayList<>();
        ExperienceDetail privateExperienceDetail = ExperienceDetail.builder()
                .id(2L)
                .guide(guide2)
                .who("경력 개발자")
                .solution("이직 준비")
                .how("포트폴리오 업데이트")
                .build();

        when(guideRepository.findById(guideId)).thenReturn(Optional.of(guide2));
        when(hashTagRepository.findByGuide(guide2)).thenReturn(hashTags);
        when(reviewRepository.getAverageScoreByGuideId(guideId)).thenReturn(null); // No reviews
        when(reviewRepository.countByGuideId(guideId)).thenReturn(0L);
        when(experienceGroupRepository.findByGuide(guide2)).thenReturn(experienceGroups);
        when(experienceDetailRepository.findByGuide(guide2)).thenReturn(Optional.of(privateExperienceDetail));

        // When
        GuideCoffeeChatResponseDTO result = guideService.getGuideCoffeeChat(guideId, loginMemberId);

        // Then
        assertNotNull(result);
        assertEquals(guide2.getTitle(), result.getTitle());
        assertEquals(guide2.isOpened(), result.isOpened());
        assertEquals(0.0, result.getReviewScore()); // Default value when no reviews
        assertEquals(0L, result.getReviewCount());

        // Verify
        verify(guideRepository).findById(guideId);
        verify(hashTagRepository).findByGuide(guide2);
        verify(reviewRepository).getAverageScoreByGuideId(guideId);
        verify(reviewRepository).countByGuideId(guideId);
        verify(experienceGroupRepository).findByGuide(guide2);
        verify(experienceDetailRepository).findByGuide(guide2);
    }
    @Test
    @DisplayName("가이드 목록 조회 - 성공")
    void getGuides_Success() {
        // 테스트 데이터 준비
        List<Long> jobFieldIds = List.of(1L);
        List<Long> chatTopicIds = List.of(1L, 2L);
        String keyword = "Java";
        Pageable pageable = Pageable.unpaged();
        String loginMemberId = "member1";
        String sort = "latest";

        guide1 = guide1.toBuilder()
                .guideJobField(guideJobField)
                .hashTags(List.of(hashTag1, hashTag2))
                .build();
        guideJobField = guideJobField.toBuilder()
                .guide(guide1)
                .build();

        // 페이지 객체 생성
        Page<Guide> guidePage = new PageImpl<>(List.of(guide1));

        // Mock 설정
        when(guideRepository.findBySearchConditions(jobFieldIds, chatTopicIds, keyword, pageable))
                .thenReturn(guidePage);
        when(reservationRepository.countByGuideAndStatus(guide1, Status.COMPLETED)).thenReturn(5L);
        when(reviewRepository.calculateAverageStarByGuide(guide1)).thenReturn(java.math.BigDecimal.valueOf(4.5));
        when(reviewRepository.countByGuide(guide1)).thenReturn(10L);
        when(reviewExperienceRepository.countThumbsUpByGuide(guide1)).thenReturn(7L);

        // 테스트 실행
        Page<GuideListResponseDTO> result = guideService.getGuides(jobFieldIds, chatTopicIds, keyword, pageable, loginMemberId, sort);

        // 검증
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        GuideListResponseDTO dto = result.getContent().get(0);
        assertEquals(guide1.getId(), dto.getGuideId());
        assertEquals(guide1.getTitle(), dto.getTitle());
        assertEquals(JobNameType.DESIGN, dto.getJobField().getJobName());
        assertEquals(2, dto.getHashTags().size());
        assertEquals(5L, dto.getStats().getTotalCoffeeChats());
        assertEquals(4.5, dto.getStats().getAverageStar());
        assertEquals(10L, dto.getStats().getTotalReviews());
        assertEquals(7L, dto.getStats().getThumbsUpCount());

        // 메서드 호출 검증
        verify(guideRepository).findBySearchConditions(jobFieldIds, chatTopicIds, keyword, pageable);
        verify(reservationRepository).countByGuideAndStatus(guide1, Status.COMPLETED);
        verify(reviewRepository).calculateAverageStarByGuide(guide1);
        verify(reviewRepository).countByGuide(guide1);
        verify(reviewExperienceRepository).countThumbsUpByGuide(guide1);
    }

    @Test
    @DisplayName("가이드 목록 조회 - 빈 결과")
    void getGuides_EmptyResult() {
        // 테스트 데이터 준비
        List<Long> jobFieldIds = List.of(999L); // 존재하지 않는 ID
        List<Long> chatTopicIds = null;
        String keyword = null;
        Pageable pageable = Pageable.unpaged();
        String loginMemberId = "member1";
        String sort = "latest";

        // 빈 페이지 객체 생성
        Page<Guide> emptyPage = new PageImpl<>(List.of());

        // Mock 설정
        when(guideRepository.findBySearchConditions(jobFieldIds, chatTopicIds, keyword, pageable))
                .thenReturn(emptyPage);

        // 테스트 실행
        Page<GuideListResponseDTO> result = guideService.getGuides(jobFieldIds, chatTopicIds, keyword, pageable, loginMemberId, sort);

        // 검증
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        // 메서드 호출 검증
        verify(guideRepository).findBySearchConditions(jobFieldIds, chatTopicIds, keyword, pageable);
        verifyNoInteractions(reservationRepository, reviewRepository, reviewExperienceRepository);
    }

    @Test
    @DisplayName("가이드 목록 조회 - 인기순 정렬")
    void getGuides_SortByPopularity() {
        // 테스트 데이터 준비
        List<Long> jobFieldIds = null;
        List<Long> chatTopicIds = null;
        String keyword = null;
        Pageable pageable = Pageable.unpaged();
        String loginMemberId = "member1";
        String sort = "popular";

        // guide1에 jobField, hashTags 연결
        guide1 = guide1.toBuilder()
                .guideJobField(guideJobField)
                .hashTags(List.of(hashTag1, hashTag2))
                .build();
        guideJobField = guideJobField.toBuilder()
                .guide(guide1)
                .build();

        // 두 번째 가이드 생성 (더 많은 리뷰를 가진 가이드)
        Guide guide3 = Guide.builder()
                .id(3L)
                .member(member1)
                .isOpened(true)
                .title("Guide 3")
                .build();

        GuideJobField guideJobField3 = GuideJobField.builder()
                .id(3L)
                .guide(guide3)
                .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                .build();

        // guide3에 jobField 연결
        guide3 = guide3.toBuilder()
                .guideJobField(guideJobField3)
                .build();
        guideJobField3 = guideJobField3.toBuilder()
                .guide(guide3)
                .build();

        // 페이지 객체 생성 (순서는 중요하지 않음, 서비스에서 재정렬됨)
        Page<Guide> guidePage = new PageImpl<>(List.of(guide1, guide3));

        // Mock 설정
        when(guideRepository.findBySearchConditions(jobFieldIds, chatTopicIds, keyword, pageable))
                .thenReturn(guidePage);

        // guide1 설정
        when(reservationRepository.countByGuideAndStatus(guide1, Status.COMPLETED)).thenReturn(5L);
        when(reviewRepository.calculateAverageStarByGuide(guide1)).thenReturn(java.math.BigDecimal.valueOf(4.5));
        when(reviewRepository.countByGuide(guide1)).thenReturn(10L); // 리뷰 10개
        when(reviewExperienceRepository.countThumbsUpByGuide(guide1)).thenReturn(7L);

        // guide3 설정
        when(reservationRepository.countByGuideAndStatus(guide3, Status.COMPLETED)).thenReturn(15L);
        when(reviewRepository.calculateAverageStarByGuide(guide3)).thenReturn(java.math.BigDecimal.valueOf(4.8));
        when(reviewRepository.countByGuide(guide3)).thenReturn(20L); // 리뷰 20개
        when(reviewExperienceRepository.countThumbsUpByGuide(guide3)).thenReturn(12L);

        // 테스트 실행
        Page<GuideListResponseDTO> result = guideService.getGuides(
                jobFieldIds, chatTopicIds, keyword, pageable, loginMemberId, sort);

        // 검증
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        // 인기순 정렬이므로 리뷰가 많은 guide3가 먼저 나와야 함
        GuideListResponseDTO first = result.getContent().get(0);
        GuideListResponseDTO second = result.getContent().get(1);

        assertEquals(3L, first.getGuideId());
        assertEquals(20L, first.getStats().getTotalReviews());

        assertEquals(1L, second.getGuideId());
        assertEquals(10L, second.getStats().getTotalReviews());

        // 메서드 호출 검증
        verify(guideRepository).findBySearchConditions(jobFieldIds, chatTopicIds, keyword, pageable);
    }

}
