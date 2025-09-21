package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CsvDataParser implements DataParser{

    @Override
    public List<Map<String, Object>> parseData(FileType fileType, InputStream inputStream) {
        List<Map<String, Object>> records = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        
        try{
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV 파일이 비어있습니다.");
        }
        String[] headers = headerLine.split(",");

        String line;
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].startsWith("\"") && columns[i].endsWith("\"")) {
                    columns[i] = columns[i].substring(1, columns[i].length() - 1);
                }
            }

            if (columns.length != headers.length) {
                continue;
            }
            Map<String, Object> record = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                record.put(headers[i], columns[i]);
            }
            records.add(record);
        }
        return records;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CSV 파싱 실패");
        }
        
    }
}
