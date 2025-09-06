package coffeandcommit.crema.global.file.impl;

import coffeandcommit.crema.global.file.FileService;
import coffeandcommit.crema.global.storage.StorageService;
import coffeandcommit.crema.global.storage.dto.FileUploadResponse;
import coffeandcommit.crema.global.validation.FileType;
import coffeandcommit.crema.global.validation.FileValidator;
import coffeandcommit.crema.global.validation.ValidatedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final StorageService storageService;
    private final FileValidator fileValidator;

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, FileType fileType, String folder, String userId) {
        ValidatedFile validatedFile = ValidatedFile.of(file, fileType, fileValidator);

        return storageService.uploadFile(validatedFile, folder, userId);
    }
}