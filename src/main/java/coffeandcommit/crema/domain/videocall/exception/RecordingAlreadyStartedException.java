package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class RecordingAlreadyStartedException extends BaseException {
    public RecordingAlreadyStartedException() {
        super(ErrorStatus.RECORDING_ALREADY_STARTED);
    }
}
