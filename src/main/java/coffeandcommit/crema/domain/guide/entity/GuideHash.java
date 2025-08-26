package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.global.common.entitiy.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "guide_hash")
public class GuideHash extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guideId; // FK, 가이드 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hash_tag_id", nullable = false)
    private HashTag hashTagId; // 해시태그
}
