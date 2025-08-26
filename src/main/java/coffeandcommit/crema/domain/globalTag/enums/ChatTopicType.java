package coffeandcommit.crema.domain.globalTag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum ChatTopicType {

    DOCUMENTS("서류 준비"),
    INTERVIEW("면접 대비"),
    PRACTICAL_SKILL("실무 스킬"),
    COMPANY_LIFE("회사 생활"),
    MENTOR_STORY("멘토 이야기"),
    CAREER_DIRECTION("커리어 방향성");

    private final String description;
}
