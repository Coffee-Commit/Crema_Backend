package coffeandcommit.crema.domain.guide.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.QGuide;
import coffeandcommit.crema.domain.guide.entity.QGuideJobField;
import coffeandcommit.crema.domain.guide.entity.QHashTag;
import coffeandcommit.crema.domain.guide.entity.QGuideChatTopic;
import coffeandcommit.crema.domain.globalTag.entity.QChatTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class GuideRepositoryImpl implements GuideRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public GuideRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Guide> findBySearchConditions(List<JobNameType> jobNames, List<TopicNameType> chatTopicNames, String keyword, Pageable pageable) {
        QGuide g = QGuide.guide;
        QGuideJobField gjf = QGuideJobField.guideJobField;
        QHashTag ht = QHashTag.hashTag;
        QGuideChatTopic gct = QGuideChatTopic.guideChatTopic;
        QChatTopic ct = QChatTopic.chatTopic;

        BooleanBuilder where = new BooleanBuilder();
        where.and(g.isOpened.isTrue());

        // 직무 필터: EXISTS 사용으로 메인 쿼리 조인 최소화
        if (jobNames != null && !jobNames.isEmpty()) {
            where.and(
                    JPAExpressions.selectOne()
                            .from(gjf)
                            .where(gjf.guide.eq(g)
                                    .and(gjf.jobName.in(jobNames)))
                            .exists()
            );
        }

        // 주제 필터: EXISTS로 행 증식 방지
        if (chatTopicNames != null && !chatTopicNames.isEmpty()) {
            where.and(
                    JPAExpressions.selectOne()
                            .from(gct)
                            .join(gct.chatTopic, ct)
                            .where(gct.guide.eq(g)
                                    .and(ct.topicName.in(chatTopicNames)))
                            .exists()
            );
        }

        // 키워드: 접두어 + EXISTS (태그)
        // 주의: MySQL 인덱스 활용을 위해 컬럼에 lower() 미사용 (DB 콜레이션으로 대/소문자 처리)
        if (keyword != null && !keyword.isBlank()) {
            String likePrefix = keyword.trim() + "%";
            where.and(
                    g.title.like(likePrefix)
                            .or(
                                    JPAExpressions.selectOne()
                                            .from(ht)
                                            .where(ht.guide.eq(g)
                                                    .and(ht.hashTagName.like(likePrefix)))
                                            .exists()
                            )
            );
        }

        var contentQuery = queryFactory
                .select(g)
                .from(g)
                .where(where);

        // 정렬 적용 (허용 필드만)
        for (OrderSpecifier<?> os : toOrderSpecifiers(pageable.getSort(), g)) {
            contentQuery.orderBy(os);
        }

        List<Guide> content = pageable.isUnpaged()
                ? contentQuery.fetch()
                : contentQuery.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();

        Long total = queryFactory
                .select(g.id.count())
                .from(g)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    // 허용된 정렬 키만 매핑
    private List<OrderSpecifier<?>> toOrderSpecifiers(Sort sort, QGuide g) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        if (sort == null) return orders;

        for (Sort.Order o : sort) {
            ComparableExpressionBase<?> expr = switch (o.getProperty()) {
                case "modifiedAt" -> g.modifiedAt;
                case "createdAt" -> g.createdAt;
                case "title" -> g.title;
                default -> null;
            };
            if (expr != null) {
                orders.add(new OrderSpecifier<>(o.isAscending() ? Order.ASC : Order.DESC, expr));
            }
        }
        return orders;
    }
}

