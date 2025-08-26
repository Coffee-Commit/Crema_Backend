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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member memberId; // FK, 멤버 ID는 String (UUID)

    @Column()
    private boolean isApproved;

    private String description;

    private boolean isOpened;

    private String title;

    private String certificationImageUrl;

    private LocalDateTime approvedAt;

}
