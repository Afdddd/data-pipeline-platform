package com.core.data_pipeline_platform.common.cli;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class HelpCommands {

    @ShellMethod(key = "commands", value = "Show all available commands with examples")
    public String showCommands() {
        return """
                Data Pipeline Platform CLI Commands
                
                파일 업로드:
                   upload <파일경로>                    - 일반 파일 업로드
                   
                 청크 업로드 (대용량 파일):
                    chunk-upload <파일경로> [--chunk-size] - 대용량 파일 청크 업로드 (자동 완료)
                   
                파싱된 데이터 관리:
                   list [--page] [--size]               - 파싱된 데이터 목록
                   show <파일ID>                        - 상세 정보 조회
                   delete-parsed <파일ID>               - 파싱된 데이터 삭제
                   count                                - 총 개수 조회
                   
                파일 생성:
                   generate <포맷> [--name] [--rows] [--output-dir] - 테스트 파일 생성
                   
                 사용 예시:
                    upload ./test.csv
                    chunk-upload ./large-file.json --chunk-size 2097152
                    list --page 0 --size 5
                    show 1
                    generate json --name sensor-data --rows 1000
                   
                도움말:
                   help                                 - Spring Shell 기본 도움말
                   commands                             - 이 명령어 목록
                """;
    }

    @ShellMethod(key = "status", value = "Show system status and statistics")
    public String showStatus() {
        return """
                Data Pipeline Platform Status
                
                System: Running
                Supported Formats: JSON, CSV, XML, BIN
                Features: File Upload, Chunk Upload, Data Parsing, File Generation
                
                Quick Start:
                   1. generate json --name test --rows 100
                   2. upload ./test.json
                   3. list
                   4. show 1
                """;
    }
}
