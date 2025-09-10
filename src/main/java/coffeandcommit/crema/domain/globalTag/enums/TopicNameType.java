package coffeandcommit.crema.domain.globalTag.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum TopicNameType {
    UNDEFINED("미정"),

    RESUME("이력서"),
    COVER_LETTER("자소서"),
    PORTFOLIO("포트폴리오"),
    INTERVIEW("면접"),
    PRACTICAL_WORK("실무"),
    ORGANIZATION_CULTURE("조직문화"),
    WORK_LIFE_BALANCE("워라밸"),
    RELATIONSHIP("인간관계"),
    PASS_EXPERIENCE("합격 경험"),
    INDUSTRY_TREND("업계 트렌드"),
    CAREER_CHANGE("직무 전환"),
    JOB_CHANGE("이직");

    private final String description;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TopicNameType from(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        for (TopicNameType t : values()) {
            if (t.name().equalsIgnoreCase(text)) {
                return t;
            }
        }
        for (TopicNameType t : values()) {
            if (t.getDescription().equals(text)) {
                return t;
            }
        }
        throw new IllegalArgumentException("사용 불가능한 TopicNameType: " + text);
    }
}
