package coffeandcommit.crema.domain.member.dto.response;

import coffeandcommit.crema.domain.guide.enums.DayType;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.review.dto.response.GuideInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "멤버 커피챗 예약 응답")
public class MemberCoffeeChatResponse {

    @Schema(description = "예약 ID", example = "1")
    private Long reservationId;

    @Schema(description = "가이드 정보")
    private GuideInfo guide;

    @Schema(description = "신청일자 (ISO String)", example = "2025-08-15T14:01:00")
    private String createdAt;

    @Schema(description = "희망 날짜 (yyyy-MM-dd)", example = "2025-08-26")
    private String preferredDateOnly;

    @Schema(description = "희망 요일", example = "화")
    private String preferredDayOfWeek;

    @Schema(description = "희망 시간 범위 (HH:mm~HH:mm)", example = "19:00~19:30")
    private String preferredTimeRange;

    @Schema(description = "예약 상태", example = "PENDING")
    private Status status;

    @Schema(description = "화상채팅방 세션 ID", example = "session_reservation_1_2025-08-26T19:00:00")
    private String videoSessionId;

    @Schema(description = "시간 타입", example = "MINUTE_30")
    private String timeType;

    public static MemberCoffeeChatResponse from(Reservation reservation) {
        // 가이드 정보 추출 (기존 GuideInfo 클래스 재사용)
        GuideInfo guideInfo = GuideInfo.from(reservation.getGuide());

        // 신청일자 (ISO String) - 가이드 쪽과 동일
        String createdAt = reservation.getCreatedAt() != null
                ? reservation.getCreatedAt().toString()
                : null;

        // 시간 타입
        String timeType = null;
        if (reservation.getTimeUnit() != null && reservation.getTimeUnit().getTimeType() != null) {
            timeType = reservation.getTimeUnit().getTimeType().name();
        }

        // 날짜/시간 정보 추출 - 가이드 쪽 로직과 동일
        String preferredDateOnly = null;
        String preferredDayOfWeek = null;
        String preferredTimeRange = null;

        if (reservation.getSurvey() != null && reservation.getSurvey().getPreferredDate() != null) {
            LocalDateTime preferredDateTime = reservation.getSurvey().getPreferredDate();

            // 날짜: yyyy-MM-dd
            preferredDateOnly = preferredDateTime.toLocalDate().toString();

            // 요일: DayType enum의 description 사용 (월, 화, 수, ...)
            DayType dayType = convertToDayType(preferredDateTime.getDayOfWeek());
            preferredDayOfWeek = dayType.getDescription();

            // 시간 범위: HH:mm~HH:mm
            if (reservation.getTimeUnit() != null && reservation.getTimeUnit().getTimeType() != null) {
                int durationMinutes = reservation.getTimeUnit().getTimeType().getMinutes();
                LocalDateTime endDateTime = preferredDateTime.plusMinutes(durationMinutes);
                preferredTimeRange = preferredDateTime.toLocalTime().toString() + "~" + endDateTime.toLocalTime().toString();
            }
        }

        // 화상채팅방은 reservationId로 quick-join API 사용하므로 별도 필드 불필요

        return MemberCoffeeChatResponse.builder()
                .reservationId(reservation.getId())
                .guide(guideInfo)
                .createdAt(createdAt)
                .preferredDateOnly(preferredDateOnly)
                .preferredDayOfWeek(preferredDayOfWeek)
                .preferredTimeRange(preferredTimeRange)
                .status(reservation.getStatus())
                .timeType(timeType)
                .build();
    }

    /**
     * DayOfWeek를 DayType enum으로 변환 (가이드 쪽 로직과 동일)
     */
    private static DayType convertToDayType(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DayType.MONDAY;
            case TUESDAY -> DayType.TUESDAY;
            case WEDNESDAY -> DayType.WEDNESDAY;
            case THURSDAY -> DayType.THURSDAY;
            case FRIDAY -> DayType.FRIDAY;
            case SATURDAY -> DayType.SATURDAY;
            case SUNDAY -> DayType.SUNDAY;
        };
    }
}