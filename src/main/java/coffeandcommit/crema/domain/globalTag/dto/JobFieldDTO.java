package coffeandcommit.crema.domain.globalTag.dto;

import coffeandcommit.crema.domain.globalTag.entity.JobField;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobFieldDTO {

    private Long id;
    private JobType jobType;
    private JobNameType jobName;

    public static JobFieldDTO from(JobField jobField) {
        return JobFieldDTO.builder()
                .id(jobField.getId())
                .jobType(jobField.getJobType())
                .jobName(jobField.getJobName())
                .build();
    }
}
