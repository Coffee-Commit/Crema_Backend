package coffeandcommit.crema.global.storage.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

import coffeandcommit.crema.global.file.FileService;
import coffeandcommit.crema.global.storage.StorageService;
import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.validation.FileType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.Base64;

@SpringBootTest
public class StorageServiceIntegrationTest {

    @Autowired
    private FileService fileService;

    @Autowired
    private StorageService storageService;

    @Value("${spring.cloud.gcp.storage.bucket-name}")
    private String bucketName;

    private String uploadedFileKey = null;


    @AfterEach
    void tearDown() {
        if (uploadedFileKey != null) {
            try {
                System.out.println("Clean up file : " + uploadedFileKey);
                storageService.deleteFile(uploadedFileKey);
            } catch (Exception e) {
                System.out.println("Cleanup failed for key: " + uploadedFileKey + " - " + e.getMessage());
            } finally {
                uploadedFileKey = null;
            }
        }
    }

    @Test
    @DisplayName("GCS 파일 업로드 및 URL 조회 후 삭제")
    @EnabledIfEnvironmentVariable(named = "RUN_GCS_IT", matches = "true")
    void uploadAndViewAndDelete_IntegrationTest() {
        // given
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII"
        );
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "integration-test-file.png",
                "image/png",
                png
        );

        FileUploadResponse response = fileService.uploadFile(
                testFile,
                FileType.IMAGE,
                "integration-tests",
                "test-user"
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileKey()).isNotBlank();
        assertThat(response.getFileUrl()).contains(bucketName);

        // given
        this.uploadedFileKey = response.getFileKey();

        // when
        String viewUrl = storageService.generateViewUrl(this.uploadedFileKey);

        System.out.println("Generated View URL: " + viewUrl);
        assertThat(viewUrl).isNotNull();
        assertThat(viewUrl).contains("storage.googleapis.com");
    }
}
