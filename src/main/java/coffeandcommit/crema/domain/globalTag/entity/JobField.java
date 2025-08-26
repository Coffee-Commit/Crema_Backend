package coffeandcommit.crema.domain.globalTag.entity;

import coffeandcommit.crema.global.common.entitiy.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "job_field")
public class JobField extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobType; // 직무 대분류 이름

    @Column(nullable = false)
    private String jobName; // 직무 분야 이름
}
