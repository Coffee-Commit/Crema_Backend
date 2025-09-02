package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class ChatSaveFailedException extends BaseException {
    public ChatSaveFailedException() {
        super(ErrorStatus.CHAT_SAVE_FAILED);
    }
}