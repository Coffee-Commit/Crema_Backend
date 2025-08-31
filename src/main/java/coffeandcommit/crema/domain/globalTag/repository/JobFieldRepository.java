package coffeandcommit.crema.domain.globalTag.repository;

import coffeandcommit.crema.domain.globalTag.entity.JobField;
import coffeandcommit.crema.domain.globalTag.enums.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobFieldRepository extends JpaRepository<JobField, Long> {
    Optional<JobField> findByJobType(JobType jobType);
}
