package coffeandcommit.crema.domain.globalTag.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum JobNameType {

    BACKEND("백엔드 개발"),
    FRONTEND("프론트엔드 개발"),
    APP("앱 개발"),
    DATA_AI("데이터/AI 엔지니어링"),
    DEVOPS_CLOUD("DevOps/클라우드"),

    MARKETING("마케팅(디지털, 브랜드 포함)"),
    SALES("세일즈/영업"),
    CONSULTING("전략/컨설팅"),
    OPERATION_CS("운영/고객관리(CS)"),

    SERVICE_PLANNING("서비스/사업 기획"),
    PM("프로덕트 매니저(PM)"),
    UX_UI("UX/UI 디자인"),
    BRANDING_GRAPHIC("브랜딩/그래픽 디자인"),

    HR("인사/조직관리(HR)"),
    FINANCE("재무/회계"),
    LEGAL("법무/특허");

    private final String description;
}
