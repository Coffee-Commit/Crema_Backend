package coffeandcommit.crema.domain.review.enums;

public enum ReviewWriteFilter {
    ALL,
    WRITTEN,
    NOT_WRITTEN;

    public static ReviewWriteFilter from(String value) {
        if (value == null || value.isBlank()) return ALL;
        switch (value.trim().toUpperCase()) {
            case "WRITTEN":
                return WRITTEN;
            case "NOT_WRITTEN":
                return NOT_WRITTEN;
            case "ALL":
                return ALL;
            default:
                // 무효 값은 ALL로 처리하거나, 정책상 예외를 던질 수 있음
                return ALL;
        }
    }
}

