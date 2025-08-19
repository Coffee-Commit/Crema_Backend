package coffeandcommit.crema.domain.videocall.exception;

public class ParticipantNotFoundException extends RuntimeException {
    public ParticipantNotFoundException(String message) {
        super(message);
    }
    
    public ParticipantNotFoundException() {
        super("참가자를 찾을 수 없습니다.");
    }
}