package coffeandcommit.crema.domain.review.dto.response;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyReviewResponseDTO {

    private Long reservationId;          // 예약 ID (항상 존재)

    private GuideInfo guide;             // 가이드 정보
    private ReservationInfo reservation; // 예약 정보
    private ReviewInfo review;           // 리뷰 정보 (없으면 null)

    public static MyReviewResponseDTO from(Reservation reservation, Review review) {
        return MyReviewResponseDTO.builder()
                .reservationId(reservation.getId())
                .guide(GuideInfo.from(reservation.getGuide()))
                .reservation(ReservationInfo.from(reservation))
                .review(ReviewInfo.from(review))
                .build();
    }
}
