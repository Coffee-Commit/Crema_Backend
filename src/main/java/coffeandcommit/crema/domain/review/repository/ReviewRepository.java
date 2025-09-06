package coffeandcommit.crema.domain.review.repository;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByReservation(Reservation reservation);

    @Query("""
        select distinct r from Review r
        join fetch r.reservation res
        left join fetch r.experienceEvaluations re
        left join fetch re.experienceGroup eg
        where r.id = :id
    """)
    Optional<Review> findByIdWithExperiences(@Param("id") Long id);

    List<Review> findByReservationIdIn(Collection<Long> reservationIds);

    @Query("SELECT COALESCE(AVG(r.starReview), 0.0) FROM Review r WHERE r.reservation.guide.id = :guideId")
    Double getAverageScoreByGuideId(@Param("guideId") Long guideId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reservation.guide.id = :guideId")
    Long countByGuideId(@Param("guideId") Long guideId);
}
