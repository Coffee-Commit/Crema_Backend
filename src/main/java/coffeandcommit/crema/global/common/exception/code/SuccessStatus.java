package coffeandcommit.crema.global.common.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 성공적인 API 응답을 위한 상태 코드를 관리하는 Enum
 */
@RequiredArgsConstructor
public enum SuccessStatus implements BaseCode {
    OK(HttpStatus.OK, "성공입니다."),
    CREATED(HttpStatus.CREATED, "요청 성공 및 리소스 생성됨");
    // TODO: 필요한 도메인별 성공 코드를 여기에 추가

    public static final String PREFIX = "[SUCCESS]";

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
