package coffeandcommit.crema.global.AWS.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Service {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final S3Client s3Client;

    /* 1. 파일 업로드 */
    public String upload(MultipartFile multipartFile, String s3FileName) throws IOException {
        // PutObjectRequest 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3FileName)
                .contentType(multipartFile.getContentType())
                .contentLength(multipartFile.getSize())
                .build();
        
        // S3에 객체 등록
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
        
        // 등록된 객체의 url 반환 (decoder: url 안의 한글or특수문자 깨짐 방지)
        String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, "ap-northeast-2", s3FileName);
        return URLDecoder.decode(url, "utf-8");
    }

    /* 2. 파일 삭제 */
    public void delete (String keyName) {
        try {
            // DeleteObjectRequest 생성 및 객체 삭제
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(keyName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            log.error(e.toString());
        }
    }

    /* 3. 이미지 직접 URL 조회 */
    public String getImageUrl(String keyName) {
        try {
            // S3 객체의 직접 URL 반환 (만료 시간 없음)
            String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, "ap-northeast-2", keyName);
            return URLDecoder.decode(url, "utf-8");
        } catch (Exception e) {
            log.error("S3 이미지 URL 조회 중 오류 발생: " + e.toString());
            return null;
        }
    }

    /* 4. 파일의 presigned URL 반환 */
    public String getPresignedURL (String keyName) {
        try {
            // GetObjectRequest 생성
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(keyName)
                    .build();
            
            // S3Presigner 생성 및 presigned URL 발급 (2분 유효)
            try (S3Presigner presigner = S3Presigner.create()) {
                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(2))
                        .getObjectRequest(getObjectRequest)
                        .build();
                
                PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
                return presignedRequest.url().toString();
            }
        } catch (Exception e) {
            log.error(e.toString());
            return "";
        }
    }
}
