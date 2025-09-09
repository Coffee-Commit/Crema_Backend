package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class ChatNotFoundException extends BaseException {
    public ChatNotFoundException() {
        super(ErrorStatus.CHAT_NOT_FOUND);
    }
    
    public ChatNotFoundException(String message) {
        super(ErrorStatus.CHAT_NOT_FOUND);
    }
}