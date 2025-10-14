package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.file.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService 테스트")
class FileStorageServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;  // JUnit 5의 임시 디렉토리

    @TempDir
    Path chunkUploadDir;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileStorageService, "chunkUploadDir", chunkUploadDir.toString());
    }

    @Test
    @DisplayName("JSON 파일 저장 - 성공")
    void storeFile_JsonFile_Success() {
        // Given
        String content = "{\"name\": \"test\"}";
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.json",
            "application/json",
            content.getBytes()
        );
        FileType fileType = FileType.JSON;

        FileEntity entity = FileEntity.builder()
                .originName("test.json")
                .fileType(FileType.JSON)
                .storedName("stored-test")
                .directoryName("dir")
                .build();

        when(fileRepository.save(any(FileEntity.class))).thenReturn(entity);


        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);


        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginName()).isEqualTo("test.json");
        assertThat(result.getFileType()).isEqualTo(FileType.JSON);
        assertThat(result.getStoredName()).isNotNull();
        assertThat(result.getDirectoryName()).isNotNull();
    }

    @Test
    @DisplayName("CSV 파일 저장 - 성공")
    void storeFile_CsvFile_Success() {
        // Given
        String content = "name,age\nJohn,30\nJane,25";
        MultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            "text/csv",
            content.getBytes()
        );
        FileType fileType = FileType.CSV;

        FileEntity entity = FileEntity.builder()
                .originName("users.csv")
                .fileType(FileType.CSV)
                .storedName("stored-test")
                .directoryName("dir")
                .build();

        when(fileRepository.save(any(FileEntity.class))).thenReturn(entity);

        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginName()).isEqualTo("users.csv");
        assertThat(result.getFileType()).isEqualTo(FileType.CSV);
    }

    @Test
    @DisplayName("XML 파일 저장 - 성공")
    void storeFile_XmlFile_Success() {
        // Given
        String content = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        MultipartFile file = new MockMultipartFile(
            "file",
            "data.xml",
            "application/xml",
            content.getBytes()
        );
        FileType fileType = FileType.XML;

        FileEntity entity = FileEntity.builder()
                .originName("data.xml")
                .fileType(FileType.XML)
                .storedName("stored-test")
                .directoryName("dir")
                .build();

        when(fileRepository.save(any(FileEntity.class))).thenReturn(entity);

        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginName()).isEqualTo("data.xml");
        assertThat(result.getFileType()).isEqualTo(FileType.XML);
    }

    @Test
    @DisplayName("빈 파일 저장 - 성공")
    void storeFile_EmptyFile_Success() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "empty.json",
            "application/json",
            new byte[0]
        );
        FileType fileType = FileType.JSON;

        FileEntity entity = FileEntity.builder()
                .originName("empty.json")
                .fileType(FileType.JSON)
                .storedName("stored-test")
                .directoryName("dir")
                .build();

        when(fileRepository.save(any(FileEntity.class))).thenReturn(entity);

        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginName()).isEqualTo("empty.json");
    }

    @Test
    @DisplayName("디렉토리 구조 확인")
    void storeFile_CheckDirectoryStructure() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.json",
            "application/json",
            "content".getBytes()
        );
        FileType fileType = FileType.JSON;

        when(fileRepository.save(any(FileEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);

        // Then
        // 디렉토리 구조: tempDir/json/uuid/
        Path jsonDir = Paths.get(tempDir.toString(), "json");
        Path specificDir = Paths.get(tempDir.toString(), "json", result.getDirectoryName());
        
        assertThat(Files.exists(jsonDir)).isTrue();
        assertThat(Files.isDirectory(jsonDir)).isTrue();
        assertThat(Files.exists(specificDir)).isTrue();
        assertThat(Files.isDirectory(specificDir)).isTrue();
    }

    @Test
    @DisplayName("여러 파일 저장 시 각각 다른 디렉토리에 저장")
    void storeFile_MultipleFiles_DifferentDirectories() {
        // Given
        MultipartFile file1 = new MockMultipartFile(
            "file1", "test1.json", "application/json", "content1".getBytes()
        );
        MultipartFile file2 = new MockMultipartFile(
            "file2", "test2.json", "application/json", "content2".getBytes()
        );
        FileType fileType = FileType.JSON;

        FileEntity testEntity1 = FileEntity.builder()
                .originName("test1.json")
                .fileType(FileType.JSON)
                .storedName("stored-test-1")
                .directoryName("dir1")
                .build();


        FileEntity testEntity2 = FileEntity.builder()
                .originName("test2.json")
                .fileType(FileType.JSON)
                .storedName("stored-test-2")
                .directoryName("dir2")
                .build();

        when(fileRepository.save(any(FileEntity.class)))
                .thenReturn(testEntity1)
                .thenReturn(testEntity2);

        // When
        FileEntity result1 = fileStorageService.storeFile(file1, fileType);
        FileEntity result2 = fileStorageService.storeFile(file2, fileType);

        // Then
        assertThat(result1.getDirectoryName()).isNotEqualTo(result2.getDirectoryName());
        assertThat(result1.getStoredName()).isNotEqualTo(result2.getStoredName());
    }

    @Test
    @DisplayName("특수문자가 포함된 파일명 처리")
    void storeFile_SpecialCharactersInFileName_Success() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "테스트-파일_#1.json",
            "application/json",
            "content".getBytes()
        );
        FileType fileType = FileType.JSON;

        FileEntity entity = FileEntity.builder()
                .originName("테스트-파일_#1.json")
                .fileType(FileType.JSON)
                .storedName("stored-test")
                .directoryName("dir")
                .build();

        when(fileRepository.save(any(FileEntity.class))).thenReturn(entity);

        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginName()).isEqualTo("테스트-파일_#1.json");
        assertThat(result.getFileType()).isEqualTo(FileType.JSON);
    }
}
