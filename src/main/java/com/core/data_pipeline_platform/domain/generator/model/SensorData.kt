package com.core.data_pipeline_platform.domain.generator.model

data class SensorData(
    val sensorId: String,
    val value: Double,
    val timestamp:  String,
    val status: String
)
