package coffeandcommit.crema.domain.review.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewRequestDTO {

    @NotNull
    private Long reservationId; // 리뷰 대상 예약 ID

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    private float starReview; // 별점 (1~5)

    @NotBlank
    @Size(min = 10, max = 500)
    private String comment; // 후기 내용

    @Valid
    private List<@Valid ExperienceEvaluationRequestDTO> experienceEvaluations;
}
