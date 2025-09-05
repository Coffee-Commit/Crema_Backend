package coffeandcommit.crema.domain.review.service;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import coffeandcommit.crema.domain.guide.repository.ExperienceGroupRepository;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
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

import java.time.LocalDateTime;

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

        // 2-1. 상태 기반 차단: COMPLETED 이외에는 리뷰 불가
        if (reservation.getStatus() != Status.COMPLETED) {
            throw new BaseException(ErrorStatus.INVALID_STATUS);
            // 필요하다면 REVIEW_NOT_ALLOWED 같은 전용 에러코드 만들어도 됨
        }

        // 2-2. null 가드 (데이터 무결성 검증)
        if (reservation.getMatchingTime() == null
                || reservation.getTimeUnit() == null
                || reservation.getTimeUnit().getTimeType() == null) {
            throw new BaseException(ErrorStatus.INTERNAL_SERVER_ERROR);
            // 운영 데이터 오류 → 서버 내부 오류 처리
        }

        // 2-3. 리뷰 작성 가능 시점 검증
        LocalDateTime startTime = reservation.getMatchingTime();
        int minutes = reservation.getTimeUnit().getTimeType().getMinutes();
        LocalDateTime endTime = startTime.plusMinutes(minutes);

        if (LocalDateTime.now().isBefore(endTime)) {
            throw new BaseException(ErrorStatus.REVIEW_NOT_ALLOWED_YET);
        }

        // 3. 중복 리뷰 방지
        if (reviewRepository.existsByReservation(reservation)) {
            throw new BaseException(ErrorStatus.DUPLICATE_REVIEW);
        }

        // 4. Review 생성
        Review review = Review.builder()
                .reservation(reservation)
                .starReview(reviewRequestDTO.getStarReview())
                .comment(reviewRequestDTO.getComment())
                .build();

        // 5. 경험 평가 매핑
        reviewRequestDTO.getExperienceEvaluations().forEach(e -> {
            ExperienceGroup experienceGroup = experienceGroupRepository.findById(e.getExperienceGroupId())
                    .orElseThrow(() -> new BaseException(ErrorStatus.EXPERIENCE_NOT_FOUND));

            ReviewExperience reviewExperience = ReviewExperience.builder()
                    .experienceGroup(experienceGroup)
                    .isThumbsUp(e.getIsThumbsUp())
                    .build();

            review.addExperienceEvaluation(reviewExperience); // FK 양방향 관계 세팅
        });

        // 7. 저장
        Review saved = reviewRepository.save(review);

        // 8. fetch join 으로 다시 조회 (experienceGroup 포함)
        Review fullyLoaded = reviewRepository.findByIdWithExperiences(saved.getId())
                .orElseThrow(() -> new BaseException(ErrorStatus.REVIEW_NOT_FOUND));

        // 9. DTO 변환
        return ReviewResponseDTO.from(fullyLoaded);
    }

}
