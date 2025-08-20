package coffeandcommit.crema.domain.videocall.controller;

import coffeandcommit.crema.domain.videocall.dto.response.FileUploadResponse;
import coffeandcommit.crema.domain.videocall.entity.UploadedFile;
import coffeandcommit.crema.domain.videocall.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "파일 업로드", description = "화상통화 파일 업로드 및 관리 API")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    @Operation(summary = "파일 업로드", description = "이미지 또는 PDF 파일을 업로드합니다.")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId,
            @RequestParam("username") String username) {
        
        log.info("파일 업로드 요청: sessionId={}, username={}, fileName={}", 
                sessionId, username, file.getOriginalFilename());
        
        // 파일 유효성 검증
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // 파일 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || 
            (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            log.warn("지원하지 않는 파일 타입: {}", contentType);
            return ResponseEntity.badRequest().build();
        }
        
        // 파일 크기 검증 (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            log.warn("파일 크기 초과: {} bytes", file.getSize());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            FileUploadResponse response = fileUploadService.uploadFile(file, sessionId, username);
            
            log.info("파일 업로드 완료: id={}", response.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 업로드 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "세션별 파일 목록 조회", description = "특정 세션에 업로드된 모든 파일을 조회합니다.")
    public ResponseEntity<List<FileUploadResponse>> getFilesBySessionId(
            @PathVariable String sessionId) {
        
        log.info("세션 파일 목록 조회 요청: sessionId={}", sessionId);
        
        try {
            List<FileUploadResponse> files = fileUploadService.getFilesBySessionId(sessionId);
            
            log.info("세션 파일 목록 조회 완료: sessionId={}, count={}", sessionId, files.size());
            
            return ResponseEntity.ok(files);
            
        } catch (Exception e) {
            log.error("세션 파일 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{fileId}/view")
    @Operation(summary = "파일 뷰어", description = "파일 ID로 파일을 뷰어에서 바로 봅니다. (주로 이미지용)")
    public ResponseEntity<Resource> viewFile(@PathVariable Long fileId) {
        
        log.info("파일 뷰어 요청: fileId={}", fileId);
        
        try {
            Resource resource = fileUploadService.loadFileAsResource(fileId);
            UploadedFile uploadedFile = fileUploadService.getFileById(fileId);
            
            // 파일명 인코딩 (한글 파일명 지원)
            String encodedFileName = URLEncoder.encode(
                uploadedFile.getOriginalFileName(), 
                StandardCharsets.UTF_8.toString()
            ).replaceAll("\\+", "%20");
            
            // Content-Type 설정
            String contentType = uploadedFile.getFileType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "inline; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
                    
        } catch (UnsupportedEncodingException e) {
            log.error("파일명 인코딩 실패", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("파일 뷰어 실패", e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{fileId}/download")
    @Operation(summary = "파일 다운로드", description = "파일 ID로 파일을 다운로드합니다.")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        
        log.info("파일 다운로드 요청: fileId={}", fileId);
        
        try {
            Resource resource = fileUploadService.loadFileAsResource(fileId);
            UploadedFile uploadedFile = fileUploadService.getFileById(fileId);
            
            // 파일명 인코딩 (한글 파일명 지원)
            String encodedFileName = URLEncoder.encode(
                uploadedFile.getOriginalFileName(), 
                StandardCharsets.UTF_8.toString()
            ).replaceAll("\\+", "%20");
            
            // Content-Type 설정
            String contentType = uploadedFile.getFileType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
                    
        } catch (UnsupportedEncodingException e) {
            log.error("파일명 인코딩 실패", e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("파일 다운로드 실패", e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/all")
    @Operation(summary = "전체 파일 목록 조회", description = "업로드된 모든 파일을 조회합니다. (디버깅용)")
    public ResponseEntity<List<FileUploadResponse>> getAllFiles() {
        
        log.info("전체 파일 목록 조회 요청");
        
        try {
            List<FileUploadResponse> files = fileUploadService.getAllFiles();
            
            log.info("전체 파일 목록 조회 완료: count={}", files.size());
            
            return ResponseEntity.ok(files);
            
        } catch (Exception e) {
            log.error("전체 파일 목록 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}