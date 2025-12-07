package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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
        DataParser parser = parsers.get(fileType);
        if(parser == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일형식입니다.");
        }
        return parser;
    }

}
