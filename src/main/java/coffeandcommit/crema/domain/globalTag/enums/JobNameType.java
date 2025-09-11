package coffeandcommit.crema.domain.globalTag.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static JobNameType from(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        // 1) Enum 이름으로 매칭 (권장 입력값)
        for (JobNameType type : values()) {
            if (type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        // 2) 한글 설명으로 매칭 (기존 클라이언트 호환)
        for (JobNameType type : values()) {
            if (type.getDescription().equals(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("사용 불가능한 JobNameType: " + text);
    }
}
