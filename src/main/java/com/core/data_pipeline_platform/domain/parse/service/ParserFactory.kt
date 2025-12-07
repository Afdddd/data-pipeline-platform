package com.core.data_pipeline_platform.domain.parse.service

import com.core.data_pipeline_platform.domain.file.enums.FileType
import org.springframework.stereotype.Component
import java.util.function.Function
import java.util.stream.Collectors

@Component
class ParserFactory(parserList: MutableList<DataParser?>) {
    private val parsers: MutableMap<FileType?, DataParser> = parserList.stream()
        .collect(
            Collectors.toMap(
                DataParser::supportedFileType,
                Function { parser: DataParser? -> parser }
            ))

    fun getParser(fileType: FileType?): DataParser {
        val parser: DataParser = parsers.get(fileType)!!
        return parser
    }
}
