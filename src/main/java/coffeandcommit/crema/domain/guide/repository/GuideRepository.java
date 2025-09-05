package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.guide.entity.Guide;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuideRepository extends JpaRepository<Guide, Long> {

    @EntityGraph(attributePaths = {"member"})
    Optional<Guide> findById(Long id);

    Optional<Guide> findByMember_Id(String memberId);
}
