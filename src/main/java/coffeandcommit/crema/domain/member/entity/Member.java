package coffeandcommit.crema.domain.member.entity;

import coffeandcommit.crema.domain.member.enums.MemberRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLRestriction("is_deleted = false") // 회원탈퇴 하지 않은 member만 조회가능하게 설정
@EntityListeners(AuditingEntityListener.class)
@Table(name = "member", indexes = {
        @Index(name = "idx_member_nickname", columnList = "nickname")
})
public class Member {

    @Id
    private String id; // OAuth2 연동을 위해 String 타입 사용 (UUID)

    @Column(nullable = true, length = 32, unique = true)
    private String nickname;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Column(nullable = true, length = 13)
    private String phoneNumber;

    @Column(nullable = false)
    private Integer point;

    @Column(nullable = true, length = 500)
    private String profileImageUrl;

    @Column(nullable = true, length = 1000)
    private String description;

    @Column(nullable = true, length = 20)
    private String provider; // OAuth 로그인 제공자

    @Column(nullable = true, length = 100)
    private String providerId; // OAuth 로그인한 유저의 ID

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 프로필 업데이트 메서드
    public void updateProfile(String nickname, String description, String profileImageUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (description != null) {
            this.description = description;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    // 포인트 추가
    public void addPoint (int point) {
        if (point < 0) {
            throw new IllegalArgumentException("추가할 포인트는 0 이상이어야 합니다.");
        }
        this.point += point;
    }

    // 포인트 차감
    public void decreasePoint (int point) {
        if (point < 0) {
            throw new IllegalArgumentException("차감할 포인트는 0 이상이어야 합니다.");
        }
        if (this.point < point) {
            throw new IllegalArgumentException("포인트가 부족합니다. 현재 포인트: " + this.point);
        }
        this.point -= point;
    }

    // 포인트 설정 (초기화 등에 사용)
    public void setPoint (int point) {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        }
        this.point = point;
    }

    // 소프트 삭제 메서드
    public void softDelete() {
        this.isDeleted = true;
    }
}