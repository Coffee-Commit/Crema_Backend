package coffeandcommit.crema.domain.review.service;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.TimeUnit;
import coffeandcommit.crema.domain.guide.enums.TimeType;
import coffeandcommit.crema.domain.guide.repository.ExperienceGroupRepository;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.domain.reservation.service.ReservationService;
import coffeandcommit.crema.domain.review.dto.request.ExperienceEvaluationRequestDTO;
import coffeandcommit.crema.domain.review.dto.request.ReviewRequestDTO;
import coffeandcommit.crema.domain.review.dto.response.ReviewResponseDTO;
import coffeandcommit.crema.domain.review.entity.Review;
import coffeandcommit.crema.domain.review.repository.ReviewRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReservationService reservationService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ExperienceGroupRepository experienceGroupRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReviewService reviewService;

    private final Long RESERVATION_ID = 1L;
    private final Long EXPERIENCE_GROUP_ID = 1L;
    private final String LOGIN_MEMBER_ID = "member123";
    private final String COMMENT = "This is a test review comment that is at least 10 characters long.";
    private final BigDecimal STAR_REVIEW = BigDecimal.valueOf(4.5);

    private Member testMember;
    private Guide testGuide;
    private Reservation testReservation;
    private ExperienceGroup testExperienceGroup;
    private ReviewRequestDTO testReviewRequestDTO;
    private Review testReview;

    @BeforeEach
    void setUp() {
        // Create test_member
        testMember = Member.builder()
                .id(LOGIN_MEMBER_ID)
                .build();

        // Create test_guide
        testGuide = Guide.builder()
                .id(100L)
                .member(testMember)
                .build();

        // Create test_reservation (먼저 생성!)
        testReservation = Reservation.builder()
                .id(RESERVATION_ID)
                .member(testMember)
                .guide(testGuide)
                .status(Status.COMPLETED)
                .matchingTime(LocalDateTime.now().minusHours(1))
                .build();

        // Create test time unit (reservation과 연결)
        TimeUnit timeUnit = TimeUnit.builder()
                .timeType(TimeType.MINUTE_30)
                .build();
        timeUnit.setReservation(testReservation);
        testReservation.setTimeUnit(timeUnit);

        // Create test experience_group
        testExperienceGroup = ExperienceGroup.builder()
                .id(EXPERIENCE_GROUP_ID)
                .experienceTitle("Test Experience")
                .build();

        // Create test review request DTO
        ExperienceEvaluationRequestDTO evaluationDTO = ExperienceEvaluationRequestDTO.builder()
                .experienceGroupId(EXPERIENCE_GROUP_ID)
                .isThumbsUp(true)
                .build();

        testReviewRequestDTO = ReviewRequestDTO.builder()
                .reservationId(RESERVATION_ID)
                .starReview(STAR_REVIEW)
                .comment(COMMENT)
                .experienceEvaluations(Collections.singletonList(evaluationDTO))
                .build();

        testReview = Review.builder()
                .id(1L)
                .reservation(testReservation)
                .starReview(STAR_REVIEW)
                .comment(COMMENT)
                .build();
    }


    @Test
    @DisplayName("createReview - 성공 케이스: 리뷰 생성 성공")
    void createReview_Success() {
        // Given
        when(reservationService.getReservationOrThrow(RESERVATION_ID)).thenReturn(testReservation);
        when(experienceGroupRepository.findById(EXPERIENCE_GROUP_ID)).thenReturn(java.util.Optional.of(testExperienceGroup));
        when(reviewRepository.existsByReservation(testReservation)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewRepository.findByIdWithExperiences(testReview.getId()))
                .thenReturn(Optional.of(testReview));

        // When
        ReviewResponseDTO result = reviewService.createReview(LOGIN_MEMBER_ID, testReviewRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals(testReview.getId(), result.getReviewId());
        assertEquals(RESERVATION_ID, result.getReservationId());
        assertEquals(STAR_REVIEW, result.getStarReview());
        assertEquals(COMMENT, result.getComment());

        // Verify interactions
        verify(reservationService, times(1)).getReservationOrThrow(RESERVATION_ID);
        verify(reviewRepository, times(1)).existsByReservation(testReservation);
        verify(experienceGroupRepository, times(1)).findById(EXPERIENCE_GROUP_ID);
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(reviewRepository, times(1)).findByIdWithExperiences(testReview.getId());

        // Capture the Review object passed to save method to verify its properties
        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        Review capturedReview = reviewCaptor.getValue();

        assertEquals(testReservation, capturedReview.getReservation());
        assertEquals(STAR_REVIEW, capturedReview.getStarReview());
        assertEquals(COMMENT, capturedReview.getComment());
        assertEquals(1, capturedReview.getExperienceEvaluations().size());
    }

    @Test
    @DisplayName("createReview - 실패 케이스: 본인 예약이 아닌 경우")
    void createReview_Forbidden() {
        // Given
        String differentMemberId = "differentMember";
        when(reservationService.getReservationOrThrow(RESERVATION_ID)).thenReturn(testReservation);

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reviewService.createReview(differentMemberId, testReviewRequestDTO);
        });

        assertEquals(ErrorStatus.FORBIDDEN, exception.getErrorCode());
        verify(reservationService, times(1)).getReservationOrThrow(RESERVATION_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("createReview - 실패 케이스: 이미 리뷰가 존재하는 경우")
    void createReview_DuplicateReview() {
        // Given
        when(reservationService.getReservationOrThrow(RESERVATION_ID)).thenReturn(testReservation);
        when(reviewRepository.existsByReservation(testReservation)).thenReturn(true);

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reviewService.createReview(LOGIN_MEMBER_ID, testReviewRequestDTO);
        });

        assertEquals(ErrorStatus.DUPLICATE_REVIEW, exception.getErrorCode());
        verify(reservationService, times(1)).getReservationOrThrow(RESERVATION_ID);
        verify(reviewRepository, times(1)).existsByReservation(testReservation);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("createReview - 실패 케이스: 경험 그룹을 찾을 수 없는 경우")
    void createReview_ExperienceNotFound() {
        // Given
        when(reservationService.getReservationOrThrow(RESERVATION_ID)).thenReturn(testReservation);
        when(reviewRepository.existsByReservation(testReservation)).thenReturn(false);
        when(experienceGroupRepository.findById(EXPERIENCE_GROUP_ID)).thenReturn(java.util.Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reviewService.createReview(LOGIN_MEMBER_ID, testReviewRequestDTO);
        });

        assertEquals(ErrorStatus.EXPERIENCE_NOT_FOUND, exception.getErrorCode());
        verify(reservationService, times(1)).getReservationOrThrow(RESERVATION_ID);
        verify(reviewRepository, times(1)).existsByReservation(testReservation);
        verify(experienceGroupRepository, times(1)).findById(EXPERIENCE_GROUP_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("createReview - 실패 케이스: 예약 상태가 COMPLETED가 아닌 경우")
    void createReview_InvalidStatus() {
        // Given
        Reservation pendingReservation = testReservation.toBuilder()
                .status(Status.PENDING)
                .build();

        when(reservationService.getReservationOrThrow(RESERVATION_ID)).thenReturn(pendingReservation);

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reviewService.createReview(LOGIN_MEMBER_ID, testReviewRequestDTO);
        });

        assertEquals(ErrorStatus.INVALID_STATUS, exception.getErrorCode());
        verify(reservationService, times(1)).getReservationOrThrow(RESERVATION_ID);
        verify(reviewRepository, never()).existsByReservation(any());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("createReview - 실패 케이스: 데이터 무결성 검증 실패 (matchingTime이 null인 경우)")
    void createReview_DataIntegrityFailure_NullMatchingTime() {
        // Given
        Reservation invalidReservation = testReservation.toBuilder()
                .matchingTime(null)
                .build();

        when(reservationService.getReservationOrThrow(RESERVATION_ID)).thenReturn(invalidReservation);

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reviewService.createReview(LOGIN_MEMBER_ID, testReviewRequestDTO);
        });

        assertEquals(ErrorStatus.INTERNAL_SERVER_ERROR, exception.getErrorCode());
        verify(reservationService, times(1)).getReservationOrThrow(RESERVATION_ID);
        verify(reviewRepository, never()).existsByReservation(any());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("createReview - 실패 케이스: 데이터 무결성 검증 실패 (timeUnit이 null인 경우)")
    void createReview_DataIntegrityFailure_NullTimeUnit() {
        // Given
        Reservation invalidReservation = testReservation.toBuilder()
                .timeUnit(null)
                .build();

        when(reservationService.getReservationOrThrow(RESERVATION_ID)).thenReturn(invalidReservation);

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reviewService.createReview(LOGIN_MEMBER_ID, testReviewRequestDTO);
        });

        assertEquals(ErrorStatus.INTERNAL_SERVER_ERROR, exception.getErrorCode());
        verify(reservationService, times(1)).getReservationOrThrow(RESERVATION_ID);
        verify(reviewRepository, never()).existsByReservation(any());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("createReview - 실패 케이스: 미팅이 아직 끝나지 않은 경우")
    void createReview_MeetingNotEndedYet() {
        // Given
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1); // Meeting will end in the future

        Reservation futureReservation = testReservation.toBuilder()
                .matchingTime(futureTime)
                .build();

        when(reservationService.getReservationOrThrow(RESERVATION_ID)).thenReturn(futureReservation);

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reviewService.createReview(LOGIN_MEMBER_ID, testReviewRequestDTO);
        });

        assertEquals(ErrorStatus.REVIEW_NOT_ALLOWED_YET, exception.getErrorCode());
        verify(reservationService, times(1)).getReservationOrThrow(RESERVATION_ID);
        verify(reviewRepository, never()).existsByReservation(any());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("getMyReviews - 성공 케이스: 완료된 예약 목록 조회 (리뷰 있음/없음 혼합)")
    void getMyReviews_Success() {
        // Given
        // Create a second reservation without a review
        Reservation secondReservation = Reservation.builder()
                .id(2L)
                .member(testMember)
                .guide(testGuide)
                .status(Status.COMPLETED)
                .matchingTime(LocalDateTime.now().minusHours(2))
                .build();

        TimeUnit timeUnit2 = TimeUnit.builder()
                .timeType(TimeType.MINUTE_30)
                .build();
        timeUnit2.setReservation(secondReservation);
        secondReservation.setTimeUnit(timeUnit2);

        // Setup mocks
        when(reservationRepository.findByMember_IdAndStatus(LOGIN_MEMBER_ID, Status.COMPLETED))
                .thenReturn(java.util.List.of(testReservation, secondReservation));

        when(reviewRepository.findByReservationId(RESERVATION_ID))
                .thenReturn(Optional.of(testReview));

        when(reviewRepository.findByReservationId(2L))
                .thenReturn(Optional.empty());

        // When
        var result = reviewService.getMyReviews(LOGIN_MEMBER_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(RESERVATION_ID, result.get(0).getReservationId());
        assertNotNull(result.get(0).getReview());
        assertEquals(STAR_REVIEW.doubleValue(), result.get(0).getReview().getStar());
        assertEquals(COMMENT, result.get(0).getReview().getComment());

        assertEquals(2L, result.get(1).getReservationId());
        assertNull(result.get(1).getReview());

        // Verify interactions
        verify(reservationRepository, times(1)).findByMember_IdAndStatus(LOGIN_MEMBER_ID, Status.COMPLETED);
        verify(reviewRepository, times(1)).findByReservationId(RESERVATION_ID);
        verify(reviewRepository, times(1)).findByReservationId(2L);
    }

    @Test
    @DisplayName("getMyReviews - 실패 케이스: 완료된 예약이 없는 경우")
    void getMyReviews_NoReservationsFound() {
        // Given
        when(reservationRepository.findByMember_IdAndStatus(LOGIN_MEMBER_ID, Status.COMPLETED))
                .thenReturn(Collections.emptyList());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reviewService.getMyReviews(LOGIN_MEMBER_ID);
        });

        assertEquals(ErrorStatus.RESERVATION_NOT_FOUND, exception.getErrorCode());
        verify(reservationRepository, times(1)).findByMember_IdAndStatus(LOGIN_MEMBER_ID, Status.COMPLETED);
        verify(reviewRepository, never()).findByReservationId(any());
    }
}
