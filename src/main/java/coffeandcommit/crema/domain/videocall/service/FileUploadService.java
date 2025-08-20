package coffeandcommit.crema.domain.videocall.service;

import coffeandcommit.crema.domain.videocall.dto.response.FileUploadResponse;
import coffeandcommit.crema.domain.videocall.entity.UploadedFile;
import coffeandcommit.crema.domain.videocall.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileUploadService {

    private final UploadedFileRepository uploadedFileRepository;
    
    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;
    
    public FileUploadResponse uploadFile(MultipartFile file, String sessionId, String username) {
        log.info("파일 업로드 시작: sessionId={}, username={}, fileName={}", 
                sessionId, username, file.getOriginalFilename());
        
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 파일명 생성 (UUID + 원본 확장자)
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            // 파일 저장
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 데이터베이스에 파일 정보 저장
            UploadedFile uploadedFile = UploadedFile.builder()
                    .sessionId(sessionId)
                    .fileName(fileName)
                    .originalFileName(originalFileName)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .filePath(filePath.toString())
                    .uploader(username)
                    .build();
            
            UploadedFile savedFile = uploadedFileRepository.save(uploadedFile);
            
            log.info("파일 업로드 완료: id={}, fileName={}", savedFile.getId(), savedFile.getFileName());
            
            return FileUploadResponse.from(savedFile);
            
        } catch (IOException e) {
            log.error("파일 업로드 실패", e);
            throw new RuntimeException("파일 업로드에 실패했습니다: " + e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public List<FileUploadResponse> getFilesBySessionId(String sessionId) {
        log.info("세션 파일 목록 조회: sessionId={}", sessionId);
        
        List<UploadedFile> files = uploadedFileRepository.findBySessionIdOrderByUploadedAtDesc(sessionId);
        
        log.info("세션 파일 목록 조회 완료: sessionId={}, count={}", sessionId, files.size());
        
        return files.stream()
                .map(FileUploadResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Resource loadFileAsResource(Long fileId) {
        log.info("파일 다운로드 요청: fileId={}", fileId);
        
        UploadedFile uploadedFile = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + fileId));
        
        try {
            Path filePath = Paths.get(uploadedFile.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                log.info("파일 다운로드 성공: fileId={}, fileName={}", fileId, uploadedFile.getFileName());
                return resource;
            } else {
                log.error("파일을 읽을 수 없음: fileId={}, filePath={}", fileId, uploadedFile.getFilePath());
                throw new RuntimeException("파일을 읽을 수 없습니다: " + fileId);
            }
        } catch (MalformedURLException e) {
            log.error("파일 경로 오류", e);
            throw new RuntimeException("파일 경로가 올바르지 않습니다: " + e.getMessage());
        }
    }
    
    @Transactional(readOnly = true)
    public UploadedFile getFileById(Long fileId) {
        return uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + fileId));
    }
    
    @Transactional(readOnly = true)
    public List<FileUploadResponse> getAllFiles() {
        log.info("전체 파일 목록 조회");
        
        List<UploadedFile> files = uploadedFileRepository.findAllByOrderByUploadedAtDesc();
        
        log.info("전체 파일 목록 조회 완료: count={}", files.size());
        
        return files.stream()
                .map(FileUploadResponse::from)
                .collect(Collectors.toList());
    }
}