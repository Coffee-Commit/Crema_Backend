package coffeandcommit.crema.global.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import coffeandcommit.crema.global.common.exception.code.BaseCode;

/**
 * 어플리케이션의 모든 커스텀 예외에 대한 최상위 부모 클래스
 */
@Getter
public class BaseException extends RuntimeException {

    private final BaseCode errorCode;
    
    public BaseException(BaseCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public BaseException(BaseCode errorCode, String detailMessage) {
        super(errorCode.getMessage() + " - " + detailMessage);
        this.errorCode = errorCode;
    }
}
