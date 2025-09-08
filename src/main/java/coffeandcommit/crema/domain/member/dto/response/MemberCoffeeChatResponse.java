package coffeandcommit.crema.domain.member.dto.response;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "멤버 커피챗 예약 응답")
public class MemberCoffeeChatResponse {

    @Schema(description = "예약 ID", example = "1")
    private Long reservationId;

    @Schema(description = "가이드 ID", example = "1")
    private Long guideId;

    @Schema(description = "가이드 닉네임", example = "가이드닉네임")
    private String guideNickname;

    @Schema(description = "가이드 회사명", example = "테크스타트업")
    private String guideCompanyName;

    @Schema(description = "가이드 직책", example = "시니어 개발자")
    private String guideJobPosition;

    @Schema(description = "예약 상태", example = "PENDING")
    private Status status;

    @Schema(description = "매칭 시간", example = "2024-12-01T14:00:00")
    private LocalDateTime matchingTime;

    @Schema(description = "예약 시간", example = "2024-11-25T10:30:00")
    private LocalDateTime reservedAt;

    @Schema(description = "시간 타입", example = "MINUTE_30")
    private String timeType;

    @Schema(description = "가격", example = "8000")
    private Integer price;

    @Schema(description = "생성일시", example = "2024-11-25T10:30:00")
    private LocalDateTime createdAt;

    public static MemberCoffeeChatResponse from(Reservation reservation) {
        String timeType = null;
        Integer price = null;

        if (reservation.getTimeUnit() != null && reservation.getTimeUnit().getTimeType() != null) {
            timeType = reservation.getTimeUnit().getTimeType().name();
            price = reservation.getTimeUnit().getTimeType().getPrice();
        }

        return MemberCoffeeChatResponse.builder()
                .reservationId(reservation.getId())
                .guideId(reservation.getGuide().getId())
                .guideNickname(reservation.getGuide().getMember().getNickname())
                .guideCompanyName(reservation.getGuide().getCompanyName())
                .guideJobPosition(reservation.getGuide().getJobPosition())
                .status(reservation.getStatus())
                .matchingTime(reservation.getMatchingTime())
                .reservedAt(reservation.getReservedAt())
                .timeType(timeType)
                .price(price)
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}