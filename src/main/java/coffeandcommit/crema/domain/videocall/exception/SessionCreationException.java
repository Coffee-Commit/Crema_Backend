package coffeandcommit.crema.domain.videocall.exception;

public class SessionCreationException extends RuntimeException {
    public SessionCreationException(String message) {
        super(message);
    }
    
    public SessionCreationException() {
        super("세션 생성에 실패했습니다.");
    }
}