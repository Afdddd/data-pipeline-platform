package com.core.data_pipeline_platform.domain.generator.service

import com.core.data_pipeline_platform.domain.file.enums.FileType
import org.springframework.stereotype.Component
import java.util.function.Function
import java.util.stream.Collectors

@Component
class GeneratorFactory(generatorList: MutableList<FileGenerator?>) {
    private val generators: MutableMap<FileType, FileGenerator> = generatorList.stream()
        .collect(
            Collectors.toMap(
                FileGenerator::supportedFileType,
                Function { fileGenerator: FileGenerator? -> fileGenerator }
            ))

    fun getFileGenerator(fileType: FileType): FileGenerator {
        val fileGenerator: FileGenerator = generators[fileType]!!
        return fileGenerator
    }
}
