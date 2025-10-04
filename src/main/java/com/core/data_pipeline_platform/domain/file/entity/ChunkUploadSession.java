package com.core.data_pipeline_platform.domain.file.entity;

import com.core.data_pipeline_platform.domain.file.enums.ChunkUploadStatus;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "file_id")
    private FileEntity file;

    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;
    
    @Column(name = "total_size", nullable = false)
    private Long totalSize;
    
    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;
    
    @Column(name = "completed_chunks", nullable = false)
    private Integer completedChunks;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChunkUploadStatus status;
    
    @Column(name = "chunk_info", columnDefinition = "JSON")
    private String chunkInfo;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void incrementCompletedChunks() {
        this.completedChunks++;
    }

    public void updateChunkInfo(int chunkIndex, ChunkUploadStatus status) {

        Map<String, String> chunkInfoMap = new HashMap<>();

        try{
            if(chunkInfo != null && !chunkInfo.isEmpty()){
                chunkInfoMap= objectMapper.readValue(chunkInfo, new TypeReference<Map<String, String>>() {});
            }

            chunkInfoMap.put(String.valueOf(chunkIndex), status.name());

            this.chunkInfo = objectMapper.writeValueAsString(chunkInfoMap);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updateStatus(ChunkUploadStatus status) {
        this.status = status;
    }

    public int getProgress() {
        return (int) (((double) completedChunks / totalChunks) * 100);
    }
}
