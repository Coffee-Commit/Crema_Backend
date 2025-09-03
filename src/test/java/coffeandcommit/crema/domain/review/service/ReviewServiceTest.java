package coffeandcommit.crema.domain.review.service;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import coffeandcommit.crema.domain.guide.repository.ExperienceGroupRepository;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
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
import java.util.Collections;

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

    @InjectMocks
    private ReviewService reviewService;

    private final Long RESERVATION_ID = 1L;
    private final Long EXPERIENCE_GROUP_ID = 1L;
    private final String LOGIN_MEMBER_ID = "member123";
    private final String COMMENT = "This is a test review comment that is at least 10 characters long.";
    private final float STAR_REVIEW = 4.5f;

    private Member testMember;
    private Reservation testReservation;
    private ExperienceGroup testExperienceGroup;
    private ReviewRequestDTO testReviewRequestDTO;
    private Review testReview;

    @BeforeEach
    void setUp() {
        // Create test member
        testMember = Member.builder()
                .id(LOGIN_MEMBER_ID)
                .build();

        // Create test reservation
        testReservation = Reservation.builder()
                .id(RESERVATION_ID)
                .member(testMember)
                .build();

        // Create test experience group
        testExperienceGroup = ExperienceGroup.builder()
                .id(EXPERIENCE_GROUP_ID)
                .experienceTitle("Test Experience")
                .build();

        // Create test review request DTO
        ExperienceEvaluationRequestDTO evaluationDTO = ExperienceEvaluationRequestDTO.builder()
                .experienceGroupId(EXPERIENCE_GROUP_ID)
                .thumbsUp(true)
                .build();

        testReviewRequestDTO = ReviewRequestDTO.builder()
                .reservationId(RESERVATION_ID)
                .starReview(STAR_REVIEW)
                .comment(COMMENT)
                .experienceEvaluations(Collections.singletonList(evaluationDTO))
                .build();

        // Create test review
        testReview = Review.builder()
                .id(1L)
                .reservation(testReservation)
                .starReview(BigDecimal.valueOf(STAR_REVIEW))
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

        // When
        ReviewResponseDTO result = reviewService.createReview(LOGIN_MEMBER_ID, testReviewRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals(testReview.getId(), result.getReviewId());
        assertEquals(RESERVATION_ID, result.getReservationId());
        assertEquals(STAR_REVIEW, result.getStarReview(), 0.001);
        assertEquals(COMMENT, result.getComment());

        // Verify interactions
        verify(reservationService, times(1)).getReservationOrThrow(RESERVATION_ID);
        verify(reviewRepository, times(1)).existsByReservation(testReservation);
        verify(experienceGroupRepository, times(1)).findById(EXPERIENCE_GROUP_ID);
        verify(reviewRepository, times(1)).save(any(Review.class));

        // Capture the Review object passed to save method to verify its properties
        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        Review capturedReview = reviewCaptor.getValue();

        assertEquals(testReservation, capturedReview.getReservation());
        assertEquals(BigDecimal.valueOf(STAR_REVIEW), capturedReview.getStarReview());
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
}