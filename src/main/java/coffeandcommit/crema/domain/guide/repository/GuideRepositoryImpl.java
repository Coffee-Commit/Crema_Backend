package coffeandcommit.crema.domain.guide.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.QGuide;
import coffeandcommit.crema.domain.guide.entity.QGuideChatTopic;
import coffeandcommit.crema.domain.guide.entity.QGuideJobField;
import coffeandcommit.crema.domain.guide.entity.QHashTag;
import coffeandcommit.crema.domain.reservation.entity.QReservation;
import coffeandcommit.crema.domain.review.entity.QReview;
import coffeandcommit.crema.domain.review.entity.QReviewExperience;
import coffeandcommit.crema.domain.guide.entity.QExperienceGroup;
import coffeandcommit.crema.domain.reservation.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class GuideRepositoryImpl implements GuideRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public GuideRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Guide> findBySearchConditions(List<Long> jobFieldIds, List<Long> chatTopicIds, String keyword, Pageable pageable) {
        QGuide g = QGuide.guide;
        QGuideJobField gjf = QGuideJobField.guideJobField;
        QGuideChatTopic gct = QGuideChatTopic.guideChatTopic;
        QHashTag ht = QHashTag.hashTag;

        BooleanBuilder where = new BooleanBuilder();
        where.and(g.isOpened.isTrue());

        if (jobFieldIds != null && !jobFieldIds.isEmpty()) {
            // Guide has one GuideJobField
            where.and(g.guideJobField.id.in(jobFieldIds));
        }

        if (chatTopicIds != null && !chatTopicIds.isEmpty()) {
            where.and(gct.chatTopic.id.in(chatTopicIds));
        }

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase() + "%";
            where.and(
                    g.title.lower().like(like)
                            .or(ht.hashTagName.lower().like(like))
            );
        }

        // Base content query
        var contentQuery = queryFactory
                .selectDistinct(g)
                .from(g)
                .leftJoin(g.guideJobField, gjf)
                .leftJoin(g.guideChatTopics, gct)
                .leftJoin(g.hashTags, ht)
                .where(where);

        // Apply sorting from pageable
        for (OrderSpecifier<?> orderSpecifier : toOrderSpecifiers(pageable.getSort(), g)) {
            contentQuery.orderBy(orderSpecifier);
        }

        List<Guide> content;
        long total;

        if (pageable.isUnpaged()) {
            content = contentQuery.fetch();
            // Count matches distinct guides
            total = queryFactory
                    .select(g.id.countDistinct())
                    .from(g)
                    .leftJoin(g.guideJobField, gjf)
                    .leftJoin(g.guideChatTopics, gct)
                    .leftJoin(g.hashTags, ht)
                    .where(where)
                    .fetchFirst();
        } else {
            content = contentQuery
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();

            total = Objects.requireNonNullElse(
                    queryFactory
                            .select(g.id.countDistinct())
                            .from(g)
                            .leftJoin(g.guideJobField, gjf)
                            .leftJoin(g.guideChatTopics, gct)
                            .leftJoin(g.hashTags, ht)
                            .where(where)
                            .fetchFirst(),
                    0L
            );
        }

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<Guide> findBySearchConditionsOrderByReviewCount(List<Long> jobFieldIds, List<Long> chatTopicIds, String keyword, Pageable pageable) {
        QGuide g = QGuide.guide;
        QGuideJobField gjf = QGuideJobField.guideJobField;
        QGuideChatTopic gct = QGuideChatTopic.guideChatTopic;
        QHashTag ht = QHashTag.hashTag;
        QReservation res = QReservation.reservation;
        QReview rev = QReview.review;

        BooleanBuilder where = new BooleanBuilder();
        where.and(g.isOpened.isTrue());

        if (jobFieldIds != null && !jobFieldIds.isEmpty()) {
            where.and(g.guideJobField.id.in(jobFieldIds));
        }

        if (chatTopicIds != null && !chatTopicIds.isEmpty()) {
            where.and(gct.chatTopic.id.in(chatTopicIds));
        }

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase() + "%";
            where.and(
                    g.title.lower().like(like)
                            .or(ht.hashTagName.lower().like(like))
            );
        }

        NumberExpression<Long> reviewCount = rev.id.countDistinct();

        var baseQuery = queryFactory
                .select(g)
                .from(g)
                .leftJoin(g.guideJobField, gjf)
                .leftJoin(g.guideChatTopics, gct)
                .leftJoin(g.hashTags, ht)
                .leftJoin(res).on(res.guide.eq(g))
                .leftJoin(rev).on(rev.reservation.eq(res))
                .where(where)
                .groupBy(g.id)
                .orderBy(reviewCount.desc(), g.modifiedAt.desc());

        List<Guide> content;
        long total;

        if (pageable.isUnpaged()) {
            content = baseQuery.fetch();
        } else {
            content = baseQuery
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();
        }

        total = Objects.requireNonNullElse(
                queryFactory
                        .select(g.id.countDistinct())
                        .from(g)
                        .leftJoin(g.guideJobField, gjf)
                        .leftJoin(g.guideChatTopics, gct)
                        .leftJoin(g.hashTags, ht)
                        .where(where)
                        .fetchFirst(),
                0L
        );

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<GuideWithStats> findBySearchConditionsWithStats(List<Long> jobFieldIds, List<Long> chatTopicIds, String keyword, Pageable pageable, boolean orderByReviewCount) {
        QGuide g = QGuide.guide;
        QGuideJobField gjf = QGuideJobField.guideJobField;
        QGuideChatTopic gct = QGuideChatTopic.guideChatTopic;
        QHashTag ht = QHashTag.hashTag;
        QReservation res = QReservation.reservation;
        QReview rev = QReview.review;
        QReviewExperience rex = QReviewExperience.reviewExperience;
        QExperienceGroup eg = QExperienceGroup.experienceGroup;

        BooleanBuilder where = new BooleanBuilder();
        where.and(g.isOpened.isTrue());

        if (jobFieldIds != null && !jobFieldIds.isEmpty()) {
            where.and(g.guideJobField.id.in(jobFieldIds));
        }

        if (chatTopicIds != null && !chatTopicIds.isEmpty()) {
            where.and(gct.chatTopic.id.in(chatTopicIds));
        }

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase() + "%";
            where.and(
                    g.title.lower().like(like)
                            .or(ht.hashTagName.lower().like(like))
            );
        }

        // 리뷰 수 (서브쿼리): 가이드에 귀속된 리뷰 개수
        var reviewCount = com.querydsl.jpa.JPAExpressions
                .select(rev.id.count())
                .from(rev)
                .join(rev.reservation, res)
                .where(res.guide.eq(g));

        // 평균 별점 (서브쿼리): 가이드 리뷰의 평균 별점
        var avgStar = com.querydsl.jpa.JPAExpressions
                .select(rev.starReview.avg())
                .from(rev)
                .join(rev.reservation, res)
                .where(res.guide.eq(g));

        // 완료된 커피챗 수 (서브쿼리): COMPLETED 상태 예약 수
        var completedChats = com.querydsl.jpa.JPAExpressions
                .select(res.id.countDistinct())
                .from(res)
                .where(res.guide.eq(g).and(res.status.eq(Status.COMPLETED)));

        // 좋아요 수 (서브쿼리): 해당 가이드의 경험 그룹에 달린 thumbs up 개수
        var thumbsUpCount = com.querydsl.jpa.JPAExpressions
                .select(rex.id.count())
                .from(rex)
                .join(rex.experienceGroup, eg)
                .where(eg.guide.eq(g).and(rex.isThumbsUp.isTrue()));

        // content query: 필터를 위한 조인만 하고, 집계는 서브쿼리로 안전하게 계산
        var contentQuery = queryFactory
                .select(g, reviewCount, avgStar, completedChats, thumbsUpCount)
                .from(g)
                .leftJoin(g.guideJobField, gjf)
                .leftJoin(g.guideChatTopics, gct)
                .leftJoin(g.hashTags, ht)
                .where(where)
                .groupBy(g.id);

        // 정렬: 인기순이면 리뷰 수 DESC 우선, 아니면 pageable 정렬(기본 modifiedAt DESC)
        if (orderByReviewCount) {
            // 서브쿼리는 desc()가 없으므로 명시적으로 OrderSpecifier 사용
            contentQuery.orderBy(new OrderSpecifier<>(Order.DESC, reviewCount), g.modifiedAt.desc());
        } else {
            for (OrderSpecifier<?> orderSpecifier : toOrderSpecifiers(pageable.getSort(), g)) {
                contentQuery.orderBy(orderSpecifier);
            }
        }

        var tuples = pageable.isUnpaged()
                ? contentQuery.fetch()
                : contentQuery.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();

        List<GuideWithStats> content = new java.util.ArrayList<>(tuples.size());
        tuples.forEach(t -> {
            Guide guide = t.get(0, Guide.class);
            // Hibernate/Dialect에 따라 COUNT/AVG 타입이 BigInteger/Long/BigDecimal/Double 등으로 돌아올 수 있어 Number로 안전 처리
            Number rcNum = t.get(1, Number.class); // reviewCount
            Number asNum = t.get(2, Number.class); // avgStar
            Number ccNum = t.get(3, Number.class); // completedChats
            Number tuNum = t.get(4, Number.class); // thumbsUpCount

            long rc = rcNum == null ? 0L : rcNum.longValue();
            long cc = ccNum == null ? 0L : ccNum.longValue();
            long tu = tuNum == null ? 0L : tuNum.longValue();
            double avg = asNum == null ? 0.0 : asNum.doubleValue();

            content.add(GuideWithStats.builder()
                    .guide(guide)
                    .totalReviews(rc)
                    .averageStar(avg)
                    .totalCoffeeChats(cc)
                    .thumbsUpCount(tu)
                    .build());
        });

        long total = Objects.requireNonNullElse(
                queryFactory
                        .select(g.id.countDistinct())
                        .from(g)
                        .leftJoin(g.guideJobField, gjf)
                        .leftJoin(g.guideChatTopics, gct)
                        .leftJoin(g.hashTags, ht)
                        .where(where)
                        .fetchFirst(),
                0L
        );

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 안전한 정렬 매핑: 허용된 속성만 Q타입으로 매핑한다.
     * - PathBuilder.get(property) 사용 시 존재하지 않는 프로퍼티/타입 불일치로 런타임 오류가 날 수 있어
     *   명시적으로 필요한 필드만 처리한다.
     */
    private List<OrderSpecifier<?>> toOrderSpecifiers(Sort sort, QGuide g) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        if (sort == null) return orders;

        for (Sort.Order o : sort) {
            ComparableExpressionBase<?> expr = switch (o.getProperty()) {
                case "modifiedAt" -> g.modifiedAt;
                case "createdAt" -> g.createdAt;
                case "title" -> g.title;
                case "approvedDate" -> g.approvedDate;
                case "companyName" -> g.companyName;
                default -> null; // 알 수 없는 정렬 키는 무시
            };

            if (expr != null) {
                Order direction = o.isAscending() ? Order.ASC : Order.DESC;
                orders.add(new OrderSpecifier<>(direction, expr));
            }
        }
        return orders;
    }
}
