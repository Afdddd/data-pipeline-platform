package com.core.data_pipeline_platform.domain.generator.service

import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest
import com.core.data_pipeline_platform.domain.generator.model.SensorData
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JsonFileGenerator(private val objectMapper: ObjectMapper) : FileGenerator {

    override fun generateFile(request: GenerateRequest): ByteArray {
        val sensorDataList: MutableList<SensorData> = ArrayList()

        for (i in 0..<request.recordCount) {
            sensorDataList.add(
                SensorData(
                    sensorId = "SENSOR_$i",
                    value = Math.random() * 100,
                    timestamp = LocalDateTime.now().toString(),
                    status = "NORMAL"
                )
            )
        }

        try {
            return objectMapper.writeValueAsBytes(sensorDataList)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("JSON 생성 실패", e)
        }
    }

    override val supportedFileType: FileType?
        get() = FileType.JSON
}
