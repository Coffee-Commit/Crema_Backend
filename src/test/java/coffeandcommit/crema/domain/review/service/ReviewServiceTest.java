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
import coffeandcommit.crema.domain.review.dto.response.MyReviewResponseDTO;
import coffeandcommit.crema.domain.review.dto.response.ReviewResponseDTO;
import coffeandcommit.crema.domain.review.dto.response.ReviewInfo;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
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
    @DisplayName("getMyReviews - 성공: filter=ALL")
    void getMyReviews_Success_All() {
        MyReviewResponseDTO dto1 = MyReviewResponseDTO.builder()
                .reservationId(RESERVATION_ID)
                .review(ReviewInfo.builder().reviewId(1L).build())
                .build();
        MyReviewResponseDTO dto2 = MyReviewResponseDTO.builder()
                .reservationId(2L)
                .review(null)
                .build();

        when(reservationRepository.findMyReviews(eq(LOGIN_MEMBER_ID), eq(Status.COMPLETED),
                eq(coffeandcommit.crema.domain.review.enums.ReviewWriteFilter.ALL), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(dto1, dto2), pageable, 2));

        Page<MyReviewResponseDTO> result = reviewService.getMyReviews(LOGIN_MEMBER_ID, "ALL", pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(RESERVATION_ID, result.getContent().get(0).getReservationId());
        assertEquals(2L, result.getContent().get(1).getReservationId());
        assertNotNull(result.getContent().get(0).getReview());
        assertNull(result.getContent().get(1).getReview());
    }

    @Test
    @DisplayName("getMyReviews - 성공: filter=WRITTEN")
    void getMyReviews_Success_Written() {
        MyReviewResponseDTO dto = MyReviewResponseDTO.builder()
                .reservationId(RESERVATION_ID)
                .review(ReviewInfo.builder().reviewId(1L).build())
                .build();

        when(reservationRepository.findMyReviews(eq(LOGIN_MEMBER_ID), eq(Status.COMPLETED),
                eq(coffeandcommit.crema.domain.review.enums.ReviewWriteFilter.WRITTEN), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(dto), pageable, 1));

        Page<MyReviewResponseDTO> result = reviewService.getMyReviews(LOGIN_MEMBER_ID, "WRITTEN", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertNotNull(result.getContent().get(0).getReview());
    }

    @Test
    @DisplayName("getMyReviews - 성공: filter=NOT_WRITTEN")
    void getMyReviews_Success_NotWritten() {
        MyReviewResponseDTO dto = MyReviewResponseDTO.builder()
                .reservationId(2L)
                .review(null)
                .build();

        when(reservationRepository.findMyReviews(eq(LOGIN_MEMBER_ID), eq(Status.COMPLETED),
                eq(coffeandcommit.crema.domain.review.enums.ReviewWriteFilter.NOT_WRITTEN), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(dto), pageable, 1));

        Page<MyReviewResponseDTO> result = reviewService.getMyReviews(LOGIN_MEMBER_ID, "NOT_WRITTEN", pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().get(0).getReview());
    }

    @Test
    @DisplayName("getMyReviews - 빈 결과: 완료된 예약이 없는 경우")
    void getMyReviews_NoReservationsFound() {
        when(reservationRepository.findMyReviews(eq(LOGIN_MEMBER_ID), eq(Status.COMPLETED),
                eq(coffeandcommit.crema.domain.review.enums.ReviewWriteFilter.ALL), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<MyReviewResponseDTO> result = reviewService.getMyReviews(LOGIN_MEMBER_ID, "ALL", pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}
