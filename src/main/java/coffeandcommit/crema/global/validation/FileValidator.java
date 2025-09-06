package coffeandcommit.crema.global.validation;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileValidator {

    // 확장자
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg");
    private static final String ALLOWED_PDF_TYPE = "application/pdf";

    // 사이즈
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_PROFILE_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB


    public void validate(MultipartFile file, FileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        switch (fileType) {
            case IMAGE -> validateImage(file);
            case PDF -> validatePdf(file);
            case PROFILE_IMAGE -> validateProfileImage(file);
            default -> throw new IllegalArgumentException("지원하지 않는 파일 타입입니다.");
        }
    }

    public void validateProfileImage(MultipartFile file) {
        validateImageBasics(file);

        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new IllegalArgumentException("프로필 이미지 크기는 2MB를 초과할 수 없습니다.");
        }
    }

    public void validateImage(MultipartFile file) {
        validateImageBasics(file);
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("이미지 파일 크기가 너무 큽니다.");
        }
    }

    public void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF 파일이 비어있습니다.");
        }
        final String contentType = file.getContentType();
        if (!ALLOWED_PDF_TYPE.equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("PDF 파일만 업로드할 수 있습니다.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty() || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("PDF 파일명이 유효하지 않습니다.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("PDF 파일 크기가 너무 큽니다.");
        }
    }

    // 공통 이미지 검증 로직
    private void validateImageBasics(MultipartFile file) {
        final String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일명 또는 확장자가 유효하지 않습니다.");
        }

        try {
            if (!isValidImageFileSignature(file.getBytes())) {
                throw new IllegalArgumentException("파일 시그니처가 유효하지 않습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException("파일을 읽는 중 오류가 발생했습니다.", e);
        }
    }

    private boolean isValidImageFileSignature(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 8) return false;
        if (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && fileBytes[2] == (byte) 0xFF) return true;
        if (fileBytes[0] == (byte) 0x89 && fileBytes[1] == (byte) 0x50 && fileBytes[2] == (byte) 0x4E) return true;

        return false;
    }
}
