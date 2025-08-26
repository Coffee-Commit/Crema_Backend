package coffeandcommit.crema.domain.globalTag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum JobType {

    DEV_ENGINEERING("개발/엔지니어링"),
    BUSINESS_MARKETING("비즈니스/마케팅"),
    PLANNING_DESIGN("기획/디자인"),
    MANAGEMENT_SUPPORT("경영/지원");

    private final String description;
}
