package coffeandcommit.crema.domain.member.controller;

import coffeandcommit.crema.domain.globalTag.dto.TopicDTO;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.member.dto.request.MemberChatTopicRequest;
import coffeandcommit.crema.domain.member.dto.request.MemberJobFieldRequest;
import coffeandcommit.crema.domain.member.dto.response.MemberChatTopicResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberJobFieldResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberCoffeeChatResponse;
import coffeandcommit.crema.domain.member.service.MemberCoffeeChatService;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberCoffeeChatController 완전한 단위 테스트")
class MemberCoffeeChatControllerTest {

    @Mock
    private MemberCoffeeChatService memberCoffeeChatService;

    @InjectMocks
    private MemberCoffeeChatController memberCoffeeChatController;

    private UserDetails testUserDetails;
    private MemberChatTopicRequest testChatTopicRequest;
    private List<MemberChatTopicResponse> testChatTopicResponses;
    private MemberJobFieldRequest testJobFieldRequest;
    private MemberJobFieldResponse testJobFieldResponse;
    private List<MemberCoffeeChatResponse> testCoffeeChatResponses;

    @BeforeEach
    void setUp() {
        // UserDetails 설정
        testUserDetails = new User("testMemberId", "password", Collections.emptyList());

        // MemberChatTopicRequest 설정
        testChatTopicRequest = MemberChatTopicRequest.builder()
                .topicNames(List.of(
                        TopicNameType.CAREER_CHANGE,
                        TopicNameType.COVER_LETTER
                ))
                .build();

        // MemberChatTopicResponse 설정
        testChatTopicResponses = List.of(
                MemberChatTopicResponse.builder()
                        .id(1L)
                        .memberId("testMemberId")
                        .topic(TopicDTO.builder()
                                .topicName(TopicNameType.CAREER_CHANGE)
                                .description("커리어 전환")
                                .build())
                        .build(),
                MemberChatTopicResponse.builder()
                        .id(2L)
                        .memberId("testMemberId")
                        .topic(TopicDTO.builder()
                                .topicName(TopicNameType.COVER_LETTER)
                                .description("자기소개서")
                                .build())
                        .build()
        );

        // MemberJobFieldRequest 설정
        testJobFieldRequest = MemberJobFieldRequest.builder()
                .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                .build();

        // MemberJobFieldResponse 설정
        testJobFieldResponse = MemberJobFieldResponse.builder()
                .id(1L)
                .memberId("testMemberId")
                .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                .build();

        // MemberCoffeeChatResponse 설정
        testCoffeeChatResponses = List.of(
                MemberCoffeeChatResponse.builder()
                        .reservationId(1L)
                        .guideId(1L)
                        .guideNickname("테스트가이드")
                        .guideCompanyName("테스트회사")
                        .guideJobPosition("백엔드 개발자")
                        .status(Status.PENDING)
                        .matchingTime(LocalDateTime.now().plusDays(1))
                        .reservedAt(LocalDateTime.now())
                        .timeType("MINUTE_30")
                        .price(8000)
                        .createdAt(LocalDateTime.now())
                        .build(),
                MemberCoffeeChatResponse.builder()
                        .reservationId(2L)
                        .guideId(2L)
                        .guideNickname("테스트가이드2")
                        .guideCompanyName("테스트회사2")
                        .guideJobPosition("프론트엔드 개발자")
                        .status(Status.CONFIRMED)
                        .matchingTime(LocalDateTime.now().plusDays(2))
                        .reservedAt(LocalDateTime.now())
                        .timeType("MINUTE_60")
                        .price(12000)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    @Nested
    @DisplayName("관심 주제 관리")
    class ChatTopicTests {

        @Test
        @DisplayName("성공: 관심 커피챗 주제 설정")
        void registerChatTopics_Success() {
            // given
            given(memberCoffeeChatService.registerChatTopics("testMemberId", testChatTopicRequest))
                    .willReturn(testChatTopicResponses);

            // when
            ApiResponse<List<MemberChatTopicResponse>> response =
                    memberCoffeeChatController.registerChatTopics(testChatTopicRequest, testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).hasSize(2);
            assertThat(response.getResult().get(0).getTopic().getTopicName()).isEqualTo(TopicNameType.CAREER_CHANGE);
            assertThat(response.getResult().get(1).getTopic().getTopicName()).isEqualTo(TopicNameType.COVER_LETTER);

            verify(memberCoffeeChatService).registerChatTopics("testMemberId", testChatTopicRequest);
        }

        @Test
        @DisplayName("실패: 잘못된 주제 요청으로 주제 설정 실패")
        void registerChatTopics_InvalidTopic() {
            // given
            given(memberCoffeeChatService.registerChatTopics("testMemberId", testChatTopicRequest))
                    .willThrow(new BaseException(ErrorStatus.INVALID_TOPIC));

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatController.registerChatTopics(testChatTopicRequest, testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.INVALID_TOPIC);

            verify(memberCoffeeChatService).registerChatTopics("testMemberId", testChatTopicRequest);
        }

        @Test
        @DisplayName("성공: 관심 커피챗 주제 조회")
        void getChatTopics_Success() {
            // given
            given(memberCoffeeChatService.getChatTopics("testMemberId"))
                    .willReturn(testChatTopicResponses);

            // when
            ApiResponse<List<MemberChatTopicResponse>> response =
                    memberCoffeeChatController.getChatTopics(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).hasSize(2);
            assertThat(response.getResult().get(0).getMemberId()).isEqualTo("testMemberId");

            verify(memberCoffeeChatService).getChatTopics("testMemberId");
        }

        @Test
        @DisplayName("성공: 빈 주제 목록 조회")
        void getChatTopics_Empty() {
            // given
            given(memberCoffeeChatService.getChatTopics("testMemberId"))
                    .willReturn(Collections.emptyList());

            // when
            ApiResponse<List<MemberChatTopicResponse>> response =
                    memberCoffeeChatController.getChatTopics(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isEmpty();

            verify(memberCoffeeChatService).getChatTopics("testMemberId");
        }
    }

    @Nested
    @DisplayName("관심 분야 관리")
    class JobFieldTests {

        @Test
        @DisplayName("성공: 관심 커피챗 분야 설정")
        void registerJobField_Success() {
            // given
            given(memberCoffeeChatService.registerJobField("testMemberId", testJobFieldRequest))
                    .willReturn(testJobFieldResponse);

            // when
            ApiResponse<MemberJobFieldResponse> response =
                    memberCoffeeChatController.registerJobField(testJobFieldRequest, testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult().getJobName()).isEqualTo(JobNameType.IT_DEVELOPMENT_DATA);
            assertThat(response.getResult().getMemberId()).isEqualTo("testMemberId");

            verify(memberCoffeeChatService).registerJobField("testMemberId", testJobFieldRequest);
        }

        @Test
        @DisplayName("실패: 잘못된 직무 분야 요청으로 분야 설정 실패")
        void registerJobField_InvalidJobField() {
            // given
            given(memberCoffeeChatService.registerJobField("testMemberId", testJobFieldRequest))
                    .willThrow(new BaseException(ErrorStatus.INVALID_JOB_FIELD));

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatController.registerJobField(testJobFieldRequest, testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.INVALID_JOB_FIELD);

            verify(memberCoffeeChatService).registerJobField("testMemberId", testJobFieldRequest);
        }

        @Test
        @DisplayName("성공: 관심 커피챗 분야 조회")
        void getJobField_Success() {
            // given
            given(memberCoffeeChatService.getJobField("testMemberId"))
                    .willReturn(testJobFieldResponse);

            // when
            ApiResponse<MemberJobFieldResponse> response =
                    memberCoffeeChatController.getJobField(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult().getJobName()).isEqualTo(JobNameType.IT_DEVELOPMENT_DATA);

            verify(memberCoffeeChatService).getJobField("testMemberId");
        }

        @Test
        @DisplayName("실패: 회원을 찾을 수 없어 분야 조회 실패")
        void getJobField_MemberNotFound() {
            // given
            given(memberCoffeeChatService.getJobField("testMemberId"))
                    .willThrow(new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatController.getJobField(testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberCoffeeChatService).getJobField("testMemberId");
        }
    }

    @Nested
    @DisplayName("커피챗 예약 조회")
    class ReservationTests {

        @Test
        @DisplayName("성공: 대기중 커피챗 조회")
        void getPendingReservations_Success() {
            // given
            List<MemberCoffeeChatResponse> pendingReservations = testCoffeeChatResponses.stream()
                    .filter(r -> r.getStatus() == Status.PENDING)
                    .toList();
            given(memberCoffeeChatService.getPendingReservations("testMemberId"))
                    .willReturn(pendingReservations);

            // when
            ApiResponse<List<MemberCoffeeChatResponse>> response =
                    memberCoffeeChatController.getPendingReservations(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).hasSize(1);
            assertThat(response.getResult().get(0).getStatus()).isEqualTo(Status.PENDING);

            verify(memberCoffeeChatService).getPendingReservations("testMemberId");
        }

        @Test
        @DisplayName("성공: 확정된 커피챗 조회")
        void getConfirmedReservations_Success() {
            // given
            List<MemberCoffeeChatResponse> confirmedReservations = testCoffeeChatResponses.stream()
                    .filter(r -> r.getStatus() == Status.CONFIRMED)
                    .toList();
            given(memberCoffeeChatService.getConfirmedReservations("testMemberId"))
                    .willReturn(confirmedReservations);

            // when
            ApiResponse<List<MemberCoffeeChatResponse>> response =
                    memberCoffeeChatController.getConfirmedReservations(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).hasSize(1);
            assertThat(response.getResult().get(0).getStatus()).isEqualTo(Status.CONFIRMED);

            verify(memberCoffeeChatService).getConfirmedReservations("testMemberId");
        }

        @Test
        @DisplayName("성공: 완료된 커피챗 조회")
        void getCompletedReservations_Success() {
            // given
            given(memberCoffeeChatService.getCompletedReservations("testMemberId"))
                    .willReturn(Collections.emptyList());

            // when
            ApiResponse<List<MemberCoffeeChatResponse>> response =
                    memberCoffeeChatController.getCompletedReservations(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isEmpty();

            verify(memberCoffeeChatService).getCompletedReservations("testMemberId");
        }

        @Test
        @DisplayName("성공: 취소된 커피챗 조회")
        void getCancelledReservations_Success() {
            // given
            given(memberCoffeeChatService.getCancelledReservations("testMemberId"))
                    .willReturn(Collections.emptyList());

            // when
            ApiResponse<List<MemberCoffeeChatResponse>> response =
                    memberCoffeeChatController.getCancelledReservations(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isEmpty();

            verify(memberCoffeeChatService).getCancelledReservations("testMemberId");
        }

        @Test
        @DisplayName("성공: 전체 커피챗 조회")
        void getAllReservations_Success() {
            // given
            given(memberCoffeeChatService.getAllReservations("testMemberId"))
                    .willReturn(testCoffeeChatResponses);

            // when
            ApiResponse<List<MemberCoffeeChatResponse>> response =
                    memberCoffeeChatController.getAllReservations(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).hasSize(2);
            assertThat(response.getResult().get(0).getReservationId()).isEqualTo(1L);
            assertThat(response.getResult().get(1).getReservationId()).isEqualTo(2L);

            verify(memberCoffeeChatService).getAllReservations("testMemberId");
        }

        @Test
        @DisplayName("실패: 회원을 찾을 수 없어 예약 조회 실패")
        void getPendingReservations_MemberNotFound() {
            // given
            given(memberCoffeeChatService.getPendingReservations("testMemberId"))
                    .willThrow(new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatController.getPendingReservations(testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberCoffeeChatService).getPendingReservations("testMemberId");
        }

        @Test
        @DisplayName("성공: 빈 예약 목록 조회")
        void getAllReservations_Empty() {
            // given
            given(memberCoffeeChatService.getAllReservations("testMemberId"))
                    .willReturn(Collections.emptyList());

            // when
            ApiResponse<List<MemberCoffeeChatResponse>> response =
                    memberCoffeeChatController.getAllReservations(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isEmpty();

            verify(memberCoffeeChatService).getAllReservations("testMemberId");
        }
    }
}