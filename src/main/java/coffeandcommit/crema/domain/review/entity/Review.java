package coffeandcommit.crema.domain.review.entity;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "review",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_reservation", columnNames = "reservation_id")
        }
)
public class Review extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation; // FK, 예약 ID

    @Column(name = "star_review", precision = 2, scale = 1, nullable = false)
    private BigDecimal starReview; // 별점

    @Column(length = 500)
    private String comment; // 후기 내용

    @Builder.Default
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewExperience> experienceEvaluations = new ArrayList<>();

    public void addExperienceEvaluation(ReviewExperience experience) {
        experience.setReview(this);  // FK 세팅
        this.experienceEvaluations.add(experience);
    }

}
