package coffeandcommit.crema.domain.member.repository;

import coffeandcommit.crema.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {

    Optional<Member> findByUsername(String username);

    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);
}