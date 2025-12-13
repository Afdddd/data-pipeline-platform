package com.core.data_pipeline_platform.domain.generator.service

import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest
import com.core.data_pipeline_platform.domain.generator.model.SensorData
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@Component
class CsvFileGenerator : FileGenerator {
    override fun generateFile(request: GenerateRequest): ByteArray {
        val csvBuilder = StringBuilder()
        val fields = SensorData::class.java.getDeclaredFields()

        for (i in fields.indices) {
            csvBuilder.append(fields[i].name)
            if (i < fields.size - 1) {
                csvBuilder.append(",")
            }
        }
        csvBuilder.append("\n")


        for (i in 0..<request.recordCount) {
            csvBuilder.append("SENSOR_")
                .append(i)
                .append(",")
                .append(Math.random() * 100)
                .append(",")
                .append(LocalDateTime.now())
                .append(",")
                .append("Status")
                .append("\n")
        }

        return csvBuilder.toString().toByteArray(StandardCharsets.UTF_8)
    }

    override val supportedFileType: FileType?
        get() = FileType.CSV
}
