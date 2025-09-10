package coffeandcommit.crema.domain.reservation.repository;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.TimeUnit;
import coffeandcommit.crema.domain.guide.enums.TimeType;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.review.dto.response.GuideInfo;
import coffeandcommit.crema.domain.review.dto.response.MyReviewResponseDTO;
import coffeandcommit.crema.domain.review.dto.response.ReservationInfo;
import coffeandcommit.crema.domain.review.dto.response.ReviewInfo;
import coffeandcommit.crema.domain.review.entity.Review;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static coffeandcommit.crema.domain.guide.entity.QTimeUnit.timeUnit;
import static coffeandcommit.crema.domain.member.entity.QMember.member;
import static coffeandcommit.crema.domain.reservation.entity.QReservation.reservation;
import static coffeandcommit.crema.domain.review.entity.QReview.review;
import static coffeandcommit.crema.domain.guide.entity.QGuide.guide;

public class ReservationRepositoryImpl implements ReservationRepositoryCustom {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public ReservationRepositoryImpl(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<MyReviewResponseDTO> findMyReviews(String memberId, Status status, String filter, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder()
                .and(reservation.member.id.eq(memberId))
                .and(reservation.status.eq(status));

        if ("WRITTEN".equalsIgnoreCase(filter)) {
            where.and(review.id.isNotNull());
        } else if ("NOT_WRITTEN".equalsIgnoreCase(filter)) {
            where.and(review.id.isNull());
        }

        List<Tuple> rows = queryFactory
                .select(
                        reservation.id,
                        member.nickname,
                        member.profileImageUrl,
                        reservation.matchingTime,
                        timeUnit.timeType,
                        review.id,
                        review.comment,
                        review.starReview,
                        review.createdAt
                )
                .from(reservation)
                .leftJoin(review).on(review.reservation.eq(reservation))
                .leftJoin(reservation.guide, guide)
                .leftJoin(guide.member, member)
                .leftJoin(reservation.timeUnit, timeUnit)
                .where(where)
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<MyReviewResponseDTO> content = new ArrayList<>(rows.size());
        for (Tuple t : rows) {
            Long reservationId = t.get(reservation.id);
            String nickname = t.get(member.nickname);
            String profileImageUrl = t.get(member.profileImageUrl);
            LocalDateTime matchingTime = t.get(reservation.matchingTime);
            TimeType timeType = t.get(timeUnit.timeType);

            Long reviewId = t.get(review.id);
            String comment = t.get(review.comment);
            BigDecimal star = t.get(review.starReview);
            LocalDateTime createdAt = t.get(review.createdAt);

            GuideInfo guideInfo = GuideInfo.builder()
                    .nickname(nickname)
                    .profileImageUrl(profileImageUrl)
                    .build();

            ReservationInfo reservationInfo = ReservationInfo.builder()
                    .matchingDateTime(matchingTime)
                    .timeUnit(timeType)
                    .build();

            ReviewInfo reviewInfo = null;
            if (reviewId != null) {
                reviewInfo = ReviewInfo.builder()
                        .reviewId(reviewId)
                        .comment(comment != null ? comment : "")
                        .star(star != null ? star.doubleValue() : null)
                        .createdAt(createdAt)
                        .build();
            }

            content.add(MyReviewResponseDTO.builder()
                    .reservationId(reservationId)
                    .guide(guideInfo)
                    .reservation(reservationInfo)
                    .review(reviewInfo)
                    .build());
        }

        Long total = queryFactory
                .select(reservation.count())
                .from(reservation)
                .leftJoin(review).on(review.reservation.eq(reservation))
                .where(where)
                .fetchOne();

        long totalCount = total != null ? total : 0L;
        return new PageImpl<>(content, pageable, totalCount);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new OrderSpecifier[]{reservation.id.desc()};
        }
        PathBuilder<Reservation> entityPath = new PathBuilder<>(Reservation.class, reservation.getMetadata());
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order o : sort) {
            try {
                Order direction = o.isAscending() ? Order.ASC : Order.DESC;
                orders.add(new OrderSpecifier<>(direction, entityPath.getComparable(o.getProperty(), Comparable.class)));
            } catch (IllegalArgumentException e) {
                // 무효한 정렬 필드는 무시하고 기본 정렬 사용
            }
        }
        if (orders.isEmpty()) {
            return new OrderSpecifier[]{reservation.id.desc()};
        }
        return orders.toArray(new OrderSpecifier<?>[0]);
    }
}
