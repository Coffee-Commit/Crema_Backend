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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id; // OAuth2 연동을 위해 String 타입 사용

    @Column(nullable = false, length = 255, unique = true)
    private String username;

    @Column(nullable = true, length = 60)
    private String password;

    @Column(nullable = false, length = 32)
    private String realName;

    @Column(nullable = true, length = 32)
    private String nickname;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Column(nullable = true, length = 13)
    private String phoneNumber;

    @Column(nullable = false)
    private Integer point;

    @Column(nullable = true)
    private String profileImageUrl;

    @Column(nullable = true)
    private String description;

    @Column(nullable = true)
    private String provider; // OAuth 로그인 제공자 (ex: google, kakao)

    @Column(nullable = true)
    private String providerId; // Oauth 로그인한 유저의 ID

    public void updateProfile(String nickname, String description, String profileImageUrl) {
        this.nickname = nickname;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
    }

    public void addPoint (int point) {
        this.point += point;
    }

    public void decreasePoint (int point) {
        if (this.point >= point) {
            this.point -= point;
        } else {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
    }
}