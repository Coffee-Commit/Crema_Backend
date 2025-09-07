package coffeandcommit.crema.domain.guide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoffeeChatStatsResponseDTO {

    private Long totalCoffeeChats;   // 총 커피챗 완료 횟수
    private Double averageStar;      // 평균 별점
    private Long totalReviews;       // 총 리뷰 개수
    private Long thumbsUpCount;      // 도움 됐어요(따봉) 수

    public static CoffeeChatStatsResponseDTO from(Long totalCoffeeChats, Double averageStar, Long totalReviews, Long thumbsUpCount) {
        return CoffeeChatStatsResponseDTO.builder()
                .totalCoffeeChats(totalCoffeeChats)
                .averageStar(averageStar != null ? averageStar : 0.0) // null 방어 처리
                .totalReviews(totalReviews)
                .thumbsUpCount(thumbsUpCount)
                .build();
    }
}
