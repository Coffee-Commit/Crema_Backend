package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class TokenRefreshFailedException extends BaseException {
    public TokenRefreshFailedException() {
        super(ErrorStatus.TOKEN_REFRESH_FAILED);
    }
    
    public TokenRefreshFailedException(String detailMessage) {
        super(ErrorStatus.TOKEN_REFRESH_FAILED, detailMessage);
    }
}