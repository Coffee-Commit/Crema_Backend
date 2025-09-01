package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class ParticipantNotFound extends BaseException {
    public ParticipantNotFound() {
        super(ErrorStatus.PARTICIPANT_NOT_FOUND);
    }
}
