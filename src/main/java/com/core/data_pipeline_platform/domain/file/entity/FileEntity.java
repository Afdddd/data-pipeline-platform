
package com.core.data_pipeline_platform.domain.file.entity;

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

    @Column(name = "origin_name", nullable = false, length = 100)
    private String originName;

    @Column(name = "directory_name", nullable = false)
    private String directoryName;

    @Column(name = "stored_name", nullable = false, length = 100)
    private String storedName;
}
