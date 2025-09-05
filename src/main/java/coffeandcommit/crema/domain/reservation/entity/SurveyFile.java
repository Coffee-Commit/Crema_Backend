package coffeandcommit.crema.domain.reservation.entity;

import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "survey_file",
    indexes = {
        @Index(name = "idx_survey_file_survey_id", columnList = "survey_id")
    }
)
public class SurveyFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(name = "file_upload_url", length = 2048)
    private String fileUploadUrl; // 파일 업로드 URL (S3 경로)
}
