package coffeandcommit.crema.domain.member.service;

import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.globalTag.repository.ChatTopicRepository;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.member.dto.request.MemberChatTopicRequest;
import coffeandcommit.crema.domain.member.dto.request.MemberJobFieldRequest;
import coffeandcommit.crema.domain.member.dto.response.MemberChatTopicResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberJobFieldResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberCoffeeChatResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.entity.MemberChatTopic;
import coffeandcommit.crema.domain.member.entity.MemberJobField;
import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberCoffeeChatService 완전한 단위 테스트")
class MemberCoffeeChatServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatTopicRepository chatTopicRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private MemberCoffeeChatService memberCoffeeChatService;

    private Member testMember;
    private ChatTopic testChatTopic1;
    private ChatTopic testChatTopic2;
    private MemberChatTopic testMemberChatTopic1;
    private MemberChatTopic testMemberChatTopic2;
    private MemberJobField testMemberJobField;
    private Guide testGuide;
    private Reservation testReservation1;
    private Reservation testReservation2;

    @BeforeEach
    void setUp() {
        // Member 설정
        testMember = Member.builder()
                .id("testMemberId")
                .nickname("테스트멤버")
                .role(MemberRole.ROOKIE)
                .email("test@example.com")
                .point(1000)
                .profileImageUrl("https://example.com/profile.jpg")
                .description("테스트 멤버입니다")
                .provider("google")
                .providerId("google_123")
                .isDeleted(false)
                .build();

        // ChatTopic 설정
        testChatTopic1 = ChatTopic.builder()
                .id(1L)
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        testChatTopic2 = ChatTopic.builder()
                .id(2L)
                .topicName(TopicNameType.COVER_LETTER)
                .build();

        // MemberChatTopic 설정
        testMemberChatTopic1 = MemberChatTopic.builder()
                .id(1L)
                .member(testMember)
                .chatTopic(testChatTopic1)
                .build();

        testMemberChatTopic2 = MemberChatTopic.builder()
                .id(2L)
                .member(testMember)
                .chatTopic(testChatTopic2)
                .build();

        // MemberJobField 설정
        testMemberJobField = MemberJobField.builder()
                .id(1L)
                .member(testMember)
                .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                .build();

        // Guide 설정
        testGuide = Guide.builder()
                .id(1L)
                .member(testMember)
                .chatDescription("테스트 가이드")
                .isOpened(true)
                .title("테스트 가이드 제목")
                .approvedDate(LocalDateTime.now())
                .workingStart(LocalDateTime.now().minusYears(2).toLocalDate())
                .jobPosition("백엔드 개발자")
                .companyName("테스트회사")
                .isCompanyNamePublic(true)
                .isCurrent(true)
                .build();

        // Reservation 설정
        testReservation1 = Reservation.builder()
                .id(1L)
                .guide(testGuide)
                .member(testMember)
                .matchingTime(LocalDateTime.now().plusDays(1))
                .status(Status.PENDING)
                .reservedAt(LocalDateTime.now())
                .build();

        testReservation2 = Reservation.builder()
                .id(2L)
                .guide(testGuide)
                .member(testMember)
                .matchingTime(LocalDateTime.now().plusDays(2))
                .status(Status.CONFIRMED)
                .reservedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("관심 주제 관리")
    class ChatTopicTests {

        @Test
        @DisplayName("성공: 관심 커피챗 주제 등록")
        void registerChatTopics_Success() {
            // given
            MemberChatTopicRequest request = MemberChatTopicRequest.builder()
                    .topicNames(List.of(TopicNameType.CAREER_CHANGE, TopicNameType.COVER_LETTER))
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(chatTopicRepository.findByTopicName(TopicNameType.CAREER_CHANGE))
                    .willReturn(Optional.of(testChatTopic1));
            given(chatTopicRepository.findByTopicName(TopicNameType.COVER_LETTER))
                    .willReturn(Optional.of(testChatTopic2));

            Member memberWithTopics = testMember.toBuilder()
                    .chatTopics(List.of(testMemberChatTopic1, testMemberChatTopic2))
                    .build();
            given(memberRepository.save(any(Member.class)))
                    .willReturn(memberWithTopics);

            // when
            List<MemberChatTopicResponse> result = memberCoffeeChatService.registerChatTopics("testMemberId", request);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTopic().getTopicName()).isEqualTo(TopicNameType.CAREER_CHANGE);
            assertThat(result.get(1).getTopic().getTopicName()).isEqualTo(TopicNameType.COVER_LETTER);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("성공: 존재하지 않는 주제는 새로 생성")
        void registerChatTopics_CreateNewTopic() {
            // given
            MemberChatTopicRequest request = MemberChatTopicRequest.builder()
                    .topicNames(List.of(TopicNameType.CAREER_CHANGE))
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(chatTopicRepository.findByTopicName(TopicNameType.CAREER_CHANGE))
                    .willReturn(Optional.empty());
            given(chatTopicRepository.save(any(ChatTopic.class)))
                    .willReturn(testChatTopic1);

            Member memberWithTopics = testMember.toBuilder()
                    .chatTopics(List.of(testMemberChatTopic1))
                    .build();
            given(memberRepository.save(any(Member.class)))
                    .willReturn(memberWithTopics);

            // when
            List<MemberChatTopicResponse> result = memberCoffeeChatService.registerChatTopics("testMemberId", request);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTopic().getTopicName()).isEqualTo(TopicNameType.CAREER_CHANGE);

            verify(chatTopicRepository).save(any(ChatTopic.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버 ID로 주제 등록 시도")
        void registerChatTopics_MemberNotFound() {
            // given
            MemberChatTopicRequest request = MemberChatTopicRequest.builder()
                    .topicNames(List.of(TopicNameType.CAREER_CHANGE))
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("invalidMemberId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatService.registerChatTopics("invalidMemberId", request))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberRepository).findByIdAndIsDeletedFalse("invalidMemberId");
        }

        @Test
        @DisplayName("성공: 관심 커피챗 주제 조회")
        void getChatTopics_Success() {
            // given
            Member memberWithTopics = testMember.toBuilder()
                    .chatTopics(List.of(testMemberChatTopic1, testMemberChatTopic2))
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(memberWithTopics));

            // when
            List<MemberChatTopicResponse> result = memberCoffeeChatService.getChatTopics("testMemberId");

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMemberId()).isEqualTo("testMemberId");
            assertThat(result.get(0).getTopic().getTopicName()).isEqualTo(TopicNameType.CAREER_CHANGE);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
        }

        @Test
        @DisplayName("성공: 주제가 없는 멤버의 빈 주제 목록 조회")
        void getChatTopics_Empty() {
            // given
            Member memberWithoutTopics = testMember.toBuilder()
                    .chatTopics(Collections.emptyList())
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(memberWithoutTopics));

            // when
            List<MemberChatTopicResponse> result = memberCoffeeChatService.getChatTopics("testMemberId");

            // then
            assertThat(result).isEmpty();

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
        }
    }

    @Nested
    @DisplayName("관심 분야 관리")
    class JobFieldTests {

        @Test
        @DisplayName("성공: 관심 커피챗 분야 등록 (신규)")
        void registerJobField_Success_NewField() {
            // given
            MemberJobFieldRequest request = MemberJobFieldRequest.builder()
                    .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                    .build();

            Member memberWithoutJobField = testMember.toBuilder()
                    .jobField(null)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(memberWithoutJobField));

            Member memberWithJobField = memberWithoutJobField.toBuilder()
                    .jobField(testMemberJobField)
                    .build();
            given(memberRepository.save(any(Member.class)))
                    .willReturn(memberWithJobField);

            // when
            MemberJobFieldResponse result = memberCoffeeChatService.registerJobField("testMemberId", request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMemberId()).isEqualTo("testMemberId");
            assertThat(result.getJobName()).isEqualTo(JobNameType.IT_DEVELOPMENT_DATA);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("성공: 관심 커피챗 분야 등록 (기존 분야 업데이트)")
        void registerJobField_Success_UpdateExisting() {
            // given
            MemberJobFieldRequest request = MemberJobFieldRequest.builder()
                    .jobName(JobNameType.DESIGN)
                    .build();

            Member memberWithExistingJobField = testMember.toBuilder()
                    .jobField(testMemberJobField)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(memberWithExistingJobField));

            MemberJobField updatedJobField = testMemberJobField.toBuilder()
                    .jobName(JobNameType.DESIGN)
                    .build();
            Member memberWithUpdatedJobField = memberWithExistingJobField.toBuilder()
                    .jobField(updatedJobField)
                    .build();
            given(memberRepository.save(any(Member.class)))
                    .willReturn(memberWithUpdatedJobField);

            // when
            MemberJobFieldResponse result = memberCoffeeChatService.registerJobField("testMemberId", request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getJobName()).isEqualTo(JobNameType.DESIGN);

            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("성공: null 요청시 UNDEFINED로 설정")
        void registerJobField_Success_NullRequest() {
            // given
            MemberJobFieldRequest request = MemberJobFieldRequest.builder()
                    .jobName(null)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));

            MemberJobField undefinedJobField = testMemberJobField.toBuilder()
                    .jobName(JobNameType.UNDEFINED)
                    .build();
            Member memberWithUndefinedJobField = testMember.toBuilder()
                    .jobField(undefinedJobField)
                    .build();
            given(memberRepository.save(any(Member.class)))
                    .willReturn(memberWithUndefinedJobField);

            // when
            MemberJobFieldResponse result = memberCoffeeChatService.registerJobField("testMemberId", request);

            // then
            assertThat(result.getJobName()).isEqualTo(JobNameType.UNDEFINED);
        }

        @Test
        @DisplayName("성공: 관심 커피챗 분야 조회")
        void getJobField_Success() {
            // given
            Member memberWithJobField = testMember.toBuilder()
                    .jobField(testMemberJobField)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(memberWithJobField));

            // when
            MemberJobFieldResponse result = memberCoffeeChatService.getJobField("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMemberId()).isEqualTo("testMemberId");
            assertThat(result.getJobName()).isEqualTo(JobNameType.IT_DEVELOPMENT_DATA);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
        }

        @Test
        @DisplayName("성공: 분야가 설정되지 않은 멤버의 UNDEFINED 분야 조회")
        void getJobField_Success_NoJobField() {
            // given
            Member memberWithoutJobField = testMember.toBuilder()
                    .jobField(null)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(memberWithoutJobField));

            // when
            MemberJobFieldResponse result = memberCoffeeChatService.getJobField("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getJobName()).isEqualTo(JobNameType.UNDEFINED);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버 ID로 분야 조회")
        void getJobField_MemberNotFound() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("invalidMemberId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatService.getJobField("invalidMemberId"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("커피챗 예약 조회")
    class ReservationTests {

        @Test
        @DisplayName("성공: 대기중 커피챗 예약 조회")
        void getPendingReservations_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMember_IdAndStatus("testMemberId", Status.PENDING))
                    .willReturn(List.of(testReservation1));

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getPendingReservations("testMemberId");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(Status.PENDING);
            assertThat(result.get(0).getReservationId()).isEqualTo(1L);

            verify(reservationRepository).findByMember_IdAndStatus("testMemberId", Status.PENDING);
        }

        @Test
        @DisplayName("성공: 확정된 커피챗 예약 조회")
        void getConfirmedReservations_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMember_IdAndStatus("testMemberId", Status.CONFIRMED))
                    .willReturn(List.of(testReservation2));

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getConfirmedReservations("testMemberId");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(result.get(0).getReservationId()).isEqualTo(2L);

            verify(reservationRepository).findByMember_IdAndStatus("testMemberId", Status.CONFIRMED);
        }

        @Test
        @DisplayName("성공: 완료된 커피챗 예약 조회")
        void getCompletedReservations_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMember_IdAndStatus("testMemberId", Status.COMPLETED))
                    .willReturn(Collections.emptyList());

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getCompletedReservations("testMemberId");

            // then
            assertThat(result).isEmpty();

            verify(reservationRepository).findByMember_IdAndStatus("testMemberId", Status.COMPLETED);
        }

        @Test
        @DisplayName("성공: 취소된 커피챗 예약 조회")
        void getCancelledReservations_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMember_IdAndStatus("testMemberId", Status.CANCELLED))
                    .willReturn(Collections.emptyList());

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getCancelledReservations("testMemberId");

            // then
            assertThat(result).isEmpty();

            verify(reservationRepository).findByMember_IdAndStatus("testMemberId", Status.CANCELLED);
        }

        @Test
        @DisplayName("성공: 전체 커피챗 예약 조회")
        void getAllReservations_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMember_Id("testMemberId"))
                    .willReturn(List.of(testReservation1, testReservation2));

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getAllReservations("testMemberId");

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getReservationId()).isEqualTo(1L);
            assertThat(result.get(1).getReservationId()).isEqualTo(2L);

            verify(reservationRepository).findByMember_Id("testMemberId");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버 ID로 예약 조회")
        void getPendingReservations_MemberNotFound() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("invalidMemberId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatService.getPendingReservations("invalidMemberId"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberRepository).findByIdAndIsDeletedFalse("invalidMemberId");
        }

        @Test
        @DisplayName("성공: 빈 예약 목록 조회")
        void getAllReservations_Empty() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMember_Id("testMemberId"))
                    .willReturn(Collections.emptyList());

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getAllReservations("testMemberId");

            // then
            assertThat(result).isEmpty();

            verify(reservationRepository).findByMember_Id("testMemberId");
        }
    }
}