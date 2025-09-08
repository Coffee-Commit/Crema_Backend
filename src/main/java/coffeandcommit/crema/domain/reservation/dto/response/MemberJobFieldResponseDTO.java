package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.member.entity.MemberJobField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberJobFieldResponseDTO {

    private String memberId;   // 멘티 Member UUID
    private JobNameType jobName;

    public static MemberJobFieldResponseDTO from(MemberJobField memberJobField) {
        return MemberJobFieldResponseDTO.builder()
                .memberId(memberJobField.getMember().getId())
                .jobName(
                        memberJobField.getJobName() != null
                                ? memberJobField.getJobName()
                                : JobNameType.UNDEFINED // null일 경우 보정
                )
                .build();
    }
}
