package coffeandcommit.crema.domain.reservation.repository;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepositoryCustom {
    int countByMember_IdAndStatus(String memberId, Status status);

    @EntityGraph(attributePaths = {"guide", "guide.member", "timeUnit"})
    Page<Reservation> findByMember_IdAndStatus(String memberId, Status status, Pageable pageable);

    // 멤버의 모든 예약 조회
    @EntityGraph(attributePaths = {"guide", "guide.member", "timeUnit"})
    List<Reservation> findByMember_Id(String memberId);

    // 기존 메서드는 Page를 반환하므로 List 버전도 추가
    @EntityGraph(attributePaths = {"guide", "guide.member", "timeUnit"})
    List<Reservation> findByMember_IdAndStatus(String memberId, Status status);

    // WRITTEN → 리뷰가 존재하는 예약
    @EntityGraph(attributePaths = {"guide", "guide.member", "timeUnit"})
    @Query("""
        select r from Reservation r
        where r.member.id = :memberId
          and r.status = :status
          and exists (select 1 from Review rv where rv.reservation = r)
    """)
    Page<Reservation> findWrittenByMember(
            @Param("memberId") String memberId,
            @Param("status") Status status,
            Pageable pageable);

    // NOT_WRITTEN → 리뷰가 존재하지 않는 예약
    @EntityGraph(attributePaths = {"guide", "guide.member", "timeUnit"})
    @Query("""
        select r from Reservation r
        where r.member.id = :memberId
          and r.status = :status
          and not exists (select 1 from Review rv where rv.reservation = r)
    """)
    Page<Reservation> findNotWrittenByMember(
            @Param("memberId") String memberId,
            @Param("status") Status status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"member", "survey", "timeUnit"})
    List<Reservation> findByGuideAndStatus(Guide guide, Status status);

    @EntityGraph(attributePaths = {"member", "timeUnit", "timeUnit.timeType"})
    Page<Reservation> findByGuideAndMatchingTimeBetweenAndStatusIn(Guide guide, LocalDateTime matchingTimeAfter, LocalDateTime matchingTimeBefore, Collection<Status> statuses, Pageable pageable);

    Long countByGuideAndStatus(Guide guide, Status status);

    /**
     * 멤버의 모든 예약 조회 (필요한 연관 엔티티들 fetch join)
     * MemberCoffeeChatResponse에서 사용하는 모든 연관관계 포함
     */
    @EntityGraph(attributePaths = {
            "guide",
            "guide.member",           // 가이드 프로필 이미지용
            "survey",                 // 희망 날짜/시간용
            "timeUnit",               // 시간 타입용
            "videoSession"            // 화상채팅 세션용 (optional)
    })
    @Query("""
    SELECT r FROM Reservation r
    WHERE r.member.id = :memberId
    ORDER BY r.createdAt DESC
""")
    List<Reservation> findByMemberIdWithFetchJoin(@Param("memberId") String memberId);

    /**
     * 멤버의 상태별 예약 조회 (필요한 연관 엔티티들 fetch join)
     */
    @EntityGraph(attributePaths = {
            "guide",
            "guide.member",           // 가이드 프로필 이미지용
            "survey",                 // 희망 날짜/시간용
            "timeUnit",               // 시간 타입용
            "videoSession"            // 화상채팅 세션용 (optional)
    })
    @Query("""
    SELECT r FROM Reservation r
    WHERE r.member.id = :memberId
    AND r.status = :status
    ORDER BY r.createdAt DESC
""")
    List<Reservation> findByMemberIdAndStatusWithFetchJoin(
            @Param("memberId") String memberId,
            @Param("status") Status status
    );

    Page<Reservation> findByGuide(Guide guide, Pageable pageable);

    /**
     * 가이드의 모든 예약 조회 (연관 엔티티 fetch join)
     */
    @EntityGraph(attributePaths = {
            "member",
            "survey",
            "timeUnit",
            "videoSession"
    })
    @Query("""
    SELECT r FROM Reservation r
    WHERE r.guide = :guide
    ORDER BY r.createdAt DESC
    """)
    List<Reservation> findByGuideWithFetchJoin(@Param("guide") Guide guide);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);
}
