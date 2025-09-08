package coffeandcommit.crema.domain.member.dto.response;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.member.entity.MemberJobField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "멤버 커피챗 분야 응답")
public class MemberJobFieldResponse {

    @Schema(description = "멤버 커피챗 분야 ID", example = "1")
    private Long id;

    @Schema(description = "멤버 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String memberId;

    @Schema(description = "커피챗 분야", example = "IT_DEVELOPMENT_DATA")
    private JobNameType jobName;

    @Schema(description = "커피챗 분야명", example = "IT 개발/데이터")
    private String jobNameDescription;

    public static MemberJobFieldResponse from(MemberJobField memberJobField) {
        JobNameType jobName = memberJobField.getJobName() != null
                ? memberJobField.getJobName()
                : JobNameType.UNDEFINED;

        return MemberJobFieldResponse.builder()
                .id(memberJobField.getId())
                .memberId(memberJobField.getMember().getId())
                .jobName(jobName)
                .jobNameDescription(jobName.getDescription())
                .build();
    }
}