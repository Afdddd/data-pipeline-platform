package com.core.data_pipeline_platform.domain.parse.service;

import com.core.data_pipeline_platform.domain.file.enums.FileType;


import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface DataParser {
    /**
      * 지정된 파일 타입의 데이터를 파싱한다.
      * - fileType: 구현체가 지원하는 타입이어야 하며, 불일치 시 400을 던진다.
      * - inputStream: null 불가. 호출자가 생명주기/close를 관리한다.
      */
    List<Map<String, Object>> parseData(FileType fileType, InputStream inputStream);

}
