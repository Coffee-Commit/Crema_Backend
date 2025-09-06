package coffeandcommit.crema.domain.reservation.repository;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    int countByMember_IdAndStatus(String memberId, Status status);

    List<Reservation> findByMember_IdAndStatus(String memberId, Status status);
}
