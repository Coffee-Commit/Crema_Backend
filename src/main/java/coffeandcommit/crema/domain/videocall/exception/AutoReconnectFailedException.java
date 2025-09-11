package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class AutoReconnectFailedException extends BaseException {
    public AutoReconnectFailedException() {
        super(ErrorStatus.AUTO_RECONNECT_FAILED);
    }
    
    public AutoReconnectFailedException(String detailMessage) {
        super(ErrorStatus.AUTO_RECONNECT_FAILED, detailMessage);
    }
}