package com.core.data_pipeline_platform.common.cli;

import com.core.data_pipeline_platform.domain.file.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ShellComponent
@RequiredArgsConstructor
public class uploadCommands {

    private final FileUploadService fileUploadService;

    @ShellMethod(key = "upload", value = "Upload a file for processing")
    public String uploadFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return "파일을 찾을 수 없습니다: " + filePath;
            }

            Long fileId = fileUploadService.uploadFile(path);
            
            long fileSize = Files.size(path);
            String fileName = path.getFileName().toString();
            String contentType = Files.probeContentType(path);
            
            return String.format(
                "파일 업로드 성공!\n" +
                "   File ID: %d\n" +
                "   Name: %s\n" +
                "   Size: %.2f KB\n" +
                "   Type: %s",
                fileId,
                fileName,
                fileSize / 1024.0,
                contentType != null ? contentType : "unknown"
            );

        } catch (IOException e) {
            return "파일 읽기 실패: " + e.getMessage();
        } catch (Exception e) {
            return "업로드 실패: " + e.getMessage();
        }
    }
}
