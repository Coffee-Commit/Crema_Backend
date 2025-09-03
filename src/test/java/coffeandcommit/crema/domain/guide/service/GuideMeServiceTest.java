package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.globalTag.dto.TopicDTO;
import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.domain.globalTag.enums.ChatTopicType;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.globalTag.repository.ChatTopicRepository;
import coffeandcommit.crema.domain.guide.dto.request.GuideChatTopicRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideHashTagRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideJobFieldRequestDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideChatTopicResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideHashTagResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideProfileResponseDTO;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideChatTopic;
import coffeandcommit.crema.domain.guide.entity.GuideJobField;
import coffeandcommit.crema.domain.guide.entity.HashTag;
import coffeandcommit.crema.domain.guide.repository.GuideChatTopicRepository;
import coffeandcommit.crema.domain.guide.repository.GuideJobFieldRepository;
import coffeandcommit.crema.domain.guide.repository.GuideRepository;
import coffeandcommit.crema.domain.guide.repository.HashTagRepository;
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

    @InjectMocks
    private GuideMeService guideMeService;

    private String memberId;
    private Member member;
    private Guide guide;
    private GuideJobField guideJobField;
    private ChatTopic chatTopic1;
    private ChatTopic chatTopic2;
    private GuideChatTopic guideChatTopic;

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
                .chatTopic(ChatTopicType.CAREER)
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        chatTopic2 = ChatTopic.builder()
                .id(2L)
                .chatTopic(ChatTopicType.CAREER)
                .topicName(TopicNameType.JOB_CHANGE)
                .build();

        guideChatTopic = GuideChatTopic.builder()
                .id(1L)
                .guide(guide)
                .chatTopic(chatTopic1)
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

    @Test
    @DisplayName("registerChatTopics 성공 테스트")
    void registerChatTopics_Success() {
        // 요청 DTO 생성
        TopicDTO topicDTO1 = TopicDTO.builder()
                .chatTopic(ChatTopicType.CAREER)
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        TopicDTO topicDTO2 = TopicDTO.builder()
                .chatTopic(ChatTopicType.CAREER)
                .topicName(TopicNameType.JOB_CHANGE)
                .build();

        List<TopicDTO> topics = Arrays.asList(topicDTO1, topicDTO2);

        GuideChatTopicRequestDTO requestDTO = GuideChatTopicRequestDTO.builder()
                .topics(topics)
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(chatTopicRepository.findByChatTopicAndTopicName(ChatTopicType.CAREER, TopicNameType.CAREER_CHANGE))
                .thenReturn(Optional.of(chatTopic1));
        when(chatTopicRepository.findByChatTopicAndTopicName(ChatTopicType.CAREER, TopicNameType.JOB_CHANGE))
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
        assertEquals(ChatTopicType.CAREER, result.get(0).getTopic().getChatTopic());
        assertEquals(TopicNameType.CAREER_CHANGE, result.get(0).getTopic().getTopicName());

        // 두 번째 주제 검증
        assertEquals(2L, result.get(1).getId());
        assertEquals(guide.getId(), result.get(1).getGuideId());
        assertEquals(ChatTopicType.CAREER, result.get(1).getTopic().getChatTopic());
        assertEquals(TopicNameType.JOB_CHANGE, result.get(1).getTopic().getTopicName());

        // 메서드 호출 검증
        verify(guideRepository).findByMember_Id(memberId);
        verify(chatTopicRepository).findByChatTopicAndTopicName(ChatTopicType.CAREER, TopicNameType.CAREER_CHANGE);
        verify(chatTopicRepository).findByChatTopicAndTopicName(ChatTopicType.CAREER, TopicNameType.JOB_CHANGE);
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
                .chatTopic(ChatTopicType.CAREER)
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
        verify(chatTopicRepository, never()).findByChatTopicAndTopicName(any(), any());
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
                    .chatTopic(ChatTopicType.CAREER)
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
        verify(chatTopicRepository, never()).findByChatTopicAndTopicName(any(), any());
        verify(guideChatTopicRepository, never()).existsByGuideAndChatTopic(any(), any());
        verify(guideChatTopicRepository, never()).save(any());
        verify(guideChatTopicRepository, never()).findAllByGuideWithJoin(any());
    }

    @Test
    @DisplayName("registerChatTopics 유효하지 않은 주제 테스트")
    void registerChatTopics_InvalidTopic() {
        // 요청 DTO 생성
        TopicDTO topicDTO = TopicDTO.builder()
                .chatTopic(ChatTopicType.CAREER)
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        GuideChatTopicRequestDTO requestDTO = GuideChatTopicRequestDTO.builder()
                .topics(List.of(topicDTO))
                .build();

        // Mock 설정
        when(guideRepository.findByMember_Id(memberId)).thenReturn(Optional.of(guide));
        when(guideChatTopicRepository.countByGuide(guide)).thenReturn(0L);
        when(chatTopicRepository.findByChatTopicAndTopicName(ChatTopicType.CAREER, TopicNameType.CAREER_CHANGE))
                .thenReturn(Optional.empty());

        // 테스트 실행 및 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                guideMeService.registerChatTopics(memberId, requestDTO)
        );

        assertEquals(ErrorStatus.INVALID_TOPIC, exception.getErrorCode());
        verify(guideRepository).findByMember_Id(memberId);
        verify(guideChatTopicRepository).countByGuide(guide);
        verify(chatTopicRepository).findByChatTopicAndTopicName(ChatTopicType.CAREER, TopicNameType.CAREER_CHANGE);
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
        assertEquals(ChatTopicType.CAREER, result.get(0).getTopic().getChatTopic());
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
}
