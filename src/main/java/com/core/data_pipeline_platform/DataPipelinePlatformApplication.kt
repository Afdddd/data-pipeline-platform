package com.core.data_pipeline_platform

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
object DataPipelinePlatformApplication {
    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplication.run(DataPipelinePlatformApplication::class.java, *args)
    }
}
