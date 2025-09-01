package coffeandcommit.crema.domain.globalTag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum TopicNameType {

    RESUME("이력서"),
    COVER_LETTER("자소서"),
    PORTFOLIO("포트폴리오"),
    MOCK_INTERVIEW("모의 면접"),
    WORK_SKILL("업무 노하우"),
    TOOL_USAGE("필수 툴 사용법"),
    ORGANIZATION_CULTURE("조직문화"),
    WORK_LIFE_BALANCE("워라밸"),
    STARTUP_EXPERIENCE("창업 경험"),
    FREELANCE_EXPERIENCE("프리랜서 경험"),
    GLOBAL_EXPERIENCE("해외 근무 및 글로벌 경험"),
    INDUSTRY_TREND("업계 트렌드"),
    CAREER_CHANGE("직무 전환"),
    JOB_CHANGE("이직 노하우");

    private final String description;
}
