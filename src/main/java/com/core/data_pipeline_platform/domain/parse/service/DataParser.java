package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;


import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface DataParser {
    List<Map<String, Object>> parseData(FileType fileType, InputStream inputStream);
    
}
