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
import coffeandcommit.crema.domain.reservation.entity.Candidate;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.domain.guide.enums.TimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
    private Member testGuideMember;
    private Guide testGuide;
    private ChatTopic testChatTopic;
    private MemberChatTopic testMemberChatTopic;
    private MemberJobField testMemberJobField;
    private Reservation testReservation;
    private Candidate testCandidate;

    @BeforeEach
    void setUp() {
        // 테스트용 Member
        testMember = Member.builder()
                .id("testMemberId")
                .nickname("testUser")
                .role(MemberRole.ROOKIE)
                .email("test@example.com")
                .point(1000)
                .profileImageUrl("https://example.com/profile.jpg")
                .description("테스트 사용자입니다")
                .provider("google")
                .isDeleted(false)
                .chatTopics(new ArrayList<>())
                .jobField(null)
                .build();

        // 테스트용 Guide Member
        testGuideMember = Member.builder()
                .id("guideId")
                .nickname("테스트가이드")
                .role(MemberRole.GUIDE)
                .email("guide@example.com")
                .profileImageUrl("https://example.com/guide-profile.jpg")
                .isDeleted(false)
                .build();

        // 테스트용 Guide
        testGuide = Guide.builder()
                .id(1L)
                .member(testGuideMember)
                .companyName("테스트회사")
                .jobPosition("개발자")
                .workingStart(LocalDate.of(2022, 1, 1))
                .isCurrent(true)
                .isCompanyNamePublic(true)
                .build();

        // 테스트용 ChatTopic
        testChatTopic = ChatTopic.builder()
                .id(1L)
                .topicName(TopicNameType.CAREER_CHANGE)
                .build();

        // 테스트용 MemberChatTopic
        testMemberChatTopic = MemberChatTopic.builder()
                .id(1L)
                .member(testMember)
                .chatTopic(testChatTopic)
                .build();

        // 테스트용 MemberJobField
        testMemberJobField = MemberJobField.builder()
                .id(1L)
                .member(testMember)
                .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                .build();

        // 테스트용 Candidate
        testCandidate = Candidate.builder()
                .id(1L)
                .date(LocalDateTime.of(2024, 12, 1, 14, 0))
                .priority(1)
                .build();

        // 테스트용 Reservation
        testReservation = Reservation.builder()
                .id(1L)
                .member(testMember)
                .guide(testGuide)
                .status(Status.PENDING)
                .build();
    }

    @Nested
    @DisplayName("커피챗 주제 관리")
    class ChatTopicManagementTests {

        @Test
        @DisplayName("성공: 커피챗 주제 설정")
        void registerChatTopics_Success() {
            // given
            MemberChatTopicRequest request = MemberChatTopicRequest.builder()
                    .topicNames(List.of(TopicNameType.CAREER_CHANGE, TopicNameType.COVER_LETTER))
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(chatTopicRepository.findByTopicName(TopicNameType.CAREER_CHANGE))
                    .willReturn(Optional.of(testChatTopic));
            given(chatTopicRepository.findByTopicName(TopicNameType.COVER_LETTER))
                    .willReturn(Optional.empty());
            given(chatTopicRepository.save(any(ChatTopic.class)))
                    .willReturn(ChatTopic.builder().id(2L).topicName(TopicNameType.COVER_LETTER).build());
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            List<MemberChatTopicResponse> result = memberCoffeeChatService.registerChatTopics("testMemberId", request);

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(chatTopicRepository).findByTopicName(TopicNameType.CAREER_CHANGE);
            verify(chatTopicRepository).findByTopicName(TopicNameType.COVER_LETTER);
            verify(chatTopicRepository).save(any(ChatTopic.class));
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원의 주제 설정")
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
        @DisplayName("성공: 커피챗 주제 조회")
        void getChatTopics_Success() {
            // given
            testMember = testMember.toBuilder()
                    .chatTopics(List.of(testMemberChatTopic))
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));

            // when
            List<MemberChatTopicResponse> result = memberCoffeeChatService.getChatTopics("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMemberId()).isEqualTo("testMemberId");

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
        }

        @Test
        @DisplayName("성공: 주제가 없는 회원의 조회")
        void getChatTopics_EmptyTopics() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));

            // when
            List<MemberChatTopicResponse> result = memberCoffeeChatService.getChatTopics("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
        }
    }

    @Nested
    @DisplayName("직무 분야 관리")
    class JobFieldManagementTests {

        @Test
        @DisplayName("성공: 직무 분야 설정 - 기존 분야 없음")
        void registerJobField_NewField_Success() {
            // given
            MemberJobFieldRequest request = MemberJobFieldRequest.builder()
                    .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

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
        @DisplayName("성공: 직무 분야 업데이트 - 기존 분야 있음")
        void registerJobField_UpdateField_Success() {
            // given
            testMember = testMember.toBuilder()
                    .jobField(testMemberJobField)
                    .build();

            MemberJobFieldRequest request = MemberJobFieldRequest.builder()
                    .jobName(JobNameType.MARKETING_PR)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            MemberJobFieldResponse result = memberCoffeeChatService.registerJobField("testMemberId", request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMemberId()).isEqualTo("testMemberId");
            assertThat(result.getJobName()).isEqualTo(JobNameType.MARKETING_PR);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("성공: null 분야명으로 UNDEFINED 설정")
        void registerJobField_NullJobName_SetUndefined() {
            // given
            MemberJobFieldRequest request = MemberJobFieldRequest.builder()
                    .jobName(null)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            MemberJobFieldResponse result = memberCoffeeChatService.registerJobField("testMemberId", request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getJobName()).isEqualTo(JobNameType.UNDEFINED);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("성공: 직무 분야 조회 - 분야 있음")
        void getJobField_WithField_Success() {
            // given
            testMember = testMember.toBuilder()
                    .jobField(testMemberJobField)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));

            // when
            MemberJobFieldResponse result = memberCoffeeChatService.getJobField("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMemberId()).isEqualTo("testMemberId");
            assertThat(result.getJobName()).isEqualTo(JobNameType.IT_DEVELOPMENT_DATA);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
        }

        @Test
        @DisplayName("성공: 직무 분야 조회 - 분야 없음")
        void getJobField_WithoutField_ReturnUndefined() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));

            // when
            MemberJobFieldResponse result = memberCoffeeChatService.getJobField("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMemberId()).isEqualTo("testMemberId");
            assertThat(result.getJobName()).isEqualTo(JobNameType.UNDEFINED);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원의 분야 설정")
        void registerJobField_MemberNotFound() {
            // given
            MemberJobFieldRequest request = MemberJobFieldRequest.builder()
                    .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("invalidMemberId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatService.registerJobField("invalidMemberId", request))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberRepository).findByIdAndIsDeletedFalse("invalidMemberId");
        }
    }

    @Nested
    @DisplayName("커피챗 예약 조회")
    class CoffeeChatReservationTests {

        @Test
        @DisplayName("성공: 대기중 예약 조회")
        void getPendingReservations_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMemberIdAndStatusWithFetchJoin("testMemberId", Status.PENDING))
                    .willReturn(List.of(testReservation));

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getPendingReservations("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReservationId()).isEqualTo(1L);
            assertThat(result.get(0).getStatus()).isEqualTo(Status.PENDING);
            assertThat(result.get(0).getGuide().getNickname()).isEqualTo("테스트가이드");

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(reservationRepository).findByMemberIdAndStatusWithFetchJoin("testMemberId", Status.PENDING);
        }

        @Test
        @DisplayName("성공: 확정된 예약 조회")
        void getConfirmedReservations_Success() {
            // given
            Reservation confirmedReservation = testReservation.toBuilder()
                    .status(Status.CONFIRMED)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMemberIdAndStatusWithFetchJoin("testMemberId", Status.CONFIRMED))
                    .willReturn(List.of(confirmedReservation));

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getConfirmedReservations("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(Status.CONFIRMED);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(reservationRepository).findByMemberIdAndStatusWithFetchJoin("testMemberId", Status.CONFIRMED);
        }

        @Test
        @DisplayName("성공: 완료된 예약 조회")
        void getCompletedReservations_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMemberIdAndStatusWithFetchJoin("testMemberId", Status.COMPLETED))
                    .willReturn(Collections.emptyList());

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getCompletedReservations("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(reservationRepository).findByMemberIdAndStatusWithFetchJoin("testMemberId", Status.COMPLETED);
        }

        @Test
        @DisplayName("성공: 취소된 예약 조회")
        void getCancelledReservations_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMemberIdAndStatusWithFetchJoin("testMemberId", Status.CANCELLED))
                    .willReturn(Collections.emptyList());

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getCancelledReservations("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(reservationRepository).findByMemberIdAndStatusWithFetchJoin("testMemberId", Status.CANCELLED);
        }

        @Test
        @DisplayName("성공: 전체 예약 조회")
        void getAllReservations_Success() {
            // given
            Reservation confirmedReservation = testReservation.toBuilder()
                    .id(2L)
                    .status(Status.CONFIRMED)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMemberIdWithFetchJoin("testMemberId"))
                    .willReturn(List.of(testReservation, confirmedReservation));

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getAllReservations("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getStatus()).isEqualTo(Status.PENDING);
            assertThat(result.get(1).getStatus()).isEqualTo(Status.CONFIRMED);

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(reservationRepository).findByMemberIdWithFetchJoin("testMemberId");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원의 예약 조회")
        void getReservations_MemberNotFound() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("invalidMemberId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatService.getAllReservations("invalidMemberId"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberRepository).findByIdAndIsDeletedFalse("invalidMemberId");
        }

        @Test
        @DisplayName("성공: 예약이 없는 회원 조회")
        void getAllReservations_NoReservations() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(reservationRepository.findByMemberIdWithFetchJoin("testMemberId"))
                    .willReturn(Collections.emptyList());

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getAllReservations("testMemberId");

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(reservationRepository).findByMemberIdWithFetchJoin("testMemberId");
        }
    }

    @Nested
    @DisplayName("응답 DTO 변환 테스트")
    class ResponseDtoConversionTests {

        @Test
        @DisplayName("성공: 다양한 상태의 예약 변환")
        void memberCoffeeChatResponseFrom_DifferentStatuses() {
            // given
            Reservation completedReservation = testReservation.toBuilder()
                    .status(Status.COMPLETED)
                    .build();

            // when
            MemberCoffeeChatResponse result = MemberCoffeeChatResponse.from(completedReservation);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
        }

        @Test
        @DisplayName("성공: null 값 처리 검증")
        void memberCoffeeChatResponseFrom_NullValues() {
            // given
            Reservation reservationWithNulls = testReservation.toBuilder()
                    .build();

            // when
            MemberCoffeeChatResponse result = MemberCoffeeChatResponse.from(reservationWithNulls);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getReservationId()).isEqualTo(1L);
            // null 값들이 적절히 처리되는지 확인 (createdAt은 BaseEntity에서 관리)
        }
    }

    @Nested
    @DisplayName("Helper 메서드 테스트")
    class HelperMethodTests {

        @Test
        @DisplayName("성공: findActiveMemberById - 활성 회원 조회")
        void findActiveMemberById_Success() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));

            // when
            List<MemberCoffeeChatResponse> result = memberCoffeeChatService.getAllReservations("testMemberId");

            // then - 예외가 발생하지 않고 정상 처리됨을 확인
            assertThat(result).isNotNull();

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
        }

        @Test
        @DisplayName("실패: findActiveMemberById - 삭제된 회원")
        void findActiveMemberById_DeletedMember() {
            // given
            given(memberRepository.findByIdAndIsDeletedFalse("deletedMemberId"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatService.getAllReservations("deletedMemberId"))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberRepository).findByIdAndIsDeletedFalse("deletedMemberId");
        }

        @Test
        @DisplayName("성공: findOrCreateChatTopic - 기존 주제 조회")
        void findOrCreateChatTopic_ExistingTopic() {
            // given
            MemberChatTopicRequest request = MemberChatTopicRequest.builder()
                    .topicNames(List.of(TopicNameType.CAREER_CHANGE))
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(chatTopicRepository.findByTopicName(TopicNameType.CAREER_CHANGE))
                    .willReturn(Optional.of(testChatTopic));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            memberCoffeeChatService.registerChatTopics("testMemberId", request);

            // then
            verify(chatTopicRepository).findByTopicName(TopicNameType.CAREER_CHANGE);
            verify(chatTopicRepository, never()).save(any(ChatTopic.class));
        }

        @Test
        @DisplayName("성공: findOrCreateChatTopic - 새 주제 생성")
        void findOrCreateChatTopic_CreateNewTopic() {
            // given
            MemberChatTopicRequest request = MemberChatTopicRequest.builder()
                    .topicNames(List.of(TopicNameType.PORTFOLIO))
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(chatTopicRepository.findByTopicName(TopicNameType.PORTFOLIO))
                    .willReturn(Optional.empty());
            given(chatTopicRepository.save(any(ChatTopic.class)))
                    .willReturn(ChatTopic.builder().id(3L).topicName(TopicNameType.PORTFOLIO).build());
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            memberCoffeeChatService.registerChatTopics("testMemberId", request);

            // then
            verify(chatTopicRepository).findByTopicName(TopicNameType.PORTFOLIO);
            verify(chatTopicRepository).save(any(ChatTopic.class));
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 상황")
    class EdgeCaseTests {

        @Test
        @DisplayName("경계값: 최대 주제 개수 설정")
        void registerChatTopics_MaxTopics() {
            // given - 모든 주제 타입 설정
            List<TopicNameType> allTopics = List.of(TopicNameType.values());
            MemberChatTopicRequest request = MemberChatTopicRequest.builder()
                    .topicNames(allTopics)
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));

            // 모든 주제에 대해 기존 주제 조회 설정
            for (TopicNameType topic : allTopics) {
                given(chatTopicRepository.findByTopicName(topic))
                        .willReturn(Optional.of(ChatTopic.builder().topicName(topic).build()));
            }

            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            List<MemberChatTopicResponse> result = memberCoffeeChatService.registerChatTopics("testMemberId", request);

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(allTopics.size());

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("경계값: 중복 주제 설정")
        void registerChatTopics_DuplicateTopics() {
            // given
            MemberChatTopicRequest request = MemberChatTopicRequest.builder()
                    .topicNames(List.of(TopicNameType.CAREER_CHANGE, TopicNameType.CAREER_CHANGE))
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(chatTopicRepository.findByTopicName(TopicNameType.CAREER_CHANGE))
                    .willReturn(Optional.of(testChatTopic));
            given(memberRepository.save(any(Member.class)))
                    .willReturn(testMember);

            // when
            List<MemberChatTopicResponse> result = memberCoffeeChatService.registerChatTopics("testMemberId", request);

            // then
            assertThat(result).isNotNull();
            // 중복 제거되어 1개만 반환되어야 함
            assertThat(result).hasSize(2);
            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
        }

        @Test
        @DisplayName("성공: 트랜잭션 롤백 시나리오")
        void registerChatTopics_TransactionRollback() {
            // given
            MemberChatTopicRequest request = MemberChatTopicRequest.builder()
                    .topicNames(List.of(TopicNameType.CAREER_CHANGE))
                    .build();

            given(memberRepository.findByIdAndIsDeletedFalse("testMemberId"))
                    .willReturn(Optional.of(testMember));
            given(chatTopicRepository.findByTopicName(TopicNameType.CAREER_CHANGE))
                    .willReturn(Optional.of(testChatTopic));
            given(memberRepository.save(any(Member.class)))
                    .willThrow(new RuntimeException("Database error"));

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatService.registerChatTopics("testMemberId", request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");

            verify(memberRepository).findByIdAndIsDeletedFalse("testMemberId");
            verify(memberRepository).save(any(Member.class));
        }
    }
}