package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ParserFactoryTest {

    private ParserFactory parserFactory;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ArrayList<DataParser> parsers = new ArrayList<>();
        parsers.add(new JsonDataParser(objectMapper));
        parserFactory = new ParserFactory(parsers);
    }

    @Test
    void getParser_invalid_fileType() {
        // Given
        FileType invalidFileType = FileType.XML;

        // When & Then
        assertThrows(ResponseStatusException.class, () -> parserFactory.getParser(invalidFileType));
    }

    @Test
    void getParser_validFileType() {
        // Given
        FileType validFileType = FileType.JSON;

        // When
        DataParser result = parserFactory.getParser(validFileType);

        // Then
        assertNotNull(result);
        assertEquals(FileType.JSON, result.getSupportedFileType());
    }

}