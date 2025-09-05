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

    // Database Initialization - 테이블 초기화 관련
    TABLE_DATA_NOT_INITIALIZED(HttpStatus.PRECONDITION_FAILED, "테이블에 기본 데이터가 설정되지 않았습니다."),
    REFERENCE_DATA_MISSING(HttpStatus.PRECONDITION_FAILED, "필요한 기준 데이터가 누락되었습니다."),

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
    INVALID_TIME_RANGE(HttpStatus.UNPROCESSABLE_ENTITY, "시작 시간이 종료 시간보다 같거나 늦을 수 없습니다."),
    TIME_SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 시간 구간을 찾을 수 없습니다."),
    DUPLICATE_TIME_SLOT(HttpStatus.CONFLICT, "해당 요일에 겹치는 시간대가 이미 존재합니다."),
    EXPERIENCE_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 가이드 경험 소주제를 찾을 수 없습니다."),
    EXPERIENCE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "경험 대주제는 최대 6개까지만 등록할 수 있습니다."),
    INVALID_GUIDE_CHAT_TOPIC(HttpStatus.BAD_REQUEST, "잘못된 가이드 커피챗 주제 요청입니다."),
    EXPERIENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 가이드 경험 대주제를 찾을 수 없습니다."),

    // Review Domain
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예약을 찾을 수 없습니다."),
    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "이미 해당 예약에 대한 리뷰가 존재합니다."),
    REVIEW_NOT_ALLOWED_YET(HttpStatus.CONFLICT, "커피챗 종료 이후에만 리뷰를 작성할 수 있습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 리뷰를 찾을 수 없습니다."),
    INVALID_RESERVATION_ID(HttpStatus.BAD_REQUEST, "예약 ID는 null일 수 없습니다."),

    // Reservation Domain
    INVALID_STATUS(HttpStatus.CONFLICT,"잘못된 예약 상태 요청입니다."),
    ALREADY_DECIDED(HttpStatus.CONFLICT,"이미 처리된 예약입니다."),
    INVALID_TIME_UNIT(HttpStatus.BAD_REQUEST, "유효하지 않은 시간 단위입니다.");



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