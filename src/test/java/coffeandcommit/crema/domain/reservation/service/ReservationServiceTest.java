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
import coffeandcommit.crema.domain.reservation.dto.response.ReservationDecisionResponseDTO;
import coffeandcommit.crema.domain.reservation.dto.response.ReservationResponseDTO;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.entity.Survey;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
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
                .files(fileRequestDTOs)
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

        // When
        ReservationResponseDTO result = reservationService.createReservation(MEMBER_ID, testReservationRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals(RESERVATION_ID, result.getReservationId());
        verify(memberRepository, times(1)).findById(MEMBER_ID);
        verify(guideRepository, times(1)).findById(GUIDE_ID);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("createReservation - 실패 케이스: 회원이 존재하지 않는 경우")
    void createReservation_MemberNotFound() {
        // Given
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.createReservation(MEMBER_ID, testReservationRequestDTO);
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
            reservationService.createReservation(MEMBER_ID, testReservationRequestDTO);
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

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When
        ReservationDecisionResponseDTO result = reservationService.decideReservation(guideLoginId, RESERVATION_ID, confirmRequest);

        // Then
        assertNotNull(result);
        assertEquals(Status.CONFIRMED.name(), result.getStatus());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("decideReservation - 성공 케이스: 예약 거절")
    void decideReservation_CancelSuccess() {
        // Given
        String guideLoginId = "guide-member-id";
        String cancelReason = "Schedule conflict";
        ReservationDecisionRequestDTO cancelRequest = ReservationDecisionRequestDTO.builder()
                .status(Status.CANCELLED)
                .reason(cancelReason)
                .build();

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When
        ReservationDecisionResponseDTO result = reservationService.decideReservation(guideLoginId, RESERVATION_ID, cancelRequest);

        // Then
        assertNotNull(result);
        assertEquals(Status.CANCELLED.name(), result.getStatus());
        assertEquals(cancelReason, result.getReason());
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

        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservationWithPoorMember));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.decideReservation(guideLoginId, RESERVATION_ID, testReservationDecisionRequestDTO);
        });

        assertEquals(ErrorStatus.INSUFFICIENT_POINTS, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }
}
