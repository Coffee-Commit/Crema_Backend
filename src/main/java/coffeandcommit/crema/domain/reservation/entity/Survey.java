package coffeandcommit.crema.domain.reservation.entity;

import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


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

    @Column(nullable = true)
    private String messageToGuide; // 직접입력 주제

    @Column(name = "preferred_date",nullable = false)
    private LocalDateTime preferredDate; // 희망 날짜

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurveyFile> files = new ArrayList<>();

}
