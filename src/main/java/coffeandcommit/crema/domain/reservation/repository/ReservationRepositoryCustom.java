package coffeandcommit.crema.domain.reservation.repository;

import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.review.dto.response.MyReviewResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservationRepositoryCustom {
    Page<MyReviewResponseDTO> findMyReviews(String memberId, Status status, String filter, Pageable pageable);
}

