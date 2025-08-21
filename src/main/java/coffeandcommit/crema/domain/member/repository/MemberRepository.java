package coffeandcommit.crema.domain.member.repository;

import coffeandcommit.crema.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {

    Optional<Member> findByUserId(String userId);

    Optional<Member> findByNickname(String nickname);

    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByUserId(String userId);

    boolean existsByNickname(String nickname);
}