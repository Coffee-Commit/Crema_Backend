package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.BaseCode;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class SessionCreationException extends BaseException {
    public SessionCreationException() {
        super(ErrorStatus.SESSION_CREATE_EXCEPTION);
    }
    
    public SessionCreationException(String detailMessage) {
        super(ErrorStatus.SESSION_CREATE_EXCEPTION, detailMessage);
    }
}
