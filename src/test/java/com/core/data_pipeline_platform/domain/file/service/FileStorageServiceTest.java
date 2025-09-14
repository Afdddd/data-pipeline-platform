package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService 테스트")
class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    
    @TempDir
    Path tempDir;  // JUnit 5의 임시 디렉토리
    
    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        // uploadDir을 임시 디렉토리로 설정
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
    }

    @Test
    @DisplayName("JSON 파일 저장 - 성공")
    void storeFile_JsonFile_Success() throws IOException {
        // Given
        String content = "{\"name\": \"test\"}";
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.json",
            "application/json",
            content.getBytes()
        );
        FileType fileType = FileType.JSON;

        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginName()).isEqualTo("test.json");
        assertThat(result.getFileType()).isEqualTo(FileType.JSON);
        assertThat(result.getStoredName()).isNotNull();
        assertThat(result.getDirectoryName()).isNotNull();
        
        // 실제 파일이 생성되었는지 확인
        Path expectedPath = Paths.get(
            tempDir.toString(), 
            fileType.getExtension(), 
            result.getDirectoryName(),
            result.getStoredName() + "." + fileType.getExtension()
        );
        assertThat(Files.exists(expectedPath)).isTrue();
        
        // 파일 내용 확인
        String savedContent = Files.readString(expectedPath);
        assertThat(savedContent).isEqualTo(content);
    }

    @Test
    @DisplayName("CSV 파일 저장 - 성공")
    void storeFile_CsvFile_Success() throws IOException {
        // Given
        String content = "name,age\nJohn,30\nJane,25";
        MultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            "text/csv",
            content.getBytes()
        );
        FileType fileType = FileType.CSV;

        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginName()).isEqualTo("users.csv");
        assertThat(result.getFileType()).isEqualTo(FileType.CSV);
        
        // 디렉토리 구조 확인: uploads/csv/uuid/
        Path expectedPath = Paths.get(
            tempDir.toString(),
            "csv",
            result.getDirectoryName(),
            result.getStoredName() + "." + fileType.getExtension()
        );
        assertThat(Files.exists(expectedPath)).isTrue();
    }

    @Test
    @DisplayName("XML 파일 저장 - 성공")
    void storeFile_XmlFile_Success() throws IOException {
        // Given
        String content = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        MultipartFile file = new MockMultipartFile(
            "file",
            "data.xml",
            "application/xml",
            content.getBytes()
        );
        FileType fileType = FileType.XML;

        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginName()).isEqualTo("data.xml");
        assertThat(result.getFileType()).isEqualTo(FileType.XML);
        
        // 파일이 올바른 위치에 저장되었는지 확인
        Path expectedPath = Paths.get(
            tempDir.toString(),
            "xml",
            result.getDirectoryName(),
            result.getStoredName() + "." + fileType.getExtension()
        );
        assertThat(Files.exists(expectedPath)).isTrue();
        assertThat(Files.readString(expectedPath)).isEqualTo(content);
    }

    @Test
    @DisplayName("빈 파일 저장 - 성공")
    void storeFile_EmptyFile_Success() throws IOException {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "empty.json",
            "application/json",
            new byte[0]
        );
        FileType fileType = FileType.JSON;

        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginName()).isEqualTo("empty.json");
        
        // 빈 파일도 생성되는지 확인
        Path expectedPath = Paths.get(
            tempDir.toString(),
            fileType.getExtension(),
            result.getDirectoryName(),
            result.getStoredName() + "." + fileType.getExtension()
        );
        assertThat(Files.exists(expectedPath)).isTrue();
        assertThat(Files.size(expectedPath)).isEqualTo(0);
    }

    @Test
    @DisplayName("디렉토리 구조 확인")
    void storeFile_CheckDirectoryStructure() throws IOException {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.json",
            "application/json",
            "content".getBytes()
        );
        FileType fileType = FileType.JSON;

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

        // When
        FileEntity result = fileStorageService.storeFile(file, fileType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginName()).isEqualTo("테스트-파일_#1.json");
        assertThat(result.getFileType()).isEqualTo(FileType.JSON);
    }
}
