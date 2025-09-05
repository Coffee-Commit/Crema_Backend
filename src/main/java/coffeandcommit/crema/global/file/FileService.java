package coffeandcommit.crema.global.file;

import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.validation.FileType;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     *
     * 파일을 서버를 통해 클라우드 스토리지에 직접 업로드합니다.
     * (이미지, PDF 등 모든 파일 공용)
     * @param file 업로드할 파일
     * @param fileType 업로드 타입 (예: FileType.IMAGE)
     * @param folder 저장될 폴더명 (예: "profile-images")
     * @param userId 요청한 사용자의 ID
     * @return 업로드된 파일 정보 (파일 키, URL 등)
     */
    FileUploadResponse uploadFile(MultipartFile file, FileType fileType, String folder, String userId);
}
