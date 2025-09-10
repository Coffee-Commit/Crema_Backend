package coffeandcommit.crema.domain.survey.entity;

import coffeandcommit.crema.domain.reservation.entity.SurveyFile;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// @Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
// @Table(name = "survey")
public class Survey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_to_guide", length = 1000)
    private String messageToGuide;

    @Column(name = "preferred_date")
    private LocalDateTime preferredDate;
    
    // @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // @Builder.Default
    // private List<SurveyFile> files = new ArrayList<>();
}