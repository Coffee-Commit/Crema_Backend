package coffeandcommit.crema.domain.videocall.repository;

import coffeandcommit.crema.domain.videocall.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    
    @Query("SELECT f FROM UploadedFile f WHERE f.sessionId = :sessionId ORDER BY f.uploadedAt DESC")
    List<UploadedFile> findBySessionIdOrderByUploadedAtDesc(@Param("sessionId") String sessionId);
    
    @Query("SELECT f FROM UploadedFile f ORDER BY f.uploadedAt DESC")
    List<UploadedFile> findAllByOrderByUploadedAtDesc();
}