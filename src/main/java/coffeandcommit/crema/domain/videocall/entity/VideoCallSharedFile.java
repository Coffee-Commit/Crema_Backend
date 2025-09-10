package coffeandcommit.crema.domain.videocall.entity;

import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
    name = "video_call_shared_file",
    indexes = {
        @Index(name = "idx_video_call_shared_file_video_session", columnList = "video_session_id"),
        @Index(name = "idx_video_call_shared_file_uploaded_by", columnList = "uploaded_by_user_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_video_call_shared_file_session_imagekey", columnNames = {"video_session_id", "image_key"})
    }
)
public class VideoCallSharedFile extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_session_id", nullable = false)
    private VideoSession videoSession;
    
    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey; // S3 object key
    
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    @Column(name = "uploaded_by_user_id", nullable = false, length = 255)
    private String uploadedByUserId;
    
    @Column(name = "uploaded_by_name", nullable = false, length = 100)
    private String uploadedByName;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    
    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 파일 정보 업데이트
     */
    public void updateFileInfo(String fileName, Long fileSize, String contentType) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }
}