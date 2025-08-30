package coffeandcommit.crema.domain.review.entity;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "review")
public class Review extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation; // FK, 예약 ID

    @Column(nullable = false)
    private float starReview; // 평점

    @Column(length = 500)
    private String content; // 후기 내용


}
