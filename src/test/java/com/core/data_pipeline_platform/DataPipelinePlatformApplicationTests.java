package com.core.data_pipeline_platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DataPipelinePlatformApplicationTests {

    /**
     * Test suite uses JUnit Jupiter (JUnit 5) and Spring Boot Test.
     * Assertions: org.junit.jupiter.api.Assertions.
     * Focus: application context bootstrapping and main entry point contract.
     */
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
        // Sanity check that Spring Boot context initializes for the application.
        assertNotNull(applicationContext, "ApplicationContext should be initialized");
    }

    @Test
    void applicationContext_isNotNull_andHasBeans() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.getBeanDefinitionCount() > 0, "Bean definition count should be > 0");
    }

    @Test
    void environment_isAvailable() {
        assertNotNull(environment, "Environment should be injected");
        // Accessing active profiles should not throw, even if empty.
        assertNotNull(environment.getActiveProfiles(), "Active profiles array should not be null");
    }

    @Test
    void applicationClass_hasSpringBootApplicationAnnotation() {
        // The main application class should be annotated properly.
        assertTrue(DataPipelinePlatformApplication.class.isAnnotationPresent(SpringBootApplication.class),
                "Main application must be annotated with @SpringBootApplication");
    }

    @Test
    void main_doesNotThrow() {
        // Boot the app with no web server to avoid port conflicts.
        assertDoesNotThrow(() ->
                DataPipelinePlatformApplication.main(
                        new String[] { "--spring.main.web-application-type=none" }
                ),
                "main() should start without throwing when web type is NONE"
        );
    }
}
