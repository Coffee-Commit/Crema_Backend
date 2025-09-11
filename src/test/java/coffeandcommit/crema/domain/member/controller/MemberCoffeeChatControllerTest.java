package coffeandcommit.crema.domain.member.controller;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.member.dto.request.MemberChatTopicRequest;
import coffeandcommit.crema.domain.member.dto.request.MemberJobFieldRequest;
import coffeandcommit.crema.domain.member.dto.response.MemberChatTopicResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberJobFieldResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberCoffeeChatResponse;
import coffeandcommit.crema.domain.member.service.MemberCoffeeChatService;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.review.dto.response.GuideInfo;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberCoffeeChatController 완전한 단위 테스트")
class MemberCoffeeChatControllerTest {

    @Mock
    private MemberCoffeeChatService memberCoffeeChatService;

    @InjectMocks
    private MemberCoffeeChatController memberCoffeeChatController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserDetails testUserDetails;
    private MemberChatTopicRequest testChatTopicRequest;
    private List<MemberChatTopicResponse> testChatTopicResponses;
    private MemberJobFieldRequest testJobFieldRequest;
    private MemberJobFieldResponse testJobFieldResponse;
    private List<MemberCoffeeChatResponse> testCoffeeChatResponses;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberCoffeeChatController).build();
        objectMapper = new ObjectMapper();

        // 테스트 사용자 설정
        testUserDetails = new User("testMemberId", "password", Collections.emptyList());

        // 커피챗 주제 테스트 데이터
        testChatTopicRequest = MemberChatTopicRequest.builder()
                .topicNames(List.of(TopicNameType.CAREER_CHANGE, TopicNameType.COVER_LETTER))
                .build();

        testChatTopicResponses = List.of(
                MemberChatTopicResponse.builder()
                        .memberId("testMemberId")
                        .topic(null) // TopicDTO는 실제 구현에 따라 수정
                        .build()
        );

        // 직무 분야 테스트 데이터
        testJobFieldRequest = MemberJobFieldRequest.builder()
                .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                .build();

        testJobFieldResponse = MemberJobFieldResponse.builder()
                .memberId("testMemberId")
                .jobName(JobNameType.IT_DEVELOPMENT_DATA)
                .build();

        // 커피챗 예약 테스트 데이터 (새로운 DTO 구조)
        testCoffeeChatResponses = List.of(
                MemberCoffeeChatResponse.builder()
                        .reservationId(1L)
                        .guide(GuideInfo.builder()
                                .nickname("테스트가이드1")
                                .profileImageUrl("https://example.com/profile1.jpg")
                                .build())
                        .createdAt("2024-11-25T10:30:00")
                        .preferredDateOnly("2024-12-01")
                        .preferredDayOfWeek("월")
                        .preferredTimeRange("14:00~14:30")
                        .status(Status.PENDING)
                        .timeType("MINUTE_30")
                        .build(),
                MemberCoffeeChatResponse.builder()
                        .reservationId(2L)
                        .guide(GuideInfo.builder()
                                .nickname("테스트가이드2")
                                .profileImageUrl("https://example.com/profile2.jpg")
                                .build())
                        .createdAt("2024-11-20T16:45:00")
                        .preferredDateOnly("2024-12-05")
                        .preferredDayOfWeek("목")
                        .preferredTimeRange("19:00~20:00")
                        .status(Status.CONFIRMED)
                        .videoSessionId("session_reservation_2_2024-12-05T19:00:00")
                        .timeType("HOUR_1")
                        .build()
        );
    }

    @Nested
    @DisplayName("커피챗 관심사 관리")
    class ChatTopicManagementTests {

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
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult()).hasSize(1);
            assertThat(response.getResult().get(0).getMemberId()).isEqualTo("testMemberId");

            verify(memberCoffeeChatService).registerChatTopics("testMemberId", testChatTopicRequest);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원의 주제 설정")
        void registerChatTopics_MemberNotFound() {
            // given
            given(memberCoffeeChatService.registerChatTopics("testMemberId", testChatTopicRequest))
                    .willThrow(new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatController.registerChatTopics(testChatTopicRequest, testUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

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
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult()).hasSize(1);

            verify(memberCoffeeChatService).getChatTopics("testMemberId");
        }

        @Test
        @DisplayName("성공: 빈 주제 목록 조회")
        void getChatTopics_EmptyList() {
            // given
            given(memberCoffeeChatService.getChatTopics("testMemberId"))
                    .willReturn(Collections.emptyList());

            // when
            ApiResponse<List<MemberChatTopicResponse>> response =
                    memberCoffeeChatController.getChatTopics(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult()).isEmpty();

            verify(memberCoffeeChatService).getChatTopics("testMemberId");
        }
    }

    @Nested
    @DisplayName("관심 분야 관리")
    class JobFieldManagementTests {

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
            assertThat(response.getResult().getMemberId()).isEqualTo("testMemberId");
            assertThat(response.getResult().getJobName()).isEqualTo(JobNameType.IT_DEVELOPMENT_DATA);

            verify(memberCoffeeChatService).registerJobField("testMemberId", testJobFieldRequest);
        }

        @Test
        @DisplayName("성공: UNDEFINED 분야 설정")
        void registerJobField_UndefinedField() {
            // given
            MemberJobFieldRequest undefinedRequest = MemberJobFieldRequest.builder()
                    .jobName(JobNameType.UNDEFINED)
                    .build();

            MemberJobFieldResponse undefinedResponse = MemberJobFieldResponse.builder()
                    .memberId("testMemberId")
                    .jobName(JobNameType.UNDEFINED)
                    .build();

            given(memberCoffeeChatService.registerJobField("testMemberId", undefinedRequest))
                    .willReturn(undefinedResponse);

            // when
            ApiResponse<MemberJobFieldResponse> response =
                    memberCoffeeChatController.registerJobField(undefinedRequest, testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult().getJobName()).isEqualTo(JobNameType.UNDEFINED);

            verify(memberCoffeeChatService).registerJobField("testMemberId", undefinedRequest);
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
        @DisplayName("실패: 존재하지 않는 회원의 분야 조회")
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
    class CoffeeChatReservationTests {

        @Test
        @DisplayName("성공: 대기중 커피챗 조회")
        void getPendingReservations_Success() {
            // given
            List<MemberCoffeeChatResponse> pendingReservations = List.of(testCoffeeChatResponses.get(0));
            given(memberCoffeeChatService.getPendingReservations("testMemberId"))
                    .willReturn(pendingReservations);

            // when
            ApiResponse<List<MemberCoffeeChatResponse>> response =
                    memberCoffeeChatController.getPendingReservations(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult()).hasSize(1);
            assertThat(response.getResult().get(0).getStatus()).isEqualTo(Status.PENDING);
            assertThat(response.getResult().get(0).getGuide().getNickname()).isEqualTo("테스트가이드1");

            verify(memberCoffeeChatService).getPendingReservations("testMemberId");
        }

        @Test
        @DisplayName("성공: 확정된 커피챗 조회")
        void getConfirmedReservations_Success() {
            // given
            List<MemberCoffeeChatResponse> confirmedReservations = List.of(testCoffeeChatResponses.get(1));
            given(memberCoffeeChatService.getConfirmedReservations("testMemberId"))
                    .willReturn(confirmedReservations);

            // when
            ApiResponse<List<MemberCoffeeChatResponse>> response =
                    memberCoffeeChatController.getConfirmedReservations(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult()).hasSize(1);
            assertThat(response.getResult().get(0).getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(response.getResult().get(0).getVideoSessionId()).isEqualTo("session_reservation_2_2024-12-05T19:00:00");

            verify(memberCoffeeChatService).getConfirmedReservations("testMemberId");
        }

        @Test
        @DisplayName("성공: 완료된 커피챗 조회")
        void getCompletedReservations_Success() {
            // given
            List<MemberCoffeeChatResponse> completedReservations = Collections.emptyList();
            given(memberCoffeeChatService.getCompletedReservations("testMemberId"))
                    .willReturn(completedReservations);

            // when
            ApiResponse<List<MemberCoffeeChatResponse>> response =
                    memberCoffeeChatController.getCompletedReservations(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult()).isEmpty();

            verify(memberCoffeeChatService).getCompletedReservations("testMemberId");
        }

        @Test
        @DisplayName("성공: 취소된 커피챗 조회")
        void getCancelledReservations_Success() {
            // given
            List<MemberCoffeeChatResponse> cancelledReservations = Collections.emptyList();
            given(memberCoffeeChatService.getCancelledReservations("testMemberId"))
                    .willReturn(cancelledReservations);

            // when
            ApiResponse<List<MemberCoffeeChatResponse>> response =
                    memberCoffeeChatController.getCancelledReservations(testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isNotNull();
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
            assertThat(response.getResult()).isNotNull();
            assertThat(response.getResult()).hasSize(2);
            assertThat(response.getResult().get(0).getPreferredDateOnly()).isEqualTo("2024-12-01");
            assertThat(response.getResult().get(1).getPreferredTimeRange()).isEqualTo("19:00~20:00");

            verify(memberCoffeeChatService).getAllReservations("testMemberId");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원의 예약 조회")
        void getReservations_MemberNotFound() {
            // given
            given(memberCoffeeChatService.getAllReservations("invalidMemberId"))
                    .willThrow(new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

            UserDetails invalidUserDetails = new User("invalidMemberId", "password", Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> memberCoffeeChatController.getAllReservations(invalidUserDetails))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorStatus.MEMBER_NOT_FOUND);

            verify(memberCoffeeChatService).getAllReservations("invalidMemberId");
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 상황")
    class EdgeCaseTests {

        @Test
        @DisplayName("경계값: 빈 주제 목록으로 설정")
        void registerChatTopics_EmptyList() {
            // given
            MemberChatTopicRequest emptyRequest = MemberChatTopicRequest.builder()
                    .topicNames(Collections.emptyList())
                    .build();

            given(memberCoffeeChatService.registerChatTopics("testMemberId", emptyRequest))
                    .willReturn(Collections.emptyList());

            // when
            ApiResponse<List<MemberChatTopicResponse>> response =
                    memberCoffeeChatController.registerChatTopics(emptyRequest, testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult()).isEmpty();

            verify(memberCoffeeChatService).registerChatTopics("testMemberId", emptyRequest);
        }

        @Test
        @DisplayName("경계값: null 주제명으로 분야 설정")
        void registerJobField_NullJobName() {
            // given
            MemberJobFieldRequest nullRequest = MemberJobFieldRequest.builder()
                    .jobName(null)
                    .build();

            MemberJobFieldResponse undefinedResponse = MemberJobFieldResponse.builder()
                    .memberId("testMemberId")
                    .jobName(JobNameType.UNDEFINED)
                    .build();

            given(memberCoffeeChatService.registerJobField("testMemberId", nullRequest))
                    .willReturn(undefinedResponse);

            // when
            ApiResponse<MemberJobFieldResponse> response =
                    memberCoffeeChatController.registerJobField(nullRequest, testUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getResult().getJobName()).isEqualTo(JobNameType.UNDEFINED);

            verify(memberCoffeeChatService).registerJobField("testMemberId", nullRequest);
        }

        @Test
        @DisplayName("성공: 예약이 없는 회원의 조회")
        void getReservations_NoReservations() {
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