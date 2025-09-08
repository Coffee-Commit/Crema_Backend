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
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

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

        // 안전한 URL 생성 (SDK 내장 방식 사용)
        return getDirectUrl(s3FileName);
    }

    /* 2. 파일 삭제 - 반환값으로 성공/실패 확인 */
    public boolean delete(String keyName) {
        try {
            // 객체 존재 여부 먼저 확인
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(keyName)
                    .build();

            s3Client.headObject(headObjectRequest);

            // DeleteObjectRequest 생성 및 객체 삭제
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(keyName)
                    .build();

            DeleteObjectResponse response = s3Client.deleteObject(deleteObjectRequest);

            log.info("S3 object deleted successfully: {}", keyName);
            return true;

        } catch (NoSuchKeyException e) {
            log.warn("S3 object not found for deletion: {}", keyName);
            return false;
        } catch (S3Exception e) {
            log.error("S3 deletion failed for key: {} - Error: {}", keyName, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during S3 deletion: {} - {}", keyName, e.getMessage());
            return false;
        }
    }

    /* 3. 공개적인 프로필/게시글 이미지: 이미지 직접 URL 조회, 안정적인 SDK 사용(aws 표준 url 형식 보장) */
    public String getImageUrl(String keyName) {
        return getDirectUrl(keyName);
    }

    /* 4. 개인 문서/임시 다운로드 링크: 파일의 presigned URL 반환 */
    public String getPresignedURL(String keyName) {
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
            log.error("Failed to generate presigned URL for key: {} - {}", keyName, e.getMessage());
            return "";
        }
    }

    /* 5. 파일 존재 여부 확인 */
    public boolean fileExists(String keyName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(keyName)
                    .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            log.debug("S3 object not found: {}", keyName);
            return false;
        } catch (S3Exception e) {
            log.error("S3 error while checking object existence: {} - Error: {}", keyName, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while checking object existence: {} - {}", keyName, e.getMessage());
            return false;
        }
    }

    /**
     * SDK를 사용한 안전한 직접 URL 생성
     */
    private String getDirectUrl(String keyName) {
        try {
            // SDK의 utilities를 사용한 안전한 URL 생성
            var url = s3Client.utilities().getUrl(builder ->
                    builder.bucket(bucket).key(keyName)
            );
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to generate direct URL for key: {} - {}", keyName, e.getMessage());
            // 폴백으로 기본 URL 패턴 사용
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, keyName);
        }
    }
}