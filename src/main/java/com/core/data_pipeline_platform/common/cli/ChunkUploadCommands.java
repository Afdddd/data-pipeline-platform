package com.core.data_pipeline_platform.common.cli;

import com.core.data_pipeline_platform.domain.file.dto.*;
import com.core.data_pipeline_platform.domain.file.service.ChunkUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@ShellComponent
@RequiredArgsConstructor
public class ChunkUploadCommands {

    private final ChunkUploadService chunkUploadService;

    @ShellMethod(key = "chunk-upload", value = "Upload large file using chunks")
    public String chunkUpload(
            String filePath,
            @ShellOption(defaultValue = "1048576") int chunkSize  // 1MB default
    ) {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                return "파일을 찾을 수 없습니다: " + filePath;
            }

            long fileSize = Files.size(path);
            String fileName = path.getFileName().toString();
            int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

            System.out.println("청크 업로드 시작...");
            System.out.printf("파일: %s (%.2f MB)\n", fileName, fileSize / (1024.0 * 1024.0));
            System.out.printf("청크 크기: %.2f KB, 총 %d개\n\n", chunkSize / 1024.0, totalChunks);

            // 1단계: 세션 시작
            ChunkUploadStartRequest startRequest = ChunkUploadStartRequest.builder()
                    .fileName(fileName)
                    .totalChunks(totalChunks)
                    .totalSize(fileSize)
                    .build();

            ChunkUploadStartResponse startResponse = chunkUploadService.startUpload(startRequest);
            String sessionId = startResponse.sessionId();
            
            System.out.printf("세션 시작됨: %s\n", sessionId);

            // 2단계: 청크 업로드
            byte[] fileContent = Files.readAllBytes(path);
            
            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, fileContent.length);
                byte[] chunkData = new byte[end - start];
                System.arraycopy(fileContent, start, chunkData, 0, end - start);

                String encodedChunk = Base64.getEncoder().encodeToString(chunkData);

                ChunkUploadRequest uploadRequest = ChunkUploadRequest.builder()
                        .sessionId(sessionId)
                        .chunkIndex(i)
                        .chunkData(encodedChunk.getBytes())
                        .build();

                ChunkUploadResponse uploadResponse = chunkUploadService.upload(uploadRequest);

                // 진행률 표시
                int progress = (int) ((i + 1.0) / totalChunks * 100);
                String progressBar = createProgressBar(progress);
                System.out.printf("\r%s %d%% (%d/%d)", progressBar, progress, i + 1, totalChunks);
            }

            System.out.println("\n\n청크 업로드 완료! 파일 병합 중...");

            // 3단계: 완료 처리
            ChunkUploadCompleteResponse completeResponse = chunkUploadService.completeUpload(sessionId);

            return String.format(
                "청크 업로드 성공!\n" +
                "   File ID: %s\n" +
                "   Session: %s\n" +
                "   총 처리 시간: 완료",
                completeResponse.fileId(),
                sessionId
            );

        } catch (IOException e) {
            return "파일 처리 실패: " + e.getMessage();
        } catch (Exception e) {
            return "청크 업로드 실패: " + e.getMessage();
        }
    }

    private String createProgressBar(int progress) {
        int barLength = 20;
        int filled = (int) (progress / 100.0 * barLength);
        StringBuilder bar = new StringBuilder("[");
        
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");
        
        return bar.toString();
    }
}
