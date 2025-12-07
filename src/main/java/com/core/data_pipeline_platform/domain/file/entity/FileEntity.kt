
package com.core.data_pipeline_platform.domain.file.entity;

import com.core.data_pipeline_platform.domain.file.enums.FileProcessingStatus;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Column(name = "origin_name", nullable = false, length = 255, unique = true)
    private String originName;

    @Column(name = "directory_name", nullable = false)
    private String directoryName;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Enumerated(EnumType.STRING)
    private FileProcessingStatus processingStatus = FileProcessingStatus.PENDING;

    private String errorMessage;

    public void updateStatus(FileProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public void updateStatus(FileProcessingStatus processingStatus, String errorMessage) {
        this.processingStatus = processingStatus;
        this.errorMessage = errorMessage;
    }

}
