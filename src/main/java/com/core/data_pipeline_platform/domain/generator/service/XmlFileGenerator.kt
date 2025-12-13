package com.core.data_pipeline_platform.domain.generator.service

import com.core.data_pipeline_platform.domain.file.enums.FileType
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@Component
class XmlFileGenerator : FileGenerator {
    override fun generateFile(request: GenerateRequest): ByteArray {
        val xmlBuilder = StringBuilder()

        // XML 선언부
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        xmlBuilder.append("<sensorData>\n")

        // 센서 데이터 생성
        for (i in 0..<request.recordCount) {
            xmlBuilder.append("    <sensor>\n")
            xmlBuilder.append("        <sensorId>SENSOR_").append(i).append("</sensorId>\n")
            xmlBuilder.append("        <value>").append(String.format("%.2f", Math.random() * 100)).append("</value>\n")
            xmlBuilder.append("        <timestamp>").append(LocalDateTime.now()).append("</timestamp>\n")
            xmlBuilder.append("        <status>NORMAL</status>\n")
            xmlBuilder.append("    </sensor>\n")
        }

        xmlBuilder.append("</sensorData>")

        return xmlBuilder.toString().toByteArray(StandardCharsets.UTF_8)
    }

    override val supportedFileType: FileType?
        get() = FileType.XML
}