package coffeandcommit.crema.domain.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoffeeChatSummaryResponseDTO {

    private int pendingCount;    // 대기 중 커피챗 수 (status = PENDING), 취소 또는 거절된 커피챗은 포함하지 않음
    private int confirmedCount;  // 예정된 커피챗 수 (status = CONFIRMED)
    private int completedCount;  // 완료된 커피챗 수 (status = COMPLETED)

    public static CoffeeChatSummaryResponseDTO of(int pending, int confirmed, int completed) {
        return CoffeeChatSummaryResponseDTO.builder()
                .pendingCount(pending)
                .confirmedCount(confirmed)
                .completedCount(completed)
                .build();
    }
}
