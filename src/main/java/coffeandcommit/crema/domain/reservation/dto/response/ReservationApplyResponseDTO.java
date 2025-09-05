package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationApplyResponseDTO {

    private MemberDTO memberDTO;
    private GuideDTO guideDTO;

    public static ReservationApplyResponseDTO from(Member member, Guide guide) {
        return ReservationApplyResponseDTO.builder()
                .memberDTO(MemberDTO.from(member))
                .guideDTO(GuideDTO.from(guide))
                .build();
    }
}
