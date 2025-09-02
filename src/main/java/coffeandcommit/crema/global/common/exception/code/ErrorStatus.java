package coffeandcommit.crema.global.common.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 요청 처리 중 발생하는 오류에 대한 상태 코드를 관리하는 Enum
 */
@RequiredArgsConstructor
public enum ErrorStatus implements BaseCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "유효성 검사 실패"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않은 HTTP Method 입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // Member Domain
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    NICKNAME_DUPLICATED(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "닉네임 형식이 올바르지 않습니다. (2-32자, 한글/영문/숫자/언더스코어만 허용)"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),
    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "포인트 금액이 올바르지 않습니다."),

    // JWT & Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 필요합니다."),
    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OAuth2 인증에 실패했습니다."),
    UNSUPPORTED_OAUTH2_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth2 제공자입니다."),

    // File Upload - 프로필 이미지 전용 에러 추가
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "유효하지 않은 이미지 파일입니다. 실제 JPG, JPEG, PNG 파일만 업로드 가능합니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다. (최대 2MB)"),
    PROFILE_IMAGE_DIMENSION_INVALID(HttpStatus.BAD_REQUEST, "이미지 크기가 올바르지 않습니다. (100x100 ~ 1024x1024 권장)"),

    // Guide Domain
    GUIDE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 가이드를 찾을 수 없습니다."),
    GUIDE_JOB_FIELD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 가이드의 직무 분야를 찾을 수 없습니다."),
    INVALID_JOB_FIELD(HttpStatus.BAD_REQUEST, "잘못된 직무 분야 요청입니다."),
    INVALID_TOPIC(HttpStatus.BAD_REQUEST, "잘못된 주제 요청입니다."),
    MAX_TOPIC_EXCEEDED(HttpStatus.BAD_REQUEST, "등록 가능한 주제 개수를 초과했습니다."),
    GUIDE_CHAT_TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 가이드 채팅 주제를 찾을 수 없습니다."),
    MAX_HASHTAG_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "해시태그는 최대 5개까지 등록할 수 있습니다."),
    DUPLICATE_HASHTAG(HttpStatus.CONFLICT, "이미 등록된 해시태그입니다."),
    HASHTAG_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 해시태그를 찾을 수 없습니다."),
    PROFILE_IMAGE_DIMENSION_INVALID(HttpStatus.BAD_REQUEST, "이미지 크기가 올바르지 않습니다. (100x100 ~ 1024x1024 권장)"),

    // openvidu
    SESSION_CREATE_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR , "openvidu session 생성에 실패했습니다."),
    RECORDING_ALREADY_STARTED(HttpStatus.INTERNAL_SERVER_ERROR , "이미 녹음이 진행 중 입니다."),
    RECORDING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR , "녹음이 실패했습니다."),
    SESSION_CONNECT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR , "세션 연결에 실패했습니다."),
    SESSION_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR , "존재하지 않는 openvidu session 입니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR , "존재하지 않는 participant 입니다."),
    TOKEN_REFRESH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 갱신에 실패했습니다."),
    AUTO_RECONNECT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "자동 재연결에 실패했습니다."),
    OPENVIDU_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OpenVidu 서버에 연결할 수 없습니다."),

    // Chat
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅 기록을 찾을 수 없습니다."),
    CHAT_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "채팅 기록 저장에 실패했습니다.");

    public static final String PREFIX = "[ERROR]";

    private final HttpStatus httpStatus;
    private final String rawMessage;

    @Override
    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }

    @Override
    public String getMessage() {
        return PREFIX + this.rawMessage;
    }
}