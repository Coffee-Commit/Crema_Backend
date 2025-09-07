package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.review.dto.response.ReviewInfo;
import coffeandcommit.crema.domain.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideReviewResponseDTO {

    private ReviewInfo review;   // 리뷰 정보
    private MemberInfo writer;   // 작성자 정보

    public static GuideReviewResponseDTO from(Review review, Member member) {
        return GuideReviewResponseDTO.builder()
                .review(ReviewInfo.from(review))
                .writer(MemberInfo.from(member))
                .build();
    }
}
