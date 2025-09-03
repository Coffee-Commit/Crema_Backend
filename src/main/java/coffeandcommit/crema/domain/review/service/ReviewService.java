package coffeandcommit.crema.domain.review.service;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import coffeandcommit.crema.domain.guide.repository.ExperienceGroupRepository;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.service.ReservationService;
import coffeandcommit.crema.domain.review.dto.request.ReviewRequestDTO;
import coffeandcommit.crema.domain.review.dto.response.ReviewResponseDTO;
import coffeandcommit.crema.domain.review.entity.Review;
import coffeandcommit.crema.domain.review.entity.ReviewExperience;
import coffeandcommit.crema.domain.review.repository.ReviewRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReservationService reservationService;
    private final ReviewRepository reviewRepository;
    private final ExperienceGroupRepository experienceGroupRepository;

    /* 리뷰 생성 */
    @Transactional
    public ReviewResponseDTO createReview(String loginMemberId, @Valid ReviewRequestDTO reviewRequestDTO) {

        // 1. 예약 조회 (서비스 메서드 활용)
        Reservation reservation = reservationService.getReservationOrThrow(reviewRequestDTO.getReservationId());

        // 2. 본인 예약 검증
        if (!reservation.getMember().getId().equals(loginMemberId)) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        // 3. 중복 리뷰 방지
        if (reviewRepository.existsByReservation(reservation)) {
            throw new BaseException(ErrorStatus.DUPLICATE_REVIEW);
        }

        // 4. Review 생성
        Review review = Review.builder()
                .reservation(reservation)
                .starReview(BigDecimal.valueOf(reviewRequestDTO.getStarReview()))
                .comment(reviewRequestDTO.getComment())
                .build();

        // 5. 경험 평가 매핑
        List<ReviewExperience> experiences = reviewRequestDTO.getExperienceEvaluations().stream()
                .map(e -> {
                    ExperienceGroup experienceGroup = experienceGroupRepository.findById(e.getExperienceGroupId())
                            .orElseThrow(() -> new BaseException(ErrorStatus.EXPERIENCE_NOT_FOUND));

                    return ReviewExperience.builder()
                            .review(review)
                            .experienceGroup(experienceGroup)
                            .thumbsUp(e.getThumbsUp())
                            .build();
                })
                .toList();

        review.getExperienceEvaluations().addAll(experiences);

        // 6. 저장
        Review saved = reviewRepository.save(review);

        // 7. DTO 변환
        return ReviewResponseDTO.from(saved);
    }
}
