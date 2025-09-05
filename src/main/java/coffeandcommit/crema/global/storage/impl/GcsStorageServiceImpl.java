package coffeandcommit.crema.global.storage.impl;

import coffeandcommit.crema.global.storage.StorageService;
import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.validation.ValidatedFile;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GcsStorageServiceImpl implements StorageService {

    private final Storage storage;
    private final String bucketName;

    public GcsStorageServiceImpl(
            Storage storage,
            @Value("${spring.cloud.gcp.storage.bucket-name}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    private String getFileKey(String folder, String userId, String fileName) {
        String fileNameWithUserId = userId + "_" + fileName;
        return folder + "/" + fileNameWithUserId;
    }

    @Override
    public FileUploadResponse uploadFile(ValidatedFile file, String folder, String userId) {
        String fileKey = getFileKey(folder, userId, file.getOriginalFilename());
        BlobId blobId = BlobId.of(bucketName, fileKey);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        try {
            storage.create(blobInfo, file.getBytes());

            String fileUrl = "https://storage.googleapis.com/" + bucketName + "/" + fileKey;

            return FileUploadResponse.builder()
                    .fileKey(fileKey)
                    .fileUrl(fileUrl)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    @Override
    public String generateViewUrl(String fileKey) {
        URL url = storage.signUrl(
                BlobInfo.newBuilder(bucketName, fileKey).build(),
                10, TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }


    @Override
    public void deleteFile(String fileKey) {
        storage.delete(BlobId.of(bucketName, fileKey));
    }
}