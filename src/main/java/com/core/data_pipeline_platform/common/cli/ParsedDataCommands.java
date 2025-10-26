package com.core.data_pipeline_platform.common.cli;

import com.core.data_pipeline_platform.domain.parse.dto.ParsedDataResponse;
import com.core.data_pipeline_platform.domain.parse.entity.ParsedDataEntity;
import com.core.data_pipeline_platform.domain.parse.service.ParsedDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;
import java.util.Map;

@ShellComponent
@RequiredArgsConstructor
public class ParsedDataCommands {

    private final ParsedDataService parsedDataService;

    @ShellMethod(key = "list", value = "List all parsed data with pagination")
    public String listParsedData(
            @ShellOption(defaultValue = "0") int page,
            @ShellOption(defaultValue = "10") int size
    ) {
        try {
            Page<ParsedDataEntity> parsedData = parsedDataService.getAllParsedData(
                PageRequest.of(page, size)
            );

            if (parsedData.isEmpty()) {
                return "파싱된 데이터가 없습니다.";
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("파싱된 데이터 목록 (페이지 %d/%d, 총 %d개)\n\n", 
                page + 1, parsedData.getTotalPages(), parsedData.getTotalElements()));

            result.append("┌────────┬─────────────────────┬──────────┐\n");
            result.append("│   ID   │      File Name      │   Type   │\n");
            result.append("├────────┼─────────────────────┼──────────┤\n");

            for (ParsedDataEntity entity : parsedData.getContent()) {
                result.append(String.format("│ %-6d │ %-19s │ %-8s │\n",
                    entity.getId(),
                    truncate(entity.getFile().getOriginName(), 19),
                    entity.getFile().getFileType()
                ));
            }

            result.append("└────────┴─────────────────────┴──────────┘\n");
            
            if (parsedData.hasNext()) {
                result.append(String.format("\n다음 페이지: list --page %d --size %d", page + 1, size));
            }

            return result.toString();

        } catch (Exception e) {
            return "목록 조회 실패: " + e.getMessage();
        }
    }

    @ShellMethod(key = "show", value = "Show parsed data details by file ID")
    public String showParsedData(Long fileId) {
        try {
            ParsedDataEntity entity = parsedDataService.getParsedDataByFileId(fileId);
            List<Map<String, Object>> data = parsedDataService.getParsedDataAsMap(fileId);

            StringBuilder result = new StringBuilder();
            result.append("파싱된 데이터 상세 정보\n\n");
            result.append(String.format("File ID: %d\n", entity.getFile().getId()));
            result.append(String.format("File Name: %s\n", entity.getFile().getOriginName()));
            result.append(String.format("File Type: %s\n", entity.getFile().getFileType()));
            result.append(String.format("Data Count: %d개\n", data.size()));

            if (!data.isEmpty()) {
                result.append("데이터 미리보기 (처음 3개):\n");
                for (int i = 0; i < Math.min(3, data.size()); i++) {
                    result.append(String.format("[%d] %s\n", i + 1, data.get(i)));
                }
                
                if (data.size() > 3) {
                    result.append(String.format("... 외 %d개 더\n", data.size() - 3));
                }
            }

            return result.toString();

        } catch (Exception e) {
            return "상세 조회 실패: " + e.getMessage();
        }
    }

    @ShellMethod(key = "delete-parsed", value = "Delete parsed data by file ID")
    public String deleteParsedData(Long fileId) {
        try {
            parsedDataService.deleteParsedData(fileId);
            return String.format("파일 ID %d의 파싱된 데이터가 삭제되었습니다.", fileId);
        } catch (Exception e) {
            return "삭제 실패: " + e.getMessage();
        }
    }

    @ShellMethod(key = "count", value = "Get total count of parsed data")
    public String getParsedDataCount() {
        try {
            long count = parsedDataService.getParsedDataCount();
            return String.format("총 파싱된 데이터 개수: %d개", count);
        } catch (Exception e) {
            return "개수 조회 실패: " + e.getMessage();
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }
}
