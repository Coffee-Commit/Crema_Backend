package coffeandcommit.crema.domain.review.dto.response;

import coffeandcommit.crema.domain.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDTO {

    private Long reviewId;         // 리뷰 ID
    private Long reservationId;    // 예약 ID
    private BigDecimal starReview;        // 별점
    private String comment;        // 후기 내용
    private LocalDateTime createdAt; // 작성 시간

    private List<ExperienceEvaluationResponseDTO> experienceEvaluations; // 경험 평가 리스트

    public static ReviewResponseDTO from(Review review) {
        return ReviewResponseDTO.builder()
                .reviewId(review.getId())
                .reservationId(review.getReservation().getId())
                .starReview(review.getStarReview())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .experienceEvaluations(
                        review.getExperienceEvaluations().stream()
                                .map(ExperienceEvaluationResponseDTO::from)
                                .collect(Collectors.toList())
                )
                .build();
    }
}
