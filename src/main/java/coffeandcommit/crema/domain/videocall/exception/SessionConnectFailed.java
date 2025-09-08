package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class SessionConnectFailed extends BaseException {
    public SessionConnectFailed() {
        super(ErrorStatus.SESSION_CONNECT_FAILED);
    }
}
