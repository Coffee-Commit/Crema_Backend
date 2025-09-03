package coffeandcommit.crema.domain.globalTag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum ChatTopicType {

    DOCUMENTS_AND_INTERVIEW("서류 및 면접"),
    COMPANY_LIFE("회사 생활"),
    CAREER("커리어");

    private final String description;
}
