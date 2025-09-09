package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class RecordingFailedException extends BaseException {
    public RecordingFailedException() {
        super(ErrorStatus.RECORDING_FAILED);
    }
}
