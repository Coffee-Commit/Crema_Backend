package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.BaseCode;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class SessionNotFoundException extends BaseException {
    public SessionNotFoundException() {
        super(ErrorStatus.SESSION_CREATE_EXCEPTION);
    }
}
