package coffeandcommit.crema.domain.member.entity;

import coffeandcommit.crema.domain.member.enums.MemberRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "member")
public class Member {

    @Id
    private String id; // OAuth2 연동을 위해 String 타입 사용

    @Column(nullable = false, length = 255, unique = true)
    private String userId; // 사용자 아이디

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
}