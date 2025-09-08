package coffeandcommit.crema.global.storage;

import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.validation.ValidatedFile;

public interface StorageService {

    /**
     *
     * 파일을 서버를 통해 클라우드 스토리지에 직접 업로드합니다.
     * (이미지, PDF 등 모든 파일 공용)
     * @param file 업로드할 파일
     * @param folder 저장될 폴더명 (예: "profile-images")
     * @param userId 요청한 사용자의 ID
     * @return 업로드된 파일 정보 (파일 키, URL 등)
     */
    FileUploadResponse uploadFile(ValidatedFile file, String folder, String userId);


    /**
     * 파일 조회/다운로드용 URL을 생성합니다.
     * @param fileKey 파일의 전체 경로 (예: "profile-images/user123_profile.jpg")
     * @return 유효기간이 있는 조회용 URL
     */
    String generateViewUrl(String fileKey);


    /**
     * 파일을 삭제합니다.
     * @param fileKey 삭제할 파일의 전체 경로
     */
    void deleteFile(String fileKey);
}