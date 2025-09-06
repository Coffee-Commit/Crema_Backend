package coffeandcommit.crema.domain.reservation.repository;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    int countByMember_IdAndStatus(String memberId, Status status);

    @EntityGraph(attributePaths = {"guide", "guide.member", "timeUnit"})
    Page<Reservation> findByMember_IdAndStatus(String memberId, Status status, Pageable pageable);
}
