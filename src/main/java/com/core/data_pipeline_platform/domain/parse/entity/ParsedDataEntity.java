package com.core.data_pipeline_platform.domain.parse.entity;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ParsedDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private FileEntity file;

    @Column(columnDefinition = "json", nullable = false)
    private String data;
}
