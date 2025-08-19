package coffeandcommit.crema.domain.videocall.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String message) {
        super(message);
    }
    
    public SessionNotFoundException() {
        super("세션을 찾을 수 없습니다.");
    }
}