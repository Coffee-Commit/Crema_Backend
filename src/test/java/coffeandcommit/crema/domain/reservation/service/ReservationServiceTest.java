package coffeandcommit.crema.domain.reservation.service;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.TimeUnit;
import coffeandcommit.crema.domain.guide.enums.TimeType;
import coffeandcommit.crema.domain.guide.repository.GuideRepository;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.reservation.dto.request.ReservationDecisionRequestDTO;
import coffeandcommit.crema.domain.reservation.dto.request.ReservationRequestDTO;
import coffeandcommit.crema.domain.reservation.dto.request.SurveyFileRequestDTO;
import coffeandcommit.crema.domain.reservation.dto.request.SurveyRequestDTO;
import coffeandcommit.crema.domain.reservation.dto.response.CoffeeChatSummaryResponseDTO;
import coffeandcommit.crema.domain.reservation.dto.response.ReservationApplyResponseDTO;
import coffeandcommit.crema.domain.reservation.dto.response.ReservationCompletionResponseDTO;
import coffeandcommit.crema.domain.reservation.dto.response.ReservationDecisionResponseDTO;
import coffeandcommit.crema.domain.reservation.dto.response.ReservationResponseDTO;
import coffeandcommit.crema.domain.reservation.dto.response.ReservationSurveyResponseDTO;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.entity.Survey;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.file.FileService;
import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.validation.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private GuideRepository guideRepository;

    @Mock
    private FileService fileService;

    @InjectMocks
    private ReservationService reservationService;

    private Reservation testReservation;
    private Member testMember;
    private Guide testGuide;
    private ReservationRequestDTO testReservationRequestDTO;
    private ReservationDecisionRequestDTO testReservationDecisionRequestDTO;
    private final Long RESERVATION_ID = 1L;
    private final String MEMBER_ID = "test-member-id";
    private final Long GUIDE_ID = 1L;

    @BeforeEach
    void setUp() {
        // Create test member
        testMember = Member.builder()
                .id(MEMBER_ID)
                .point(20000) // Enough points for a reservation
                .build();

        // Create test guide
        testGuide = Guide.builder()
                .id(GUIDE_ID)
                .member(Member.builder().id("guide-member-id").build())
                .build();

        // Create test survey request DTO
        List<SurveyFileRequestDTO> fileRequestDTOs = Arrays.asList(
            SurveyFileRequestDTO.builder()
                .fileUploadUrl("https://example.com/file1.pdf")
                .build()
        );

        SurveyRequestDTO surveyRequestDTO = SurveyRequestDTO.builder()
                .messageToGuide("I would like to discuss career opportunities")
                .preferredDate(LocalDateTime.now().plusDays(7))
                .build();

        // Create test reservation request DTO
        testReservationRequestDTO = ReservationRequestDTO.builder()
                .guideId(GUIDE_ID)
                .timeUnit(TimeType.MINUTE_30)
                .survey(surveyRequestDTO)
                .build();

        // Create test reservation decision request DTO for confirmation
        testReservationDecisionRequestDTO = ReservationDecisionRequestDTO.builder()
                .status(Status.CONFIRMED)
                .build();

        // Create a test survey
        Survey testSurvey = Survey.builder()
                .id(1L)
                .messageToGuide("I would like to discuss career opportunities")
                .preferredDate(LocalDateTime.now().plusDays(7))
                .build();

        // Create a test time unit
        TimeUnit testTimeUnit = TimeUnit.builder()
                .id(1L)
                .timeType(TimeType.MINUTE_30)
                .build();

        // Create a test reservation
        testReservation = Reservation.builder()
                .id(RESERVATION_ID)
                .guide(testGuide)
                .member(testMember)
                .status(Status.PENDING)
                .survey(testSurvey)
                .build();

        testTimeUnit.setReservation(testReservation);
        testReservation.setTimeUnit(testTimeUnit);
    }

    @Test
    @DisplayName("getReservationOrThrow - 성공 케이스: 예약이 존재하는 경우")
    void getReservationOrThrow_Success() {
        // Given
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When
        Reservation result = reservationService.getReservationOrThrow(RESERVATION_ID);

        // Then
        assertNotNull(result);
        assertEquals(RESERVATION_ID, result.getId());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getReservationOrThrow - 실패 케이스: 예약이 존재하지 않는 경우")
    void getReservationOrThrow_ReservationNotFound() {
        // Given
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.getReservationOrThrow(RESERVATION_ID);
        });

        assertEquals(ErrorStatus.RESERVATION_NOT_FOUND, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getReservationOrThrow - 실패 케이스: reservationId가 null인 경우")
    void getReservationOrThrow_NullReservationId() {
        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.getReservationOrThrow(null);
        });
        assertEquals(ErrorStatus.INVALID_RESERVATION_ID, exception.getErrorCode());
    }

    @Test
    @DisplayName("createReservation - 성공 케이스: 예약 생성 성공")
    void createReservation_Success() {
        // Given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(testMember));
        when(guideRepository.findById(GUIDE_ID)).thenReturn(Optional.of(testGuide));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        // 파일 업로드 Mock 설정
        MultipartFile mockFile = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(mockFile);

        FileUploadResponse mockUploadResponse = FileUploadResponse.builder()
                .fileKey("survey-files/" + MEMBER_ID + "_test.pdf")
                .fileUrl("https://storage.googleapis.com/bucket/survey-files/" + MEMBER_ID + "_test.pdf")
                .build();

        when(fileService.uploadFile(eq(mockFile), eq(FileType.PDF), eq("survey-files"), eq(MEMBER_ID)))
                .thenReturn(mockUploadResponse);

        // When
        ReservationResponseDTO result =
                reservationService.createReservation(MEMBER_ID, testReservationRequestDTO, files);

        // Then
        assertNotNull(result);
        assertEquals(RESERVATION_ID, result.getReservationId());

        // Repository 호출 검증
        verify(memberRepository, times(1)).findById(MEMBER_ID);
        verify(guideRepository, times(1)).findById(GUIDE_ID);

        // save가 실제로 2번 호출됨 → 검증 수정
        verify(reservationRepository, times(2)).save(any(Reservation.class));

        // 파일 업로드가 제대로 호출되었는지 검증
        verify(fileService, times(1))
                .uploadFile(eq(mockFile), eq(FileType.PDF), eq("survey-files"), eq(MEMBER_ID));
    }


    @Test
    @DisplayName("createReservation - 실패 케이스: 회원이 존재하지 않는 경우")
    void createReservation_MemberNotFound() {
        // Given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.createReservation(MEMBER_ID, testReservationRequestDTO,null);
        });

        assertEquals(ErrorStatus.MEMBER_NOT_FOUND, exception.getErrorCode());
        verify(memberRepository, times(1)).findById(MEMBER_ID);
        verify(guideRepository, never()).findById(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createReservation - 실패 케이스: 가이드가 존재하지 않는 경우")
    void createReservation_GuideNotFound() {
        // Given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(testMember));
        when(guideRepository.findById(GUIDE_ID)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.createReservation(MEMBER_ID, testReservationRequestDTO, Collections.emptyList());
        });

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());
        verify(memberRepository, times(1)).findById(MEMBER_ID);
        verify(guideRepository, times(1)).findById(GUIDE_ID);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("decideReservation - 성공 케이스: 예약 승인")
    void decideReservation_ConfirmSuccess() {
        // Given
        String guideLoginId = "guide-member-id";
        ReservationDecisionRequestDTO confirmRequest = ReservationDecisionRequestDTO.builder()
                .status(Status.CONFIRMED)
                .build();

        // TimeUnit 세팅 (가격 스냅샷 포함)
        TimeUnit timeUnit = TimeUnit.builder()
                .timeType(TimeType.MINUTE_30) // 가격이 들어있는 enum 값
                .build();
        testReservation.setTimeUnit(timeUnit);

        // ReservationRepository mock (기존 getReservationOrThrow 내부에서 findById 호출됨)
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(testReservation));

        // Pessimistic lock 적용된 MemberRepository mock
        when(memberRepository.findByIdForUpdate(testMember.getId()))
                .thenReturn(Optional.of(testMember));
        when(memberRepository.findByIdForUpdate(testGuide.getMember().getId()))
                .thenReturn(Optional.of(testGuide.getMember()));

        // When
        ReservationDecisionResponseDTO result =
                reservationService.decideReservation(guideLoginId, RESERVATION_ID, confirmRequest);

        // Then
        assertNotNull(result);
        assertEquals(Status.CONFIRMED.name(), result.getStatus());

        // Verify 락 쿼리 호출
        verify(memberRepository, times(1)).findByIdForUpdate(testMember.getId());
        verify(memberRepository, times(1)).findByIdForUpdate(testGuide.getMember().getId());

        // Verify 포인트 이동
        int expectedPrice = testReservation.getTimeUnit().getTimeType().getPrice();
        assertEquals(20000 - expectedPrice, testMember.getPoint()); // 멘티 포인트 차감
        assertEquals(expectedPrice, testGuide.getMember().getPoint()); // 가이드 포인트 적립

        // ReservationRepository 호출 검증
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("decideReservation - 성공 케이스: 예약 거절")
    void decideReservation_CancelSuccess() {
        // Given
        String guideLoginId = "guide-member-id";
        ReservationDecisionRequestDTO cancelRequest = ReservationDecisionRequestDTO.builder()
                .status(Status.CANCELLED)
                .build();

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When
        ReservationDecisionResponseDTO result = reservationService.decideReservation(guideLoginId, RESERVATION_ID, cancelRequest);

        // Then
        assertNotNull(result);
        assertEquals(Status.CANCELLED.name(), result.getStatus());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("decideReservation - 실패 케이스: 예약이 존재하지 않는 경우")
    void decideReservation_ReservationNotFound() {
        // Given
        String guideLoginId = "guide-member-id";
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.decideReservation(guideLoginId, RESERVATION_ID, testReservationDecisionRequestDTO);
        });

        assertEquals(ErrorStatus.RESERVATION_NOT_FOUND, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("decideReservation - 실패 케이스: 이미 처리된 예약인 경우")
    void decideReservation_AlreadyDecided() {
        // Given
        String guideLoginId = "guide-member-id";
        Reservation alreadyConfirmedReservation = testReservation.toBuilder()
                .status(Status.CONFIRMED)
                .build();

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(alreadyConfirmedReservation));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.decideReservation(guideLoginId, RESERVATION_ID, testReservationDecisionRequestDTO);
        });

        assertEquals(ErrorStatus.ALREADY_DECIDED, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("decideReservation - 실패 케이스: 로그인한 사용자가 해당 예약의 가이드가 아닌 경우")
    void decideReservation_NotGuide() {
        // Given
        String wrongGuideLoginId = "wrong-guide-id";
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.decideReservation(wrongGuideLoginId, RESERVATION_ID, testReservationDecisionRequestDTO);
        });

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("decideReservation - 실패 케이스: 잘못된 상태값이 전달된 경우")
    void decideReservation_InvalidStatus() {
        // Given
        String guideLoginId = "guide-member-id";
        ReservationDecisionRequestDTO invalidStatusRequest = ReservationDecisionRequestDTO.builder()
                .status(Status.PENDING) // PENDING은 허용되지 않음
                .build();

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.decideReservation(guideLoginId, RESERVATION_ID, invalidStatusRequest);
        });

        assertEquals(ErrorStatus.INVALID_STATUS, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("decideReservation - 실패 케이스: 포인트가 부족한 경우")
    void decideReservation_InsufficientPoints() {
        // Given
        String guideLoginId = "guide-member-id";
        Member poorMember = Member.builder()
                .id("poor-member-id")
                .point(0) // 포인트 부족
                .build();

        Reservation reservationWithPoorMember = testReservation.toBuilder()
                .member(poorMember)
                .build();

        // Reservation mock
        when(reservationRepository.findById(RESERVATION_ID))
                .thenReturn(Optional.of(reservationWithPoorMember));

        // Pessimistic lock mock (멘티만 차감 시도)
        when(memberRepository.findByIdForUpdate(poorMember.getId()))
                .thenReturn(Optional.of(poorMember));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.decideReservation(guideLoginId, RESERVATION_ID, testReservationDecisionRequestDTO);
        });

        assertEquals(ErrorStatus.INSUFFICIENT_POINTS, exception.getErrorCode());

        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
        verify(memberRepository, times(1)).findByIdForUpdate(poorMember.getId());
        verify(memberRepository, never()).findByIdForUpdate(testGuide.getMember().getId());
    }

    @Test
    @DisplayName("decideReservation - 실패 케이스: timeUnit이 null인 경우")
    void decideReservation_NullTimeUnit() {
        // Given
        String guideLoginId = "guide-member-id";
        ReservationDecisionRequestDTO confirmRequest = ReservationDecisionRequestDTO.builder()
                .status(Status.CONFIRMED)
                .build();

        Reservation reservationWithNullTimeUnit = testReservation.toBuilder()
                .timeUnit(null)
                .build();

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservationWithNullTimeUnit));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.decideReservation(guideLoginId, RESERVATION_ID, confirmRequest);
        });

        assertEquals(ErrorStatus.INVALID_TIME_UNIT, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
        verify(memberRepository, never()).findById(any());
    }

    @Test
    @DisplayName("decideReservation - 실패 케이스: timeUnit.timeType이 null인 경우")
    void decideReservation_NullTimeType() {
        // Given
        String guideLoginId = "guide-member-id";
        ReservationDecisionRequestDTO confirmRequest = ReservationDecisionRequestDTO.builder()
                .status(Status.CONFIRMED)
                .build();

        TimeUnit timeUnitWithNullType = TimeUnit.builder()
                .id(1L)
                .timeType(null)
                .build();

        Reservation reservationWithNullTimeType = testReservation.toBuilder()
                .timeUnit(timeUnitWithNullType)
                .build();

        timeUnitWithNullType.setReservation(reservationWithNullTimeType);

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservationWithNullTimeType));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.decideReservation(guideLoginId, RESERVATION_ID, confirmRequest);
        });

        assertEquals(ErrorStatus.INVALID_TIME_UNIT, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
        verify(memberRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getReservationApply - 성공 케이스: 예약 신청 정보 조회")
    void getReservationApply_Success() {
        // Given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(testMember));
        when(guideRepository.findById(GUIDE_ID)).thenReturn(Optional.of(testGuide));

        // When
        ReservationApplyResponseDTO result = reservationService.getReservationApply(GUIDE_ID, MEMBER_ID);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMember());
        assertNotNull(result.getGuide());
        assertEquals(MEMBER_ID, result.getMember().getId());
        assertEquals(GUIDE_ID, result.getGuide().getId());

        // Verify repository calls
        verify(memberRepository, times(1)).findById(MEMBER_ID);
        verify(guideRepository, times(1)).findById(GUIDE_ID);
    }

    @Test
    @DisplayName("getReservationApply - 실패 케이스: 멤버가 존재하지 않는 경우")
    void getReservationApply_MemberNotFound() {
        // Given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.getReservationApply(GUIDE_ID, MEMBER_ID);
        });

        assertEquals(ErrorStatus.MEMBER_NOT_FOUND, exception.getErrorCode());
        verify(memberRepository, times(1)).findById(MEMBER_ID);
        verify(guideRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getReservationApply - 실패 케이스: 가이드가 존재하지 않는 경우")
    void getReservationApply_GuideNotFound() {
        // Given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(testMember));
        when(guideRepository.findById(GUIDE_ID)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.getReservationApply(GUIDE_ID, MEMBER_ID);
        });

        assertEquals(ErrorStatus.GUIDE_NOT_FOUND, exception.getErrorCode());
        verify(memberRepository, times(1)).findById(MEMBER_ID);
        verify(guideRepository, times(1)).findById(GUIDE_ID);
    }

    @Test
    @DisplayName("getSurvey - 성공 케이스: 멘티가 자신의 설문 조회")
    void getSurvey_SuccessForMentee() {
        // Given
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When
        ReservationSurveyResponseDTO result = reservationService.getSurvey(RESERVATION_ID, MEMBER_ID);

        // Then
        assertNotNull(result);
        assertEquals(testReservation.getSurvey().getMessageToGuide(), result.getMessageToGuide());
        assertNotNull(result.getMember());
        assertNotNull(result.getGuide());
        assertEquals(MEMBER_ID, result.getMember().getId());

        // Verify repository calls
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getSurvey - 성공 케이스: 가이드가 멘티의 설문 조회")
    void getSurvey_SuccessForGuide() {
        // Given
        String guideLoginId = "guide-member-id";
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When
        ReservationSurveyResponseDTO result = reservationService.getSurvey(RESERVATION_ID, guideLoginId);

        // Then
        assertNotNull(result);
        assertEquals(testReservation.getSurvey().getMessageToGuide(), result.getMessageToGuide());
        assertNotNull(result.getMember());
        assertNotNull(result.getGuide());
        assertEquals(MEMBER_ID, result.getMember().getId());

        // Verify repository calls
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getSurvey - 실패 케이스: 예약이 존재하지 않는 경우")
    void getSurvey_ReservationNotFound() {
        // Given
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.getSurvey(RESERVATION_ID, MEMBER_ID);
        });

        assertEquals(ErrorStatus.RESERVATION_NOT_FOUND, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getSurvey - 실패 케이스: 권한이 없는 사용자가 조회하는 경우")
    void getSurvey_Forbidden() {
        // Given
        String unauthorizedUserId = "unauthorized-user-id";
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.getSurvey(RESERVATION_ID, unauthorizedUserId);
        });

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getSurvey - 실패 케이스: 설문이 존재하지 않는 경우")
    void getSurvey_SurveyNotFound() {
        // Given
        Reservation reservationWithoutSurvey = testReservation.toBuilder()
                .survey(null)
                .build();
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservationWithoutSurvey));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.getSurvey(RESERVATION_ID, MEMBER_ID);
        });

        assertEquals(ErrorStatus.SURVEY_NOT_FOUND, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getMyCoffeeChatSummary - 성공 케이스: 커피챗 요약 정보 조회")
    void getMyCoffeeChatSummary_Success() {
        // Given
        int pendingCount = 2;
        int confirmedCount = 3;
        int completedCount = 1;

        when(reservationRepository.countByMember_IdAndStatus(MEMBER_ID, Status.PENDING)).thenReturn(pendingCount);
        when(reservationRepository.countByMember_IdAndStatus(MEMBER_ID, Status.CONFIRMED)).thenReturn(confirmedCount);
        when(reservationRepository.countByMember_IdAndStatus(MEMBER_ID, Status.COMPLETED)).thenReturn(completedCount);

        // When
        CoffeeChatSummaryResponseDTO result = reservationService.getMyCoffeeChatSummary(MEMBER_ID);

        // Then
        assertNotNull(result);
        assertEquals(pendingCount, result.getPendingCount());
        assertEquals(confirmedCount, result.getConfirmedCount());
        assertEquals(completedCount, result.getCompletedCount());

        // Verify repository calls
        verify(reservationRepository, times(1)).countByMember_IdAndStatus(MEMBER_ID, Status.PENDING);
        verify(reservationRepository, times(1)).countByMember_IdAndStatus(MEMBER_ID, Status.CONFIRMED);
        verify(reservationRepository, times(1)).countByMember_IdAndStatus(MEMBER_ID, Status.COMPLETED);
    }

    @Test
    @DisplayName("getReservationCompletion - 성공 케이스: 예약 완료 정보 조회")
    void getReservationCompletion_Success() {
        // Given
        LocalDateTime preferredDateTime = LocalDateTime.of(2023, 10, 15, 14, 0); // 2023-10-15 14:00
        TimeType timeType = TimeType.MINUTE_30;

        // Update survey with preferred date
        Survey surveyWithDate = testReservation.getSurvey().toBuilder()
                .preferredDate(preferredDateTime)
                .build();

        // Update time unit with time type
        TimeUnit timeUnitWithType = testReservation.getTimeUnit().toBuilder()
                .timeType(timeType)
                .build();

        // Create reservation with updated survey and time unit
        Reservation reservationWithDetails = testReservation.toBuilder()
                .survey(surveyWithDate)
                .build();

        reservationWithDetails.setTimeUnit(timeUnitWithType);
        timeUnitWithType.setReservation(reservationWithDetails);

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservationWithDetails));

        // When
        ReservationCompletionResponseDTO result = reservationService.getReservationCompletion(MEMBER_ID, RESERVATION_ID);

        // Then
        assertNotNull(result);
        assertEquals(RESERVATION_ID, result.getReservationId());
        assertEquals("2023-10-15", result.getPreferredDateOnly());
        assertEquals("일", result.getPreferredDayOfWeek());
        assertTrue(result.getPreferredTimeRange().startsWith("14:00"));
        assertEquals(timeType.getPrice(), result.getPrice());

        // Verify repository calls
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getReservationCompletion - 실패 케이스: 예약이 존재하지 않는 경우")
    void getReservationCompletion_ReservationNotFound() {
        // Given
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.getReservationCompletion(MEMBER_ID, RESERVATION_ID);
        });

        assertEquals(ErrorStatus.RESERVATION_NOT_FOUND, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getReservationCompletion - 실패 케이스: 권한이 없는 사용자가 조회하는 경우")
    void getReservationCompletion_Forbidden() {
        // Given
        String unauthorizedUserId = "unauthorized-user-id";
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.getReservationCompletion(unauthorizedUserId, RESERVATION_ID);
        });

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }
}
