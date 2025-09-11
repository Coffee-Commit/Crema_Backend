package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class OpenViduConnectionException extends BaseException {
    public OpenViduConnectionException() {
        super(ErrorStatus.OPENVIDU_CONNECTION_FAILED);
    }
    
    public OpenViduConnectionException(String detailMessage) {
        super(ErrorStatus.OPENVIDU_CONNECTION_FAILED, detailMessage);
    }
}