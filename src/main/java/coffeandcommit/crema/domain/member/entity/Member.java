package coffeandcommit.crema.domain.member.entity;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@SQLRestriction("is_deleted = false") // 회원탈퇴 하지 않은 member만 조회가능하게 설정
@EntityListeners(AuditingEntityListener.class)
@Table(name = "member", indexes = {
        @Index(name = "idx_member_nickname", columnList = "nickname"),
        @Index(name = "idx_member_provider_provider_id", columnList = "provider, provider_id")
},
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_provider", columnNames = {"provider", "provider_id"}), // OAuth 회원가입시 중복 방지
                @UniqueConstraint(name = "uk_member_nickname_is_deleted", columnNames = {"nickname", "is_deleted"}) // 소프트 삭제된 계정의 닉네임을 재사용 가능하게 하되, 활성 계정 간 닉네임 충돌은 금지
        })
public class Member extends BaseEntity {

    @Id
    private String id; // UUID로 멤버 식별자 생성

    @Column(nullable = true, length = 64, unique = true)
    private String nickname;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Column(nullable = true, length = 320) // 이메일 표준 최대 길이
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Integer point = 0;

    @Column(nullable = true, length = 500)
    private String profileImageUrl;

    @Column(nullable = true, length = 1000)
    private String description;

    @Column(nullable = true, length = 20)
    private String provider; // OAuth 로그인 제공자

    @Column(nullable = true, length = 100)
    private String providerId; // OAuth 로그인한 유저의 ID

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Guide guide;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MemberJobField jobField;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemberChatTopic> chatTopics = new ArrayList<>();

    // 프로필 업데이트 (이메일 추가)
    public void updateProfile(String nickname, String description, String profileImageUrl, String email) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (description != null) {
            this.description = description;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (email != null) {
            this.email = email.trim().toLowerCase(); // 소문자로 정규화
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