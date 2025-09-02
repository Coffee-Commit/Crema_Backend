package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "hash_tag",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"guide_id", "hash_tag_name"})
    },
        indexes = {
            @Index(name = "idx_hash_tag_guide", columnList = "guide_id"),
            @Index(name = "idx_hash_tag_name", columnList = "hash_tag_name")
    }
)
public class HashTag extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide;

    @Column(name = "hash_tag_name", length = 24)
    private String hashTagName; // 해시태그 이름
}
