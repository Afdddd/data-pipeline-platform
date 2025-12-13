package com.core.data_pipeline_platform.domain.generator.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class GenerateRequest(
    val fileName: @NotBlank String?,
    val recordCount: @Min(1) @Max(1000) Int
)
