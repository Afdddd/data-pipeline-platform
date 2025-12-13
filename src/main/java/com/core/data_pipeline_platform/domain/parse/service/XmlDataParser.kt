package com.core.data_pipeline_platform.domain.parse.service

import com.core.data_pipeline_platform.domain.file.enums.FileType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

@Service
class XmlDataParser : DataParser {
    override fun parseData(fileType: FileType, inputStream: InputStream): MutableList<MutableMap<String, Any>> {
        if (fileType != FileType.XML) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 타입 불일치: XML이어야 합니다.")
        }

        val records: MutableList<MutableMap<String, Any>> = ArrayList()

        try {
            // DOM 파서 생성
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(inputStream)


            // 루트 태그 검증
            val root = document.documentElement
            if ("sensorData" != root.tagName) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 XML 형식이 아닙니다. 루트 태그는 'sensorData'여야 합니다.")
            }


            // sensor 요소들 찾기
            val sensorNodes = document.getElementsByTagName("sensor")


            // sensor 데이터 존재 검증
            if (sensorNodes.length == 0) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "sensor 데이터가 없습니다.")
            }

            // 각 sensor 요소를 Map으로 변환
            for (i in 0..<sensorNodes.length) {
                val sensorElement = sensorNodes.item(i) as Element
                val record: MutableMap<String, Any> = HashMap()

                record[SENSOR_ID] = getElementContent(sensorElement, SENSOR_ID)
                record[VALUE] = getElementContent(sensorElement, VALUE)
                record[TIMESTAMP] = getElementContent(sensorElement, TIMESTAMP)
                record[STATUS] = getElementContent(sensorElement, STATUS)

                records.add(record)
            }

            return records
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "XML 파싱 실패", e)
        }
    }

    override val supportedFileType: FileType
        get() = FileType.XML

    private fun getElementContent(element: Element, tagName: String): Any {
        val nodes = element.getElementsByTagName(tagName)
        if (nodes.getLength() == 0) {
            return ""
        }
        return nodes.item(0).textContent
    }

    companion object {
        private const val SENSOR_ID = "sensorId"
        private const val VALUE = "value"
        private const val TIMESTAMP = "timestamp"
        private const val STATUS = "status"
    }
}