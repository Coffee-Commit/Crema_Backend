package coffeandcommit.crema.domain.globalTag.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum JobNameType {
    UNDEFINED("미정"),

    DESIGN("디자인"),
    PLANNING_STRATEGY("기획/전략"),
    MARKETING_PR("마케팅/홍보"),
    MANAGEMENT_SUPPORT("경영/지원"),
    IT_DEVELOPMENT_DATA("IT 개발/데이터"),
    RESEARCH_RND("연구/R&D");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
