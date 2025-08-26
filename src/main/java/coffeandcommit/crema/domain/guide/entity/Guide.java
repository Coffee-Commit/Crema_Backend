package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "guide")
public class Guide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member memberId; // FK, 멤버 ID는 String (UUID)

    @Column(nullable = false)
    private boolean isApproved;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean isOpened;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2048)
    private String certificationImageUrl;

    private LocalDateTime approvedAt;

}
