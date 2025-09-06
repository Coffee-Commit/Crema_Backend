package coffeandcommit.crema.domain.reservation.repository;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    int countByMember_IdAndStatus(String memberId, Status status);

    @EntityGraph(attributePaths = {"guide", "guide.member", "timeUnit"})
    Page<Reservation> findByMember_IdAndStatus(String memberId, Status status, Pageable pageable);

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
}
