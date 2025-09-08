package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.guide.entity.Guide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GuideRepositoryCustom {

    Page<Guide> findBySearchConditions(
            List<Long> jobFieldIds,
            List<Long> chatTopicIds,
            String keyword,
            Pageable pageable
    );

    Page<Guide> findBySearchConditionsOrderByReviewCount(
            List<Long> jobFieldIds,
            List<Long> chatTopicIds,
            String keyword,
            Pageable pageable
    );

    /**
     * 필터 조건과 함께 집계 지표(리뷰수, 평균별점, 완료된 커피챗 수, 좋아요 수)를
     * 단일 쿼리에서 계산하여 반환합니다.
     * orderByReviewCount=true 면 인기순(리뷰수 DESC), 아니면 최신순(기본 pageable sort 적용)으로 정렬합니다.
     */
    Page<GuideWithStats> findBySearchConditionsWithStats(
            List<Long> jobFieldIds,
            List<Long> chatTopicIds,
            String keyword,
            Pageable pageable,
            boolean orderByReviewCount
    );
}
