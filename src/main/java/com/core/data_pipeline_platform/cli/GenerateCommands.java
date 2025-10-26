package com.core.data_pipeline_platform.cli;

import com.core.data_pipeline_platform.domain.file.enums.FileType;
import com.core.data_pipeline_platform.domain.generator.dto.GenerateRequest;
import com.core.data_pipeline_platform.domain.generator.service.*;
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
public class GenerateCommands {

    private final GeneratorFactory factory;

    @ShellMethod(key = "generate", value = "Generate a test file")
    public String generate(
            String format,  // json, csv, xml, bin
            @ShellOption(defaultValue = "test-file") String name,
            @ShellOption(defaultValue = "100") int rows,
            @ShellOption(defaultValue = "./") String outputDir
    ) {
        try {
            GenerateRequest request = new GenerateRequest(name, rows);
            FileType fileType = FileType.fromExtension(format);
            byte[] content = factory.getFileGenerator(fileType).generateFile(request);

            // 파일 저장
            Path outputPath = Paths.get(outputDir, name + "." + format.toLowerCase());
            Files.write(outputPath, content);

            return String.format(
                    "파일 생성 완료!\n" +
                    "   Format: %s\n" +
                    "   Rows: %d\n" +
                    "   Size: %.2f KB\n" +
                    "   Path: %s",
                    format.toUpperCase(),
                    rows,
                    content.length / 1024.0,
                    outputPath.toAbsolutePath()
            );

        } catch (IllegalArgumentException e) {
            return " " + e.getMessage();
        } catch (IOException e) {
            return "파일 저장 실패: " + e.getMessage();
        } catch (Exception e) {
            return "생성 실패: " + e.getMessage();
        }
    }
}

