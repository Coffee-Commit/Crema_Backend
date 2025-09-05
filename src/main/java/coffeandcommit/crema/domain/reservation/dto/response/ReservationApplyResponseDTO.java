package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationApplyResponseDTO {

    private MemberDTO member;
    private GuideDTO guide;

    public static ReservationApplyResponseDTO from(Member member, Guide guide) {
        return ReservationApplyResponseDTO.builder()
                .member(MemberDTO.from(member))
                .guide(GuideDTO.from(guide))
                .build();
    }
}
