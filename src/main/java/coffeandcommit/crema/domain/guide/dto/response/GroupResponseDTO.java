package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponseDTO {

    private Long id;                 // 경험 대주제 PK
    private Long guideChatTopicId;   // 연결된 커피챗 주제 ID
    private String chatTopicName;    // 주제 이름 (ex: 포트폴리오, 자소서 등)
    private String experienceTitle;  // 경험 제목
    private String experienceContent;// 경험 내용

    public static GroupResponseDTO from(ExperienceGroup experienceGroup) {
        return GroupResponseDTO.builder()
                .id(experienceGroup.getId())
                .guideChatTopicId(experienceGroup.getGuideChatTopic().getId())
                .chatTopicName(experienceGroup.getGuideChatTopic().getChatTopic().getTopicName().getDescription())
                .experienceTitle(experienceGroup.getExperienceTitle())
                .experienceContent(experienceGroup.getExperienceContent())
                .build();
    }
}
