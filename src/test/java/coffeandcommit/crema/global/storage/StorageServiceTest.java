package coffeandcommit.crema.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.storage.impl.GcsStorageServiceImpl;
import coffeandcommit.crema.global.validation.FileType;
import coffeandcommit.crema.global.validation.FileValidator;
import coffeandcommit.crema.global.validation.ValidatedFile;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private Storage storage;

    @Mock
    private FileValidator fileValidator;

    // 실사용에선 구체 클래스가 아닌 StorageService를 써서 구현해주세요
    @InjectMocks
    private GcsStorageServiceImpl storageService;

    private final String bucketName = "test-bucket";
    private final String testUser = "1234";

    @BeforeEach
    void setUp() {
        storageService = new GcsStorageServiceImpl(storage, bucketName);
    }

    @Test
    @DisplayName("유효한 이미지 파일 업로드 성공 시 응답 반환")
    void uploadFile_withValidImage_shouldSucceed() throws IOException {
        // given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "test-image.png",
                "image/png",
                "test image content".getBytes()
        );

        doNothing().when(fileValidator).validate(any(MockMultipartFile.class), any(FileType.class));

        ValidatedFile validatedFile = ValidatedFile.of(imageFile, FileType.IMAGE, fileValidator);

        String expectedFileKey = "images/" + testUser + "_" + imageFile.getOriginalFilename();
        String expectedFileUrl = "https://storage.googleapis.com/" + bucketName + "/" + expectedFileKey;

        // storage.create()
        // BlobInfo, Byte[]가 존재해도 null 반환
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        // when
        FileUploadResponse response = storageService.uploadFile(validatedFile, "images", testUser);

        // then
        assertNotNull(response);
        assertThat(response.getFileKey()).isEqualTo(expectedFileKey);
        assertThat(response.getFileUrl()).isEqualTo(expectedFileUrl);

        // call count check
        verify(storage, times(1)).create(any(BlobInfo.class), eq(imageFile.getBytes()));
    }
}