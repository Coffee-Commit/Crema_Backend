package coffeandcommit.crema.global.validation;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public class ValidatedFile {
    private final MultipartFile file;

    private ValidatedFile(MultipartFile file) {
        this.file = file;
    }

    public static ValidatedFile of(MultipartFile file, FileType fileType, FileValidator validator) {
        validator.validate(file, fileType);
        return new ValidatedFile(file);
    }

    public String getOriginalFilename() {
        return file.getOriginalFilename();
    }

    public String getContentType() {
        return file.getContentType();
    }

    public long getSize() {
        return file.getSize();
    }

    public byte[] getBytes() throws IOException {
        return file.getBytes();
    }

    public MultipartFile getFile() {
        return file;
    }
}
