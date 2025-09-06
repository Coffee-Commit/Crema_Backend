package coffeandcommit.crema.domain.review.dto.response;

import coffeandcommit.crema.domain.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewInfo {

    private Long reviewId;            // 리뷰 PK (작성 전이면 없음)
    private String comment;           // 리뷰 코멘트 (최대 100자)
    private Double star;             // 리뷰 별점 (1~5)
    private LocalDateTime createdAt;  // 리뷰 작성일

    public static ReviewInfo from(Review review) {
        if (review == null) {
            return null;
        }
        return ReviewInfo.builder()
                .reviewId(review.getId())
                .comment(review.getComment() != null ? review.getComment() : "")
                .star(review.getStarReview() != null ? review.getStarReview().doubleValue() : null)
                .createdAt(review.getCreatedAt())
                .build();
    }
}
