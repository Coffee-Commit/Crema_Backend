package coffeandcommit.crema.domain.survey.entity;

import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


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

    @Column(columnDefinition = "TEXT")
    private String messageToGuide; // 직접입력 주제

    @Column(nullable = false)
    private LocalDateTime preferred_date; // 희망 날짜

}
