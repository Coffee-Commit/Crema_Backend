package coffeandcommit.crema.domain.review.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewRequestDTO {

    @NotNull
    private Long reservationId; // 리뷰 대상 예약 ID

    @NotNull
    @Digits(integer = 1, fraction = 1)
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    private BigDecimal starReview; // 별점 (1~5)

    @NotBlank
    @Size(min = 10, max = 500)
    private String comment; // 후기 내용

    @NotEmpty(message = "경험 평가는 최소 1개 이상 입력해야 합니다.")
    @Valid
    private List<@Valid ExperienceEvaluationRequestDTO> experienceEvaluations;

    @AssertTrue(message = "starReview must be in 0.5 increments within [0.0, 5.0]")
    private boolean isValidStarStep() {
        if (starReview == null) return true; // @NotNull이 처리
        return starReview
                .multiply(new BigDecimal("10"))   // 10배
                .remainder(new BigDecimal("5"))   // 5로 나눈 나머지
                .compareTo(BigDecimal.ZERO) == 0; // 0이면 0.5 단위
    }
}
