package coffeandcommit.crema.global.validation;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileValidator {

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg");
    private static final String ALLOWED_PDF_TYPE = "application/pdf";
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    public void validate(MultipartFile file, FileType fileType) {
        switch (fileType) {
            case IMAGE -> validateImage(file);
            case PDF -> validatePdf(file);
            default -> throw new IllegalArgumentException("지원하지 않는 파일 타입입니다.");
        }
    }

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
        }
        final String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다,");
        }
        if (!originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }
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
}
