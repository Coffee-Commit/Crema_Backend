package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.guide.entity.Guide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Guide 목록 조회 시 함께 가져올 집계 지표 묶음.
 * - 성능 개선: 가이드당 N회 쿼리 대신 단일 쿼리에서 합산/평균을 계산해 전달한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class GuideWithStats {
    private final Guide guide;
    private final long totalReviews;
    private final Double averageStar; // 소수점 한 자리 반올림은 상위 서비스에서 처리
    private final long totalCoffeeChats; // COMPLETED 상태 예약 수
    private final long thumbsUpCount; // ReviewExperience 의 isThumbsUp=true 개수
}

