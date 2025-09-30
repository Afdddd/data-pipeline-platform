package com.core.data_pipeline_platform.domain.file.entity;

import com.core.data_pipeline_platform.domain.file.enums.ChunkUploadStatus;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 청크 업로드 세션을 관리하는 엔티티
 */
@Entity
@Table(name = "chunk_upload_session")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChunkUploadSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "file_id")
    private FileEntity file;
    
    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;
    
    @Column(name = "total_size", nullable = false)
    private Long totalSize;
    
    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;
    
    @Column(name = "completed_chunks", nullable = false)
    private Integer completedChunks = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChunkUploadStatus status = ChunkUploadStatus.PENDING;
    
    @Column(name = "chunk_info", columnDefinition = "JSON")
    private String chunkInfo = "{}";
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
