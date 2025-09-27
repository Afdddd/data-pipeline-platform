package com.core.data_pipeline_platform.domain.file.service;

import com.core.data_pipeline_platform.domain.file.entity.FileEntity;
import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.file.repository.FileRepository;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.core.data_pipeline_platform.domain.parse.repository.ParsedDataRepository;
import com.core.data_pipeline_platform.domain.parse.service.DataParsingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)  // Mockito 사용을 위한 어노테이션
@DisplayName("FileUploadService 테스트")
class FileUploadServiceTest {

    @Mock  // Mock 객체 생성
    private FileRepository fileRepository;
    
    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DataParsingService dataParsingService;

    @Mock
    private ParsedDataRepository parsedDataRepository;

    @InjectMocks  // Mock 객체들을 주입받는 실제 테스트 대상
    private FileUploadService fileUploadService;

    private MultipartFile mockFile;

    @BeforeEach  // 각 테스트 실행 전에 실행
    void setUp() {
        // 테스트용 Mock 파일 생성
        mockFile = new MockMultipartFile(
            "file",                    // 파라미터 이름
            "test.json",              // 원본 파일명
            "application/json",       // Content Type
            "test content".getBytes() // 파일 내용
        );
    }

    @Test
    @DisplayName("정상적인 파일 업로드 - 성공")
    void uploadFile_Success() throws IOException {
        // Given (준비)
        given(fileRepository.existsByOriginName("test.json"))
            .willReturn(false);  // 중복 파일 없음
        
        FileEntity storageEntity = FileEntity.builder()
            .fileType(FileType.JSON)
            .originName("test.json")
            .directoryName("uuid-directory")
            .storedName("uuid-stored")
            .build();
        
        given(fileStorageService.storeFile(mockFile, FileType.JSON))
            .willReturn(storageEntity);
        
        FileEntity savedFile = FileEntity.builder()
            .id(1L)
            .fileType(FileType.JSON)
            .originName("test.json")
            .directoryName("uuid-directory")
            .storedName("uuid-stored")
            .build();
        
        given(fileRepository.save(storageEntity))
            .willReturn(savedFile);

        ParsedDataEntity parsedData = ParsedDataEntity.builder()
                .id(1L)
                .file(savedFile)
                .data("{name : test")
                .build();

        given(dataParsingService.parseToEntity(any(FileType.class), any(InputStream.class), any(FileEntity.class)))
                .willReturn(parsedData);

        given(parsedDataRepository.save(any(ParsedDataEntity.class)))
                .willReturn(parsedData);

        // When (실행)
        Long result = fileUploadService.uploadFile(mockFile);

        // Then (검증)
        assertThat(result).isEqualTo(1L);
        
        // 메서드 호출 검증
        then(fileRepository).should().existsByOriginName("test.json");
        then(fileStorageService).should().storeFile(mockFile, FileType.JSON);
        then(fileRepository).should().save(storageEntity);
    }

    @Test
    @DisplayName("지원하지 않는 파일 형식 - 예외 발생")
    void uploadFile_UnsupportedFileType_ThrowsException() {
        // Given
        MultipartFile unsupportedFile = new MockMultipartFile(
            "file", 
            "test.txt",  // 지원하지 않는 확장자
            "text/plain", 
            "content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileUploadService.uploadFile(unsupportedFile))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getReason())
            .isEqualTo("지원하지 않는 형식입니다.");
        
        // Repository 호출되지 않았는지 검증
        then(fileRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("중복 파일명 - 예외 발생")
    void uploadFile_DuplicateFileName_ThrowsException() {
        // Given
        given(fileRepository.existsByOriginName("test.json"))
            .willReturn(true);  // 중복 파일 존재

        // When & Then
        assertThatThrownBy(() -> fileUploadService.uploadFile(mockFile))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getReason())
            .isEqualTo("이미 존재하는 파일 이름입니다.");
        
        // save와 storage 메서드는 호출되지 않아야 함
        then(fileRepository).should(never()).save(any());
        then(fileStorageService).should(never()).storeFile(any(), any());
    }

    @Test
    @DisplayName("확장자가 없는 파일 - 예외 발생")
    void uploadFile_NoExtension_ThrowsException() {
        // Given
        MultipartFile noExtensionFile = new MockMultipartFile(
            "file", 
            "testfile",  // 확장자 없음
            "application/octet-stream", 
            "content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileUploadService.uploadFile(noExtensionFile))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getReason())
            .isEqualTo("지원하지 않는 형식입니다.");
    }

    @Test
    @DisplayName("DB 제약 조건 위반 - CONFLICT 예외발생")
    void uploadFile_DataIntegrityViolation_ThrowsConflict() {
        // Given
        given(fileRepository.existsByOriginName("test.json"))
                .willReturn(false);

        FileEntity storageEntity = FileEntity.builder()
                .fileType(FileType.JSON)
                .originName("test.json")
                .directoryName("uuid-directory")
                .storedName("uuid-storedName")
                .build();

        given(fileStorageService.storeFile(mockFile, FileType.JSON))
                .willReturn(storageEntity);

        given(fileRepository.save(storageEntity))
                .willThrow(new DataIntegrityViolationException("UNIQUE constraint violation"));

        // When & Then
        assertThatThrownBy(() -> fileUploadService.uploadFile(mockFile))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseEx = (ResponseStatusException) ex;
                    assertThat(responseEx.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(responseEx.getReason()).isEqualTo("이미 존재하는 파일 이름입니다.");
                });

        then(fileRepository).should().existsByOriginName("test.json");
        then(fileStorageService).should().storeFile(mockFile, FileType.JSON);
        then(fileRepository).should().save(storageEntity);

    }

    @Test
    @DisplayName("Pre-check와 DB 제약조건 모두 통과 - 정상 처리")
    void uploadFile_NoRaceCondition_Success() {
        // Given
        given(fileRepository.existsByOriginName("unique.json"))
                .willReturn(false);

        FileEntity storageEntity = FileEntity.builder()
                .fileType(FileType.JSON)
                .originName("unique.json")
                .directoryName("uuid-directory")
                .storedName("uuid-stored")
                .build();

        given(fileStorageService.storeFile(any(), eq(FileType.JSON)))
                .willReturn(storageEntity);

        FileEntity savedFile = FileEntity.builder()
                .id(1L)
                .fileType(FileType.JSON)
                .originName("unique.json")
                .directoryName("uuid-directory")
                .storedName("uuid-stored")
                .build();

        given(fileRepository.save(storageEntity))
                .willReturn(savedFile);  // DB 제약조건도 통과

        ParsedDataEntity parsedData = ParsedDataEntity.builder()
                .id(1L)
                .file(savedFile)
                .data("{name : test")
                .build();

        given(dataParsingService.parseToEntity(any(FileType.class), any(InputStream.class), any(FileEntity.class)))
                .willReturn(parsedData);

        given(parsedDataRepository.save(any(ParsedDataEntity.class)))
                .willReturn(parsedData);


        MultipartFile uniqueFile = new MockMultipartFile(
                "file", "unique.json", "application/json", "content".getBytes()
        );

        // When
        Long result = fileUploadService.uploadFile(uniqueFile);

        // Then
        assertThat(result).isEqualTo(1L);

        then(fileRepository).should().existsByOriginName("unique.json");
        then(fileStorageService).should().storeFile(any(), eq(FileType.JSON));
        then(fileRepository).should().save(storageEntity);
    }
}
