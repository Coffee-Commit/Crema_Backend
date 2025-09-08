package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.guide.entity.Guide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuideRepository extends JpaRepository<Guide, Long> {


    Optional<Guide> findByMember_Id(String memberId);

    /**
     * 가이드 목록을 조회합니다.
     * 조건에 따라 필터링된 가이드를 페이지 단위로 반환합니다.
     * 영업 중(isOpened = true)인 가이드만 조회됩니다.
     *
     * @param jobFieldIds 직무분야 ID 목록
     * @param chatTopicIds 커피챗 주제 ID 목록
     * @param keyword 검색 키워드 (title, hashTagName)
     * @param pageable 페이징 정보
     * @return 필터링된 가이드 목록
     */
    @Query(
        value = """
            SELECT DISTINCT g
            FROM Guide g
            LEFT JOIN g.guideJobField gjf
            LEFT JOIN g.guideChatTopics gct
            LEFT JOIN g.hashTags ht
            WHERE g.isOpened = true
              AND (:jobFieldIds IS NULL OR gjf.id IN :jobFieldIds)
              AND (:chatTopicIds IS NULL OR gct.chatTopic.id IN :chatTopicIds)
              AND (:keyword IS NULL
                   OR LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(ht.hashTagName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
        countQuery = """
            SELECT COUNT(DISTINCT g)
            FROM Guide g
            LEFT JOIN g.guideJobField gjf
            LEFT JOIN g.guideChatTopics gct
            LEFT JOIN g.hashTags ht
            WHERE g.isOpened = true
              AND (:jobFieldIds IS NULL OR gjf.id IN :jobFieldIds)
              AND (:chatTopicIds IS NULL OR gct.chatTopic.id IN :chatTopicIds)
              AND (:keyword IS NULL
                   OR LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(ht.hashTagName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """
    )
    Page<Guide> findBySearchConditions(
            @Param("jobFieldIds") List<Long> jobFieldIds,
            @Param("chatTopicIds") List<Long> chatTopicIds,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
