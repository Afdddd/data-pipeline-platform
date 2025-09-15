package com.core.data_pipeline_platform.domain.generator.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SensorData {
    private String sensorId;
    private double value;
    private String timestamp;
    private String status;
}
