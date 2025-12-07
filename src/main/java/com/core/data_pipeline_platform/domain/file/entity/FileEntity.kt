package com.core.data_pipeline_platform.domain.file.entity

import com.core.data_pipeline_platform.domain.file.enums.FileProcessingStatus
import com.core.data_pipeline_platform.domain.file.enums.FileType
import jakarta.persistence.*
import lombok.Getter

@Entity
@Getter
class FileEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,


    @Column(name = "origin_name", nullable = false, length = 255, unique = true)
    var originName: String? = null,

    @Column(name = "directory_name", nullable = false)
    var directoryName: String? = null,

    @Column(name = "stored_name", nullable = false, length = 255)
    var storedName: String? = null,

    @Enumerated(EnumType.STRING)
    var fileType: FileType? = null
) {

    @Enumerated(EnumType.STRING)
    var processingStatus: FileProcessingStatus? = FileProcessingStatus.PENDING

    var errorMessage: String? = null

    fun updateStatus(processingStatus: FileProcessingStatus?) {
        this.processingStatus = processingStatus
    }

    fun updateStatus(processingStatus: FileProcessingStatus?, errorMessage: String?) {
        this.processingStatus = processingStatus
        this.errorMessage = errorMessage
    }
}
