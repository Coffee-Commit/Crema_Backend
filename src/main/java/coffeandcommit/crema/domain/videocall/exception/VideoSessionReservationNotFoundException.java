package coffeandcommit.crema.domain.videocall.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class VideoSessionReservationNotFoundException extends BaseException {
    public VideoSessionReservationNotFoundException() {
        super(ErrorStatus.RESERVATION_NOT_FOUND);
    }
    
    public VideoSessionReservationNotFoundException(String detailMessage) {
        super(ErrorStatus.RESERVATION_NOT_FOUND, detailMessage);
    }
}