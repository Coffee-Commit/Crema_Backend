package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.guide.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
}
