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

        // 4. 별점 검증 및 가공 (0.5 단위)
        BigDecimal rawStar = reviewRequestDTO.getStarReview();
        if (!isValidStarStep(rawStar)) {
            throw new BaseException(ErrorStatus.VALIDATION_ERROR);
        }

        // 5. Review 생성
        Review review = Review.builder()
                .reservation(reservation)
                .starReview(reviewRequestDTO.getStarReview())
                .comment(reviewRequestDTO.getComment())
                .build();

        // 6. 경험 평가 매핑
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

        experiences.forEach(review::addExperienceEvaluation);

        // 7. 저장
        Review saved = reviewRepository.save(review);

        // 8. fetch join 으로 다시 조회 (experienceGroup 포함)
        Review fullyLoaded = reviewRepository.findByIdWithExperiences(saved.getId())
                .orElseThrow(() -> new BaseException(ErrorStatus.REVIEW_NOT_FOUND));

        // 9. DTO 변환
        return ReviewResponseDTO.from(fullyLoaded);
    }
    private boolean isValidStarStep(BigDecimal starReview) {
        if (starReview == null) return false;

        // (별점 * 10) % 5 == 0 → 0.5 단위만 허용
        return starReview
                .multiply(BigDecimal.TEN)
                .remainder(new BigDecimal("5"))
                .compareTo(BigDecimal.ZERO) == 0;
    }

}
