package coffeandcommit.crema.domain.review.repository;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByReservation(Reservation reservation);
}
