
    /**
     * 고유한 파일명 생성
     * 형식: {userId}_{timestamp}_{uuid}_{originalName}
     */
    private String generateFileName(String originalFilename, String userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        // 원본 파일명에서 확장자 추출
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
            originalFilename = originalFilename.substring(0, lastDotIndex);
        }

        // 파일명 길이 제한 (S3 키 길이 제한 고려)
        if (originalFilename.length() > 30) {
            originalFilename = originalFilename.substring(0, 30);
        }

        // 특수문자 제거 및 안전한 파일명 생성
        String safeName = originalFilename.replaceAll("[^a-zA-Z0-9가-힣._-]", "_");

        return String.format("%s_%s_%s_%s%s", userId, timestamp, uuid, safeName, extension);
    }
}