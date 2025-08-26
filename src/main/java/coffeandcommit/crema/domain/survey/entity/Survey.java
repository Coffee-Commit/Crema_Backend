package coffeandcommit.crema.domain.survey.entity;

import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "survey")
public class Survey extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String fileUploadURL;

    @Column(nullable = false)
    private String talkSubject; // 대화주제

    @Column(columnDefinition = "TEXT")
    private String subjectDescription; // 직접입력 주제

}
