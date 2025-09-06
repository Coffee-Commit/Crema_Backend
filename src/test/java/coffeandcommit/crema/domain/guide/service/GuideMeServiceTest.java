package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.globalTag.dto.TopicDTO;
import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.globalTag.repository.ChatTopicRepository;
import coffeandcommit.crema.domain.guide.dto.request.GuideChatTopicRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideCoffeeChatRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideExperienceDetailRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideExperienceRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideHashTagRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideJobFieldRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideScheduleRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GroupRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.ScheduleRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.TimeSlotRequestDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideChatTopicResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideCoffeeChatResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideExperienceDetailResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideExperienceResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideHashTagResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideProfileResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideScheduleResponseDTO;
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
import coffeandcommit.crema.domain.guide.repository.TimeSlotRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GuideMeServiceTest {

    @Mock
    private GuideRepository guideRepository;

    @Mock
    private GuideJobFieldRepository guideJobFieldRepository;

    @Mock
    private ChatTopicRepository chatTopicRepository;

    @Mock
    private GuideChatTopicRepository guideChatTopicRepository;

    @Mock
    private HashTagRepository hashTagRepository;

    @Mock
    private GuideScheduleRepository guideScheduleRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private ExperienceDetailRepository experienceDetailRepository;

    @Mock
    private ExperienceGroupRepository experienceGroupRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private GuideMeService guideMeService;

    private String memberId;
    private Member member;
    private Guide guide;
    private GuideJobField guideJobField;
    private ChatTopic chatTopic1;
    private ChatTopic chatTopic2;
    private GuideChatTopic guideChatTopic;
    private GuideSchedule guideSchedule;
    private TimeSlot timeSlot;
    private ExperienceDetail experienceDetail;

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
                .chatDescription("description")
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

        chatTopic1 = ChatTopic.builder()
                .id(1L)
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        chatTopic2 = ChatTopic.builder()
                .id(2L)
                .topicName(TopicNameType.JOB_CHANGE)
                .build();

        guideChatTopic = GuideChatTopic.builder()
                .id(1L)
                .guide(guide)
                .chatTopic(chatTopic1)
                .build();

        guideSchedule = GuideSchedule.builder()
                .id(1L)
                .guide(guide)
                .dayOfWeek(DayType.MONDAY)
                .timeSlots(new ArrayList<>())  // Initialize timeSlots list
                .build();

        timeSlot = TimeSlot.builder()
                .id(1L)
                .schedule(guideSchedule)
                .startTimeOption(java.time.LocalTime.of(9, 0))
                .endTimeOption(java.time.LocalTime.of(10, 0))
                .build();

        // Set up the relationship between GuideSchedule and TimeSlot
        guideSchedule.addTimeSlot(timeSlot);  // Use the helper method instead of direct access

        // Create test experience detail
        experienceDetail = ExperienceDetail.builder()
                .id(1L)
                .guide(guide)
                .who("신입 개발자")
                .solution("취업 준비")
                .how("포트폴리오 작성")
                .build();
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

    @Test
    @DisplayName("registerChatTopics 성공 테스트")
    void registerChatTopics_Success() {
        // 요청 DTO 생성
        TopicDTO topicDTO1 = TopicDTO.builder()
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        TopicDTO topicDTO2 = TopicDTO.builder()
                .topicName(TopicNameType.JOB_CHANGE)
                .build();

        List<TopicDTO> topics = Arrays.asList(topicDTO1, topicDTO2);

        GuideChatTopicRequestDTO requestDTO = GuideChatTopicRequestDTO.builder()
                .topics(topics)
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(chatTopicRepository.findByTopicName(TopicNameType.CAREER_CHANGE))
                .thenReturn(Optional.of(chatTopic1));
        when(chatTopicRepository.findByTopicName(TopicNameType.JOB_CHANGE))
                .thenReturn(Optional.of(chatTopic2));

        // 현재 주제 개수 설정
        when(guideChatTopicRepository.countByGuide(guide)).thenReturn(0L);

        // 첫 번째 주제는 이미 등록되어 있음
        when(guideChatTopicRepository.existsByGuideAndChatTopic(guide, chatTopic1))
                .thenReturn(true);

        // 두 번째 주제는 등록되어 있지 않음
        when(guideChatTopicRepository.existsByGuideAndChatTopic(guide, chatTopic2))
                .thenReturn(false);

        // 저장 시 반환할 객체 설정
        GuideChatTopic savedGuideChatTopic = GuideChatTopic.builder()
                .id(2L)
                .guide(guide)
                .chatTopic(chatTopic2)
                .build();

        when(guideChatTopicRepository.save(any(GuideChatTopic.class))).thenReturn(savedGuideChatTopic);

        // 저장 후 조회 결과 설정
        List<GuideChatTopic> guideChatTopics = Arrays.asList(guideChatTopic, savedGuideChatTopic);
        when(guideChatTopicRepository.findAllByGuideWithJoin(guide)).thenReturn(guideChatTopics);

        // 테스트 실행
        List<GuideChatTopicResponseDTO> result = guideMeService.registerChatTopics(memberId, requestDTO);

        // 검증
        assertNotNull(result);
        assertEquals(2, result.size());

        // 첫 번째 주제 검증
        assertEquals(1L, result.get(0).getId());
        assertEquals(guide.getId(), result.get(0).getGuideId());
        assertEquals(TopicNameType.CAREER_CHANGE, result.get(0).getTopic().getTopicName());

        // 두 번째 주제 검증
        assertEquals(2L, result.get(1).getId());
        assertEquals(guide.getId(), result.get(1).getGuideId());
        assertEquals(TopicNameType.JOB_CHANGE, result.get(1).getTopic().getTopicName());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(chatTopicRepository).findByTopicName(TopicNameType.CAREER_CHANGE);
        verify(chatTopicRepository).findByTopicName(TopicNameType.JOB_CHANGE);
        verify(guideChatTopicRepository).existsByGuideAndChatTopic(guide, chatTopic1);
        verify(guideChatTopicRepository).existsByGuideAndChatTopic(guide, chatTopic2);
        verify(guideChatTopicRepository).save(any(GuideChatTopic.class));
        verify(guideChatTopicRepository).findAllByGuideWithJoin(guide);
    }

    @Test
    @DisplayName("registerChatTopics 가이드 없음 테스트")
    void registerChatTopics_GuideNotFound() {
        // 요청 DTO 생성
        TopicDTO topicDTO = TopicDTO.builder()
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        GuideChatTopicRequestDTO requestDTO = GuideChatTopicRequestDTO.builder()
                .topics(List.of(topicDTO))
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerChatTopics(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(chatTopicRepository, never()).findByTopicName(any());
        verify(guideChatTopicRepository, never()).existsByGuideAndChatTopic(any(), any());
        verify(guideChatTopicRepository, never()).save(any());
        verify(guideChatTopicRepository, never()).findAllByGuideWithJoin(any());
    }

    @Test
    @DisplayName("registerChatTopics 최대 주제 개수 초과 테스트")
    void registerChatTopics_MaxTopicExceeded() {
        // 요청 DTO 생성 - 6개의 주제 (최대 5개)
        List<TopicDTO> topics = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            topics.add(TopicDTO.builder()
                    .topicName(TopicNameType.CAREER_CHANGE)
                    .build());
        }

        GuideChatTopicRequestDTO requestDTO = GuideChatTopicRequestDTO.builder()
                .topics(topics)
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideChatTopicRepository.countByGuide(guide)).thenReturn(0L);

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerChatTopics(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.MAX_TOPIC_EXCEEDED, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideChatTopicRepository).countByGuide(guide);
        verify(chatTopicRepository, never()).findByTopicName(any());
        verify(guideChatTopicRepository, never()).existsByGuideAndChatTopic(any(), any());
        verify(guideChatTopicRepository, never()).save(any());
        verify(guideChatTopicRepository, never()).findAllByGuideWithJoin(any());
    }

    @Test
    @DisplayName("registerChatTopics 유효하지 않은 주제 테스트")
    void registerChatTopics_InvalidTopic() {
        // 요청 DTO 생성
        TopicDTO topicDTO = TopicDTO.builder()
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        GuideChatTopicRequestDTO requestDTO = GuideChatTopicRequestDTO.builder()
                .topics(List.of(topicDTO))
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideChatTopicRepository.countByGuide(guide)).thenReturn(0L);
        when(chatTopicRepository.findByTopicName(TopicNameType.CAREER_CHANGE))
                .thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerChatTopics(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.INVALID_TOPIC, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideChatTopicRepository).countByGuide(guide);
        verify(chatTopicRepository).findByTopicName(TopicNameType.CAREER_CHANGE);
        verify(guideChatTopicRepository, never()).existsByGuideAndChatTopic(any(), any());
        verify(guideChatTopicRepository, never()).save(any());
        verify(guideChatTopicRepository, never()).findAllByGuideWithJoin(any());
    }

    @Test
    @DisplayName("deleteChatTopic 성공 테스트")
    void deleteChatTopic_Success() {
        // Mock 설정
        Long topicId = 1L;
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideChatTopicRepository.findById(topicId)).thenReturn(Optional.of(guideChatTopic));

        // 삭제 후 남은 주제 설정
        GuideChatTopic remainingTopic = GuideChatTopic.builder()
                .id(2L)
                .guide(guide)
                .chatTopic(chatTopic2)
                .build();

        when(guideChatTopicRepository.findAllByGuideWithJoin(guide)).thenReturn(List.of(remainingTopic));

        // 테스트 실행
        List<GuideChatTopicResponseDTO> result = guideMeService.deleteChatTopic(memberId, topicId);

        // 검증
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals(guide.getId(), result.get(0).getGuideId());
        assertEquals(TopicNameType.JOB_CHANGE, result.get(0).getTopic().getTopicName());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideChatTopicRepository).findById(topicId);
        verify(guideChatTopicRepository).delete(guideChatTopic);
        verify(guideChatTopicRepository).findAllByGuideWithJoin(guide);
    }

    @Test
    @DisplayName("deleteChatTopic 가이드 없음 테스트")
    void deleteChatTopic_GuideNotFound() {
        // Mock 설정
        Long topicId = 1L;
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteChatTopic(memberId, topicId)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideChatTopicRepository, never()).findById(anyLong());
        verify(guideChatTopicRepository, never()).delete(any());
        verify(guideChatTopicRepository, never()).findAllByGuideWithJoin(any());
    }

    @Test
    @DisplayName("deleteChatTopic 주제 없음 테스트")
    void deleteChatTopic_TopicNotFound() {
        // Mock 설정
        Long topicId = 999L;
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideChatTopicRepository.findById(topicId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteChatTopic(memberId, topicId)
        );

        assertEquals(ErrorStatus.GUIDE_CHAT_TOPIC_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideChatTopicRepository).findById(topicId);
        verify(guideChatTopicRepository, never()).delete(any());
        verify(guideChatTopicRepository, never()).findAllByGuideWithJoin(any());
    }

    @Test
    @DisplayName("deleteChatTopic 권한 없음 테스트")
    void deleteChatTopic_Forbidden() {
        // Mock 설정
        Long topicId = 1L;

        // 다른 가이드의 주제
        Guide otherGuide = Guide.builder()
                .id(2L)
                .member(Member.builder().id("other-member-id").build())
                .build();

        GuideChatTopic otherGuideTopic = GuideChatTopic.builder()
                .id(1L)
                .guide(otherGuide)
                .chatTopic(chatTopic1)
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideChatTopicRepository.findById(topicId)).thenReturn(Optional.of(otherGuideTopic));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteChatTopic(memberId, topicId)
        );

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideChatTopicRepository).findById(topicId);
        verify(guideChatTopicRepository, never()).delete(any());
        verify(guideChatTopicRepository, never()).findAllByGuideWithJoin(any());
    }

    @Test
    @DisplayName("registerGuideHashTags 성공 테스트")
    void registerGuideHashTags_Success() {
        // 요청 DTO 생성
        GuideHashTagRequestDTO hashTagRequestDTO1 = GuideHashTagRequestDTO.builder()
                .hashTagName("Java")
                .build();

        GuideHashTagRequestDTO hashTagRequestDTO2 = GuideHashTagRequestDTO.builder()
                .hashTagName("Spring")
                .build();

        List<GuideHashTagRequestDTO> requestDTOs = Arrays.asList(hashTagRequestDTO1, hashTagRequestDTO2);

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(hashTagRepository.countByGuide(guide)).thenReturn(0L);
        when(hashTagRepository.existsByGuideAndHashTagName(guide, "Java")).thenReturn(false);
        when(hashTagRepository.existsByGuideAndHashTagName(guide, "Spring")).thenReturn(false);

        // 저장 시 반환할 객체 설정
        HashTag savedHashTag1 = HashTag.builder()
                .id(1L)
                .guide(guide)
                .hashTagName("Java")
                .build();

        HashTag savedHashTag2 = HashTag.builder()
                .id(2L)
                .guide(guide)
                .hashTagName("Spring")
                .build();

        when(hashTagRepository.saveAll(anyList())).thenReturn(Arrays.asList(savedHashTag1, savedHashTag2));

        // 테스트 실행
        List<GuideHashTagResponseDTO> result = guideMeService.registerGuideHashTags(memberId, requestDTOs);

        // 검증
        assertNotNull(result);
        assertEquals(2, result.size());

        // 첫 번째 해시태그 검증
        assertEquals(1L, result.get(0).getId());
        assertEquals(guide.getId(), result.get(0).getGuideId());
        assertEquals("Java", result.get(0).getHashTagName());

        // 두 번째 해시태그 검증
        assertEquals(2L, result.get(1).getId());
        assertEquals(guide.getId(), result.get(1).getGuideId());
        assertEquals("Spring", result.get(1).getHashTagName());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(hashTagRepository).countByGuide(guide);
        verify(hashTagRepository).existsByGuideAndHashTagName(guide, "Java");
        verify(hashTagRepository).existsByGuideAndHashTagName(guide, "Spring");
        verify(hashTagRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("registerGuideHashTags 가이드 없음 테스트")
    void registerGuideHashTags_GuideNotFound() {
        // 요청 DTO 생성
        GuideHashTagRequestDTO hashTagRequestDTO = GuideHashTagRequestDTO.builder()
                .hashTagName("Java")
                .build();

        List<GuideHashTagRequestDTO> requestDTOs = List.of(hashTagRequestDTO);

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideHashTags(memberId, requestDTOs)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(hashTagRepository, never()).countByGuide(any());
        verify(hashTagRepository, never()).existsByGuideAndHashTagName(any(), anyString());
        verify(hashTagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registerGuideHashTags 최대 해시태그 개수 초과 테스트")
    void registerGuideHashTags_MaxHashTagExceeded() {
        // 요청 DTO 생성
        List<GuideHashTagRequestDTO> requestDTOs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            requestDTOs.add(GuideHashTagRequestDTO.builder()
                    .hashTagName("Tag" + i)
                    .build());
        }

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(hashTagRepository.countByGuide(guide)).thenReturn(3L); // 이미 3개의 해시태그가 있음

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideHashTags(memberId, requestDTOs)
        );

        assertEquals(ErrorStatus.MAX_HASHTAG_EXCEEDED, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(hashTagRepository).countByGuide(guide);
        verify(hashTagRepository, never()).existsByGuideAndHashTagName(any(), anyString());
        verify(hashTagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registerGuideHashTags 중복 해시태그 테스트")
    void registerGuideHashTags_DuplicateHashTag() {
        // 요청 DTO 생성
        GuideHashTagRequestDTO hashTagRequestDTO = GuideHashTagRequestDTO.builder()
                .hashTagName("Java")
                .build();

        List<GuideHashTagRequestDTO> requestDTOs = List.of(hashTagRequestDTO);

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(hashTagRepository.countByGuide(guide)).thenReturn(0L);
        when(hashTagRepository.existsByGuideAndHashTagName(guide, "Java")).thenReturn(true); // 이미 존재하는 해시태그

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideHashTags(memberId, requestDTOs)
        );

        assertEquals(ErrorStatus.DUPLICATE_HASHTAG, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(hashTagRepository).countByGuide(guide);
        verify(hashTagRepository).existsByGuideAndHashTagName(guide, "Java");
        verify(hashTagRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("deleteGuideHashTag 성공 테스트")
    void deleteGuideHashTag_Success() {
        // Mock 설정
        Long hashTagId = 1L;
        HashTag hashTag = HashTag.builder()
                .id(hashTagId)
                .guide(guide)
                .hashTagName("Java")
                .build();

        HashTag remainingHashTag = HashTag.builder()
                .id(2L)
                .guide(guide)
                .hashTagName("Spring")
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(hashTagRepository.findById(hashTagId)).thenReturn(Optional.of(hashTag));
        when(hashTagRepository.findByGuide(guide)).thenReturn(List.of(remainingHashTag));

        // 테스트 실행
        List<GuideHashTagResponseDTO> result = guideMeService.deleteGuideHashTag(memberId, hashTagId);

        // 검증
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals(guide.getId(), result.get(0).getGuideId());
        assertEquals("Spring", result.get(0).getHashTagName());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(hashTagRepository).findById(hashTagId);
        verify(hashTagRepository).delete(hashTag);
        verify(hashTagRepository).findByGuide(guide);
    }

    @Test
    @DisplayName("deleteGuideHashTag 가이드 없음 테스트")
    void deleteGuideHashTag_GuideNotFound() {
        // Mock 설정
        Long hashTagId = 1L;
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteGuideHashTag(memberId, hashTagId)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(hashTagRepository, never()).findById(anyLong());
        verify(hashTagRepository, never()).delete(any());
        verify(hashTagRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("deleteGuideHashTag 해시태그 없음 테스트")
    void deleteGuideHashTag_HashTagNotFound() {
        // Mock 설정
        Long hashTagId = 999L;
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(hashTagRepository.findById(hashTagId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteGuideHashTag(memberId, hashTagId)
        );

        assertEquals(ErrorStatus.HASHTAG_NOT_FOUND, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(hashTagRepository).findById(hashTagId);
        verify(hashTagRepository, never()).delete(any());
        verify(hashTagRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("deleteGuideHashTag 권한 없음 테스트")
    void deleteGuideHashTag_Forbidden() {
        // Mock 설정
        Long hashTagId = 1L;

        // 다른 가이드의 해시태그
        Guide otherGuide = Guide.builder()
                .id(2L)
                .member(Member.builder().id("other-member-id").build())
                .build();

        HashTag otherGuideHashTag = HashTag.builder()
                .id(hashTagId)
                .guide(otherGuide)
                .hashTagName("Java")
                .build();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(hashTagRepository.findById(hashTagId)).thenReturn(Optional.of(otherGuideHashTag));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteGuideHashTag(memberId, hashTagId)
        );

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(hashTagRepository).findById(hashTagId);
        verify(hashTagRepository, never()).delete(any());
        verify(hashTagRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("deleteGuideSchedule 성공 테스트 - 시간대만 삭제")
    void deleteGuideSchedule_Success_DeleteTimeSlotOnly() {
        // Mock 설정
        Long timeSlotId = 1L;

        // 두 개의 시간대가 있는 스케줄 생성
        TimeSlot secondTimeSlot = TimeSlot.builder()
                .id(2L)
                .schedule(guideSchedule)
                .startTimeOption(java.time.LocalTime.of(11, 0))
                .endTimeOption(java.time.LocalTime.of(12, 0))
                .build();

        guideSchedule.getTimeSlots().add(secondTimeSlot);

        List<GuideSchedule> remainingSchedules = new ArrayList<>();
        remainingSchedules.add(guideSchedule);

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(guideScheduleRepository.findByGuide(guide)).thenReturn(remainingSchedules);

        // 테스트 실행
        GuideScheduleResponseDTO result = guideMeService.deleteGuideSchedule(memberId, timeSlotId);

        // 검증
        assertNotNull(result);
        assertEquals(guide.getId(), result.getGuideId());
        assertEquals(1, result.getSchedules().size());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(timeSlotRepository).findById(timeSlotId);
        verify(timeSlotRepository).delete(timeSlot);
        verify(guideScheduleRepository, never()).delete(any());
        verify(guideScheduleRepository).findByGuide(guide);
    }

    @Test
    @DisplayName("deleteGuideSchedule 성공 테스트 - 스케줄 전체 삭제")
    void deleteGuideSchedule_Success_DeleteEntireSchedule() {
        // Mock 설정
        Long timeSlotId = 1L;

        // 시간대가 하나뿐인 스케줄
        guideSchedule.getTimeSlots().clear();
        guideSchedule.getTimeSlots().add(timeSlot);

        List<GuideSchedule> remainingSchedules = new ArrayList<>();

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(guideScheduleRepository.findByGuide(guide)).thenReturn(remainingSchedules);

        // 테스트 실행
        GuideScheduleResponseDTO result = guideMeService.deleteGuideSchedule(memberId, timeSlotId);

        // 검증
        assertNotNull(result);
        assertEquals(guide.getId(), result.getGuideId());
        assertTrue(result.getSchedules().isEmpty());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(timeSlotRepository).findById(timeSlotId);
        verify(guideScheduleRepository).delete(guideSchedule);
        verify(timeSlotRepository, never()).delete(any());
        verify(guideScheduleRepository).findByGuide(guide);
    }

    @Test
    @DisplayName("deleteGuideSchedule 가이드 없음 테스트")
    void deleteGuideSchedule_GuideNotFound() {
        // Mock 설정
        Long timeSlotId = 1L;
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteGuideSchedule(memberId, timeSlotId)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(timeSlotRepository, never()).findById(anyLong());
        verify(guideScheduleRepository, never()).delete(any());
        verify(timeSlotRepository, never()).delete(any());
        verify(guideScheduleRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("deleteGuideSchedule 시간대 없음 테스트")
    void deleteGuideSchedule_TimeSlotNotFound() {
        // Mock 설정
        Long timeSlotId = 999L;
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteGuideSchedule(memberId, timeSlotId)
        );

        assertEquals(ErrorStatus.TIME_SLOT_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(timeSlotRepository).findById(timeSlotId);
        verify(guideScheduleRepository, never()).delete(any());
        verify(timeSlotRepository, never()).delete(any());
        verify(guideScheduleRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("deleteGuideSchedule 권한 없음 테스트")
    void deleteGuideSchedule_Forbidden() {
        // Mock 설정
        Long timeSlotId = 1L;

        // 다른 가이드의 스케줄과 시간대
        Guide otherGuide = Guide.builder()
                .id(2L)
                .member(Member.builder().id("other-member-id").build())
                .build();

        GuideSchedule otherGuideSchedule = GuideSchedule.builder()
                .id(2L)
                .guide(otherGuide)
                .dayOfWeek(DayType.TUESDAY)
                .timeSlots(new ArrayList<>())
                .build();

        TimeSlot otherTimeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .schedule(otherGuideSchedule)
                .startTimeOption(java.time.LocalTime.of(13, 0))
                .endTimeOption(java.time.LocalTime.of(14, 0))
                .build();

        otherGuideSchedule.getTimeSlots().add(otherTimeSlot);

        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(otherTimeSlot));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteGuideSchedule(memberId, timeSlotId)
        );

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(timeSlotRepository).findById(timeSlotId);
        verify(guideScheduleRepository, never()).delete(any());
        verify(timeSlotRepository, never()).delete(any());
        verify(guideScheduleRepository, never()).findByGuide(any());
    }

    @Test
    @DisplayName("registerGuideSchedules 성공 테스트")
    void registerGuideSchedules_Success() {
        // 테스트 데이터 준비
        TimeSlotRequestDTO timeSlotRequestDTO1 = TimeSlotRequestDTO.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        TimeSlotRequestDTO timeSlotRequestDTO2 = TimeSlotRequestDTO.builder()
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .build();

        ScheduleRequestDTO scheduleRequestDTO = ScheduleRequestDTO.builder()
                .dayOfWeek(DayType.MONDAY)
                .timeSlots(List.of(timeSlotRequestDTO1, timeSlotRequestDTO2))
                .build();

        GuideScheduleRequestDTO requestDTO = GuideScheduleRequestDTO.builder()
                .schedules(List.of(scheduleRequestDTO))
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));

        // 저장된 스케줄 생성
        List<TimeSlot> timeSlotList = new ArrayList<>();

        GuideSchedule savedSchedule = GuideSchedule.builder()
                .id(2L)
                .guide(guide)
                .dayOfWeek(DayType.MONDAY)
                .timeSlots(timeSlotList)  // Initialize with the list we created
                .build();

        TimeSlot savedTimeSlot1 = TimeSlot.builder()
                .id(2L)
                .schedule(savedSchedule)
                .startTimeOption(java.time.LocalTime.of(9, 0))
                .endTimeOption(java.time.LocalTime.of(10, 0))
                .build();

        TimeSlot savedTimeSlot2 = TimeSlot.builder()
                .id(3L)
                .schedule(savedSchedule)
                .startTimeOption(java.time.LocalTime.of(14, 0))
                .endTimeOption(java.time.LocalTime.of(15, 0))
                .build();

        // Add time slots to the list
        timeSlotList.add(savedTimeSlot1);
        timeSlotList.add(savedTimeSlot2);

        when(guideScheduleRepository.saveAll(any())).thenReturn(List.of(savedSchedule));

        // 테스트 실행
        GuideScheduleResponseDTO result = guideMeService.registerGuideSchedules(memberId, requestDTO);

        // 검증
        assertNotNull(result);
        assertEquals(guide.getId(), result.getGuideId());
        assertEquals(1, result.getSchedules().size());
        assertEquals(DayType.MONDAY, result.getSchedules().get(0).getDayOfWeek());
        assertEquals(2, result.getSchedules().get(0).getTimeSlots().size());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideScheduleRepository).saveAll(any());
    }

    @Test
    @DisplayName("registerGuideSchedules 가이드 없음 테스트")
    void registerGuideSchedules_GuideNotFound() {
        // 테스트 데이터 준비
        TimeSlotRequestDTO timeSlotRequestDTO = TimeSlotRequestDTO.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        ScheduleRequestDTO scheduleRequestDTO = ScheduleRequestDTO.builder()
                .dayOfWeek(DayType.MONDAY)
                .timeSlots(List.of(timeSlotRequestDTO))
                .build();

        GuideScheduleRequestDTO requestDTO = GuideScheduleRequestDTO.builder()
                .schedules(List.of(scheduleRequestDTO))
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideSchedules(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideScheduleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("registerGuideSchedules 유효하지 않은 시간 범위 테스트")
    void registerGuideSchedules_InvalidTimeRange() {
        // 테스트 데이터 준비 - 시작 시간이 종료 시간보다 늦은 경우
        TimeSlotRequestDTO invalidTimeSlotRequestDTO = TimeSlotRequestDTO.builder()
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(9, 0))
                .build();

        ScheduleRequestDTO scheduleRequestDTO = ScheduleRequestDTO.builder()
                .dayOfWeek(DayType.MONDAY)
                .timeSlots(List.of(invalidTimeSlotRequestDTO))
                .build();

        GuideScheduleRequestDTO requestDTO = GuideScheduleRequestDTO.builder()
                .schedules(List.of(scheduleRequestDTO))
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideSchedules(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.INVALID_TIME_RANGE, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideScheduleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("registerGuideSchedules 동일한 시작/종료 시간 테스트")
    void registerGuideSchedules_SameStartEndTime() {
        // 테스트 데이터 준비 - 시작 시간과 종료 시간이 같은 경우
        TimeSlotRequestDTO sameTimeSlotRequestDTO = TimeSlotRequestDTO.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(9, 0))
                .build();

        ScheduleRequestDTO scheduleRequestDTO = ScheduleRequestDTO.builder()
                .dayOfWeek(DayType.MONDAY)
                .timeSlots(List.of(sameTimeSlotRequestDTO))
                .build();

        GuideScheduleRequestDTO requestDTO = GuideScheduleRequestDTO.builder()
                .schedules(List.of(scheduleRequestDTO))
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideSchedules(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.INVALID_TIME_RANGE, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideScheduleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("registerExperienceDetail 성공 테스트 - 새로 생성")
    void registerExperienceDetail_CreateNew_Success() {
        // 테스트 데이터 준비
        GuideExperienceDetailRequestDTO requestDTO = GuideExperienceDetailRequestDTO.builder()
                .who("신입 개발자")
                .solution("취업 준비")
                .how("포트폴리오 작성")
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceDetailRepository.findByGuide(guide)).thenReturn(Optional.empty());
        when(experienceDetailRepository.save(any(ExperienceDetail.class))).thenAnswer(invocation -> {
            ExperienceDetail savedDetail = invocation.getArgument(0);
            return ExperienceDetail.builder()
                    .id(1L)
                    .guide(savedDetail.getGuide())
                    .who(savedDetail.getWho())
                    .solution(savedDetail.getSolution())
                    .how(savedDetail.getHow())
                    .build();
        });

        // 테스트 실행
        GuideExperienceDetailResponseDTO result = guideMeService.registerExperienceDetail(memberId, requestDTO);

        // 검증
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(guide.getId(), result.getGuideId());
        assertEquals(requestDTO.getWho(), result.getWho());
        assertEquals(requestDTO.getSolution(), result.getSolution());
        assertEquals(requestDTO.getHow(), result.getHow());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceDetailRepository).findByGuide(guide);
        verify(experienceDetailRepository).save(any(ExperienceDetail.class));
    }

    @Test
    @DisplayName("registerExperienceDetail 성공 테스트 - 기존 업데이트")
    void registerExperienceDetail_Update_Success() {
        // 테스트 데이터 준비
        GuideExperienceDetailRequestDTO requestDTO = GuideExperienceDetailRequestDTO.builder()
                .who("경력 개발자")
                .solution("이직 준비")
                .how("기술 면접 준비")
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceDetailRepository.findByGuide(guide)).thenReturn(Optional.of(experienceDetail));
        when(experienceDetailRepository.save(any(ExperienceDetail.class))).thenAnswer(invocation -> {
            ExperienceDetail savedDetail = invocation.getArgument(0);
            return ExperienceDetail.builder()
                    .id(experienceDetail.getId())
                    .guide(savedDetail.getGuide())
                    .who(savedDetail.getWho())
                    .solution(savedDetail.getSolution())
                    .how(savedDetail.getHow())
                    .build();
        });

        // 테스트 실행
        GuideExperienceDetailResponseDTO result = guideMeService.registerExperienceDetail(memberId, requestDTO);

        // 검증
        assertNotNull(result);
        assertEquals(experienceDetail.getId(), result.getId());
        assertEquals(guide.getId(), result.getGuideId());
        assertEquals(requestDTO.getWho(), result.getWho());
        assertEquals(requestDTO.getSolution(), result.getSolution());
        assertEquals(requestDTO.getHow(), result.getHow());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceDetailRepository).findByGuide(guide);
        verify(experienceDetailRepository).save(any(ExperienceDetail.class));
    }

    @Test
    @DisplayName("registerExperienceDetail 가이드 없음 테스트")
    void registerExperienceDetail_GuideNotFound() {
        // 테스트 데이터 준비
        GuideExperienceDetailRequestDTO requestDTO = GuideExperienceDetailRequestDTO.builder()
                .who("신입 개발자")
                .solution("취업 준비")
                .how("포트폴리오 작성")
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerExperienceDetail(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceDetailRepository, never()).findByGuide(any(Guide.class));
        verify(experienceDetailRepository, never()).save(any(ExperienceDetail.class));
    }

    @Test
    @DisplayName("deleteExperienceDetail 성공 테스트")
    void deleteExperienceDetail_Success() {
        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceDetailRepository.findById(1L)).thenReturn(Optional.of(experienceDetail));
        doNothing().when(experienceDetailRepository).delete(experienceDetail);

        // 테스트 실행
        GuideExperienceDetailResponseDTO result = guideMeService.deleteExperienceDetail(memberId, 1L);

        // 검증
        assertNotNull(result);
        assertEquals(experienceDetail.getId(), result.getId());
        assertEquals(guide.getId(), result.getGuideId());
        assertEquals(experienceDetail.getWho(), result.getWho());
        assertEquals(experienceDetail.getSolution(), result.getSolution());
        assertEquals(experienceDetail.getHow(), result.getHow());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceDetailRepository).findById(1L);
        verify(experienceDetailRepository).delete(experienceDetail);
    }

    @Test
    @DisplayName("deleteExperienceDetail 가이드 없음 테스트")
    void deleteExperienceDetail_GuideNotFound() {
        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteExperienceDetail(memberId, 1L)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceDetailRepository, never()).findById(anyLong());
        verify(experienceDetailRepository, never()).delete(any(ExperienceDetail.class));
    }

    @Test
    @DisplayName("deleteExperienceDetail 경험 소주제 없음 테스트")
    void deleteExperienceDetail_ExperienceDetailNotFound() {
        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceDetailRepository.findById(999L)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteExperienceDetail(memberId, 999L)
        );

        assertEquals(ErrorStatus.EXPERIENCE_DETAIL_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceDetailRepository).findById(999L);
        verify(experienceDetailRepository, never()).delete(any(ExperienceDetail.class));
    }

    @Test
    @DisplayName("deleteExperienceDetail 접근 권한 없음 테스트")
    void deleteExperienceDetail_Forbidden() {
        // 다른 가이드의 경험 소주제 생성
        Guide otherGuide = Guide.builder()
                .id(2L)
                .member(Member.builder().id("other-member-id").build())
                .isOpened(true)
                .build();

        ExperienceDetail otherExperienceDetail = ExperienceDetail.builder()
                .id(2L)
                .guide(otherGuide)
                .who("다른 가이드의 소주제")
                .solution("다른 가이드의 해결책")
                .how("다른 가이드의 방법")
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceDetailRepository.findById(2L)).thenReturn(Optional.of(otherExperienceDetail));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteExperienceDetail(memberId, 2L)
        );

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceDetailRepository).findById(2L);
        verify(experienceDetailRepository, never()).delete(any(ExperienceDetail.class));
    }

    @Test
    @DisplayName("deleteGuideExperience 성공 테스트")
    void deleteGuideExperience_Success() {
        // 테스트 데이터 준비
        Long experienceId = 1L;
        ExperienceGroup experienceGroup = ExperienceGroup.builder()
                .id(experienceId)
                .guide(guide)
                .guideChatTopic(guideChatTopic)
                .experienceTitle("경험 제목")
                .experienceContent("경험 내용")
                .build();

        List<ExperienceGroup> remainingGroups = new ArrayList<>();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceGroupRepository.findById(experienceId)).thenReturn(Optional.of(experienceGroup));
        when(experienceGroupRepository.findByGuide(guide)).thenReturn(remainingGroups);

        // 테스트 실행
        GuideExperienceResponseDTO result = guideMeService.deleteGuideExperience(memberId, experienceId);

        // 검증
        assertNotNull(result);
        assertTrue(result.getGroups().isEmpty());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceGroupRepository).findById(experienceId);
        verify(experienceGroupRepository).delete(experienceGroup);
        verify(experienceGroupRepository).findByGuide(guide);
    }

    @Test
    @DisplayName("deleteGuideExperience 가이드 없음 테스트")
    void deleteGuideExperience_GuideNotFound() {
        // Mock 설정
        Long experienceId = 1L;
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteGuideExperience(memberId, experienceId)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceGroupRepository, never()).findById(anyLong());
        verify(experienceGroupRepository, never()).delete(any(ExperienceGroup.class));
        verify(experienceGroupRepository, never()).findByGuide(any(Guide.class));
    }

    @Test
    @DisplayName("deleteGuideExperience 경험 없음 테스트")
    void deleteGuideExperience_ExperienceNotFound() {
        // Mock 설정
        Long experienceId = 999L;
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceGroupRepository.findById(experienceId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteGuideExperience(memberId, experienceId)
        );

        assertEquals(ErrorStatus.EXPERIENCE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceGroupRepository).findById(experienceId);
        verify(experienceGroupRepository, never()).delete(any(ExperienceGroup.class));
        verify(experienceGroupRepository, never()).findByGuide(any(Guide.class));
    }

    @Test
    @DisplayName("deleteGuideExperience 접근 권한 없음 테스트")
    void deleteGuideExperience_Forbidden() {
        // 다른 가이드의 경험 생성
        Guide otherGuide = Guide.builder()
                .id(2L)
                .member(Member.builder().id("other-member-id").build())
                .isOpened(true)
                .build();

        Long experienceId = 1L;
        ExperienceGroup otherExperienceGroup = ExperienceGroup.builder()
                .id(experienceId)
                .guide(otherGuide)
                .guideChatTopic(guideChatTopic)
                .experienceTitle("다른 가이드의 경험")
                .experienceContent("다른 가이드의 경험 내용")
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceGroupRepository.findById(experienceId)).thenReturn(Optional.of(otherExperienceGroup));

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.deleteGuideExperience(memberId, experienceId)
        );

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceGroupRepository).findById(experienceId);
        verify(experienceGroupRepository, never()).delete(any(ExperienceGroup.class));
        verify(experienceGroupRepository, never()).findByGuide(any(Guide.class));
    }

    @Test
    @DisplayName("registerGuideExperience 성공 테스트")
    void registerGuideExperience_Success() {
        // 테스트 데이터 준비
        GroupRequestDTO groupRequestDTO1 = GroupRequestDTO.builder()
                .guideChatTopicId(guideChatTopic.getId())
                .experienceTitle("첫 번째 경험")
                .experienceContent("첫 번째 경험 내용")
                .build();

        GroupRequestDTO groupRequestDTO2 = GroupRequestDTO.builder()
                .guideChatTopicId(guideChatTopic.getId())
                .experienceTitle("두 번째 경험")
                .experienceContent("두 번째 경험 내용")
                .build();

        List<GroupRequestDTO> groupRequestDTOs = Arrays.asList(groupRequestDTO1, groupRequestDTO2);

        GuideExperienceRequestDTO requestDTO = GuideExperienceRequestDTO.builder()
                .groups(groupRequestDTOs)
                .build();

        // 저장될 ExperienceGroup 객체들
        ExperienceGroup savedGroup1 = ExperienceGroup.builder()
                .id(1L)
                .guide(guide)
                .guideChatTopic(guideChatTopic)
                .experienceTitle(groupRequestDTO1.getExperienceTitle())
                .experienceContent(groupRequestDTO1.getExperienceContent())
                .build();

        ExperienceGroup savedGroup2 = ExperienceGroup.builder()
                .id(2L)
                .guide(guide)
                .guideChatTopic(guideChatTopic)
                .experienceTitle(groupRequestDTO2.getExperienceTitle())
                .experienceContent(groupRequestDTO2.getExperienceContent())
                .build();

        List<ExperienceGroup> savedGroups = Arrays.asList(savedGroup1, savedGroup2);

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceGroupRepository.countByGuide(guide)).thenReturn(0L);
        when(guideChatTopicRepository.findById(guideChatTopic.getId())).thenReturn(Optional.of(guideChatTopic));
        when(experienceGroupRepository.saveAll(any())).thenReturn(savedGroups);

        // 테스트 실행
        GuideExperienceResponseDTO result = guideMeService.registerGuideExperience(memberId, requestDTO);

        // 검증
        assertNotNull(result);
        assertEquals(2, result.getGroups().size());

        // 첫 번째 그룹 검증
        assertEquals(1L, result.getGroups().get(0).getId());
        assertEquals(guideChatTopic.getId(), result.getGroups().get(0).getGuideChatTopicId());
        assertEquals("첫 번째 경험", result.getGroups().get(0).getExperienceTitle());
        assertEquals("첫 번째 경험 내용", result.getGroups().get(0).getExperienceContent());

        // 두 번째 그룹 검증
        assertEquals(2L, result.getGroups().get(1).getId());
        assertEquals(guideChatTopic.getId(), result.getGroups().get(1).getGuideChatTopicId());
        assertEquals("두 번째 경험", result.getGroups().get(1).getExperienceTitle());
        assertEquals("두 번째 경험 내용", result.getGroups().get(1).getExperienceContent());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceGroupRepository).countByGuide(guide);
        verify(guideChatTopicRepository, times(2)).findById(guideChatTopic.getId());
        verify(experienceGroupRepository).saveAll(any());
    }

    @Test
    @DisplayName("registerGuideExperience 가이드 없음 테스트")
    void registerGuideExperience_GuideNotFound() {
        // 테스트 데이터 준비
        GroupRequestDTO groupRequestDTO = GroupRequestDTO.builder()
                .guideChatTopicId(1L)
                .experienceTitle("경험 제목")
                .experienceContent("경험 내용")
                .build();

        GuideExperienceRequestDTO requestDTO = GuideExperienceRequestDTO.builder()
                .groups(List.of(groupRequestDTO))
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideExperience(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceGroupRepository, never()).countByGuide(any());
        verify(guideChatTopicRepository, never()).findById(anyLong());
        verify(experienceGroupRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("registerGuideExperience 경험 개수 초과 테스트")
    void registerGuideExperience_ExperienceLimitExceeded() {
        // 테스트 데이터 준비 - 7개의 경험 그룹 (최대 6개)
        List<GroupRequestDTO> groupRequestDTOs = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            groupRequestDTOs.add(GroupRequestDTO.builder()
                    .guideChatTopicId(1L)
                    .experienceTitle("경험 제목 " + i)
                    .experienceContent("경험 내용 " + i)
                    .build());
        }

        GuideExperienceRequestDTO requestDTO = GuideExperienceRequestDTO.builder()
                .groups(groupRequestDTOs)
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceGroupRepository.countByGuide(guide)).thenReturn(0L);

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideExperience(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.EXPERIENCE_LIMIT_EXCEEDED, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceGroupRepository).countByGuide(guide);
        verify(guideChatTopicRepository, never()).findById(anyLong());
        verify(experienceGroupRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("registerGuideExperience 유효하지 않은 가이드 채팅 주제 테스트")
    void registerGuideExperience_InvalidGuideChatTopic() {
        // 테스트 데이터 준비
        GroupRequestDTO groupRequestDTO = GroupRequestDTO.builder()
                .guideChatTopicId(999L) // 존재하지 않는 ID
                .experienceTitle("경험 제목")
                .experienceContent("경험 내용")
                .build();

        GuideExperienceRequestDTO requestDTO = GuideExperienceRequestDTO.builder()
                .groups(List.of(groupRequestDTO))
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(experienceGroupRepository.countByGuide(guide)).thenReturn(0L);
        when(guideChatTopicRepository.findById(999L)).thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerGuideExperience(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.INVALID_GUIDE_CHAT_TOPIC, exception.getErrorCode());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(experienceGroupRepository).countByGuide(guide);
        verify(guideChatTopicRepository).findById(999L);
        verify(experienceGroupRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("registerGuideCoffeeChat 성공 테스트")
    void registerGuideCoffeeChat_Success() {
        // Given
        String loginMemberId = memberId;
        GuideCoffeeChatRequestDTO requestDTO = GuideCoffeeChatRequestDTO.builder()
                .title("New Coffee Chat Title")
                .chatDescription("New Coffee Chat Description")
                .build();

        // 기존 guide (조회 시 반환)
        Guide guide = Guide.builder()
                .id(1L)
                .member(member)
                .title("Old Title")
                .chatDescription("Old Description")
                .isOpened(false)
                .build();

        // 업데이트된 guide (save 시 반환)
        Guide updatedGuide = Guide.builder()
                .id(1L) // ID 동일
                .member(member)
                .title("New Coffee Chat Title")
                .chatDescription("New Coffee Chat Description")
                .isOpened(true)
                .build();

        // 해시태그
        List<HashTag> hashTags = Arrays.asList(
                HashTag.builder().id(1L).guide(updatedGuide).hashTagName("Java").build(),
                HashTag.builder().id(2L).guide(updatedGuide).hashTagName("Spring").build()
        );

        // 경험 그룹 & 상세
        List<ExperienceGroup> experienceGroups = new ArrayList<>();

        // Stubbing
        when(guideRepository.findByMember_Id(loginMemberId)).thenReturn(Optional.of(guide));
        when(guideRepository.save(any(Guide.class))).thenReturn(updatedGuide);

        when(hashTagRepository.findByGuide(any(Guide.class))).thenReturn(hashTags);
        when(reviewRepository.getAverageScoreByGuideId(anyLong())).thenReturn(4.5);
        when(reviewRepository.countByGuideId(anyLong())).thenReturn(10L);
        when(experienceGroupRepository.findByGuide(any(Guide.class))).thenReturn(experienceGroups);
        when(experienceDetailRepository.findByGuide(any(Guide.class))).thenReturn(Optional.of(experienceDetail));

        // When
        GuideCoffeeChatResponseDTO result = guideMeService.registerGuideCoffeeChat(loginMemberId, requestDTO);

        // Then
        assertNotNull(result);
        assertEquals("New Coffee Chat Title", result.getTitle());
        assertEquals("New Coffee Chat Description", result.getChatDescription());
        assertTrue(result.isOpened()); // 등록 시 무조건 true
        assertEquals(4.5, result.getReviewScore());
        assertEquals(10L, result.getReviewCount());
        assertNotNull(result.getTags());
        assertEquals(2, result.getTags().size());
        assertNotNull(result.getExperienceDetail());

        // Verify
        verify(guideRepository).findByMember_Id(loginMemberId);
        verify(guideRepository).save(any(Guide.class));
        verify(hashTagRepository).findByGuide(any(Guide.class));
        verify(reviewRepository).getAverageScoreByGuideId(anyLong());
        verify(reviewRepository).countByGuideId(anyLong());
        verify(experienceGroupRepository).findByGuide(any(Guide.class));
        verify(experienceDetailRepository).findByGuide(any(Guide.class));
    }


    @Test
    @DisplayName("registerGuideCoffeeChat 가이드 없음 테스트")
    void registerGuideCoffeeChat_GuideNotFound() {
        // Given
        String loginMemberId = "nonexistent-member-id";
        GuideCoffeeChatRequestDTO requestDTO = GuideCoffeeChatRequestDTO.builder()
                .title("New Coffee Chat Title")
                .chatDescription("New Coffee Chat Description")
                .build();

        when(guideRepository.findByMember_Id(loginMemberId)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> 
            guideMeService.registerGuideCoffeeChat(loginMemberId, requestDTO)
        );

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());

        // Verify
        verify(guideRepository).findByMember_Id(loginMemberId);
        verifyNoMoreInteractions(guideRepository);
        verifyNoInteractions(hashTagRepository, reviewRepository, experienceGroupRepository, experienceDetailRepository);
    }

    @Test
    @DisplayName("registerGuideCoffeeChat 권한 없음 테스트")
    void registerGuideCoffeeChat_Forbidden() {
        // Given
        String loginMemberId = "different-member-id";
        GuideCoffeeChatRequestDTO requestDTO = GuideCoffeeChatRequestDTO.builder()
                .title("New Coffee Chat Title")
                .chatDescription("New Coffee Chat Description")
                .build();

        Member differentMember = Member.builder()
                .id("different-member-id")
                .nickname("Different Member")
                .build();

        Guide guideWithDifferentOwner = Guide.builder()
                .id(1L)
                .member(member) // Original member, not the login member
                .title("Original Title")
                .chatDescription("Original Description")
                .isOpened(true)
                .build();

        when(guideRepository.findByMember_Id(loginMemberId)).thenReturn(Optional.of(guideWithDifferentOwner));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> 
            guideMeService.registerGuideCoffeeChat(loginMemberId, requestDTO)
        );

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());

        // Verify
        verify(guideRepository).findByMember_Id(loginMemberId);
        verifyNoMoreInteractions(guideRepository);
        verifyNoInteractions(hashTagRepository, reviewRepository, experienceGroupRepository, experienceDetailRepository);
    }
}
