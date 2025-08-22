package coffeandcommit.crema.domain.member.repository;

import coffeandcommit.crema.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {

    Optional<Member> findByNickname(String nickname);

    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByNickname(String nickname);
}