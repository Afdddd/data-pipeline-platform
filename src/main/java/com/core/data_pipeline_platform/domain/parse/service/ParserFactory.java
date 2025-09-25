package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ParserFactory {

    private final Map<FileType, DataParser> parsers;

    public ParserFactory(List<DataParser> parserList) {
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(
                        DataParser::getSupportedFileType,
                        parser -> parser
                ));

    }

    public DataParser getParser(FileType fileType) {
        return parsers.get(fileType);
    }

}
