package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "guide")
public class Guide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member; // FK, 멤버 ID는 String (UUID)

    @Column(name = "chat_description", length = 1000)
    private String chatDescription;

    @Column(nullable = false)
    private boolean isOpened;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2048)
    private String certificationImageUrl;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "working_start")
    private LocalDate workingStart;

    @Column(name = "working_end")
    private LocalDate workingEnd;

    @Column(name = "working_period")
    private String workingPeriod;

    @Column(name = "company_name", length = 16)
    private String companyName;

    @Column(name = "is_company_name_public", nullable = false)
    @Builder.Default
    private boolean isCompanyNamePublic = true;

    @Column(name = "job_position")
    private String jobPosition;

    @Column(name = "is_current")
    private boolean isCurrent;


    // test auth에서 멤버(가이드) 하드 삭제 시, 연관된 가이드 정보도 함께 삭제되도록 설정
    @OneToOne(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private GuideJobField guideJobField;

    @OneToMany(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GuideChatTopic> guideChatTopics = new ArrayList<>();

    @OneToMany(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HashTag> hashTags = new ArrayList<>();

    @OneToMany(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GuideSchedule> guideSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExperienceGroup> experienceGroups = new ArrayList<>();

    @OneToOne(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ExperienceDetail experienceDetail;

}
