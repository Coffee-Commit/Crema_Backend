package coffeandcommit.crema.global.common.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 요청 처리 중 발생하는 오류에 대한 상태 코드를 관리하는 Enum
 */
@RequiredArgsConstructor
public enum ErrorStatus implements BaseCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "유효성 검사 실패"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않은 HTTP Method 입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // Member Domain
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USERNAME_DUPLICATED(HttpStatus.BAD_REQUEST, "이미 사용 중인 사용자명입니다."),
    PHONE_NUMBER_DUPLICATED(HttpStatus.BAD_REQUEST, "이미 사용 중인 전화번호입니다."),

    // JWT & Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 필요합니다."),
    
    // Domain Specific
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    // TODO: 필요한 도메인별 에러 코드를 여기에 추가
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 모집 공고를 찾을 수 없습니다."),
    DUPLICATE_BOOKMARK(HttpStatus.BAD_REQUEST, "이미 즐겨찾기에 등록된 공고입니다."),
    RECRUITMENTBOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 즐겨찾기 정보를 찾을 수 없습니다.");

    public static final String PREFIX = "[ERROR]";

    private final HttpStatus httpStatus;
    private final String rawMessage;

    @Override
    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }

    @Override
    public String getMessage() {
        return PREFIX + this.rawMessage;
    }
}
