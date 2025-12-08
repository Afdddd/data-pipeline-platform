package com.core.data_pipeline_platform.domain.parse.entity

import com.core.data_pipeline_platform.domain.file.entity.FileEntity
import jakarta.persistence.*

@Entity
class ParsedDataEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne
    var file: FileEntity,

    @Column(columnDefinition = "json", nullable = false)
    var data: String
){

}
