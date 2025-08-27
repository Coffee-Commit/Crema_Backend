package coffeandcommit.crema.domain.member.repository;

import coffeandcommit.crema.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {

    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.isDeleted = false")
    Optional<Member> findByIdAndIsDeletedFalse(@Param("id") String id);

    @Query("SELECT m FROM Member m WHERE m.nickname = :nickname AND m.isDeleted = false")
    Optional<Member> findByNicknameAndIsDeletedFalse(@Param("nickname") String nickname);

    @Query("SELECT m FROM Member m WHERE m.provider = :provider AND m.providerId = :providerId AND m.isDeleted = false")
    Optional<Member> findByProviderAndProviderIdAndIsDeletedFalse(@Param("provider") String provider, @Param("providerId") String providerId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.nickname = :nickname AND m.isDeleted = false")
    boolean existsByNicknameAndIsDeletedFalse(@Param("nickname") String nickname);
}