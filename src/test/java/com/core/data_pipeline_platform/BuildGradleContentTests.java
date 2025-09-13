package com.core.data_pipeline_platform;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BuildGradleContentTests
 *
 * Purpose:
 * - Validate the Gradle build script changes from this PR's diff.
 * - Ensure plugin versions, Java toolchain, dependencies, repositories, and test task config
 *   match the expected values.
 *
 * Test Framework: JUnit 5 (Jupiter). AssertJ may be present via spring-boot-starter-test,
 * but these tests rely on JUnit assertions to avoid additional dependencies.
 */
class BuildGradleContentTests {

    private Path locateBuildGradle() throws IOException {
        // Prefer root build.gradle; if missing, scan recursively for a file matching expected markers from the diff.
        Path root = Paths.get("").toAbsolutePath();
        Path candidate = root.resolve("build.gradle");
        if (Files.exists(candidate)) return candidate;

        try (var stream = Files.walk(root, 5)) {
            List<Path> matches = stream
                    .filter(p -> p.getFileName().toString().equals("build.gradle"))
                    .filter(p -> {
                        try {
                            String c = Files.readString(p, StandardCharsets.UTF_8);
                            return c.contains("id 'org.springframework.boot' version '3.5.5'")
                                    && c.contains("group = 'com.core'")
                                    && c.contains("languageVersion = JavaLanguageVersion.of(21)");
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            assertFalse(matches.isEmpty(), "Could not find a build.gradle matching expected PR changes.");
            return matches.get(0);
        }
    }

    private String content() throws IOException {
        return Files.readString(locateBuildGradle(), StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("Plugins and Java toolchain")
    class PluginsAndToolchain {

        @Test
        @DisplayName("Includes Java and Spring Boot plugins with exact versions from the diff")
        void pluginsPresentWithExactVersions() throws IOException {
            String c = content();
            assertTrue(c.contains("id 'java'"), "Expected 'java' plugin.");
            assertTrue(c.contains("id 'org.springframework.boot' version '3.5.5'"),
                    "Expected Spring Boot plugin version 3.5.5.");
            assertTrue(c.contains("id 'io.spring.dependency-management' version '1.1.7'"),
                    "Expected io.spring.dependency-management version 1.1.7.");
        }

        @Test
        @DisplayName("Java toolchain is set to 21")
        void toolchainJava21() throws IOException {
            String c = content();
            assertTrue(
                    c.contains("languageVersion = JavaLanguageVersion.of(21)"),
                    "Expected Java toolchain languageVersion 21."
            );
        }
    }

    @Nested
    @DisplayName("Project coordinates and metadata")
    class CoordinatesAndMetadata {

        @Test
        @DisplayName("Group, version, and description match expected values")
        void coordinates() throws IOException {
            String c = content();
            assertTrue(c.contains("group = 'com.core'"), "Expected group = 'com.core'.");
            assertTrue(c.contains("version = '0.0.1-SNAPSHOT'"), "Expected version = '0.0.1-SNAPSHOT'.");
            assertTrue(c.contains("description = 'Demo project for Spring Boot'"),
                    "Expected project description.");
        }
    }

    @Nested
    @DisplayName("Repositories")
    class RepositoriesBlock {

        @Test
        @DisplayName("Uses mavenCentral and does not contain deprecated jcenter")
        void repositoriesValid() throws IOException {
            String c = content();
            assertTrue(c.contains("repositories") && c.contains("mavenCentral()"),
                    "Expected repositories { mavenCentral() }");
            assertFalse(c.contains("jcenter()"), "Should not use deprecated jcenter().");
        }
    }

    @Nested
    @DisplayName("Dependencies")
    class DependenciesBlock {

        @Test
        @DisplayName("Includes expected Spring Starters with correct scopes")
        void springStartersScopes() throws IOException {
            String c = content();
            assertTrue(c.contains("implementation 'org.springframework.boot:spring-boot-starter-data-jpa'"));
            assertTrue(c.contains("implementation 'org.springframework.boot:spring-boot-starter-validation'"));
            assertTrue(c.contains("implementation 'org.springframework.boot:spring-boot-starter-web'"));
            assertTrue(c.contains("testImplementation 'org.springframework.boot:spring-boot-starter-test'"));
        }

        @Test
        @DisplayName("Includes MariaDB driver with explicit version 3.5.6 (implementation scope)")
        void mariadbDependency() throws IOException {
            String c = content();
            assertTrue(c.contains("implementation 'org.mariadb.jdbc:mariadb-java-client:3.5.6'"),
                    "Expected MariaDB Java client 3.5.6 with implementation scope.");
        }

        @Test
        @DisplayName("Includes JUnit Platform launcher as testRuntimeOnly")
        void junitPlatformLauncherRuntimeOnly() throws IOException {
            String c = content();
            assertTrue(c.contains("testRuntimeOnly 'org.junit.platform:junit-platform-launcher'"),
                    "Expected JUnit Platform launcher as testRuntimeOnly.");
        }

        @Test
        @DisplayName("Does not accidentally include test artifacts in implementation scope")
        void noTestArtifactsInImplementation() throws IOException {
            String c = content();
            Pattern bad = Pattern.compile("implementation\\s+'org\\.junit\\.platform:junit-platform-launcher'");
            assertFalse(bad.matcher(c).find(),
                    "junit-platform-launcher must not be on implementation scope.");
        }
    }

    @Nested
    @DisplayName("Test task configuration")
    class TestTaskConfig {

        @Test
        @DisplayName("Configures test task to use JUnit Platform")
        void usesJUnitPlatform() throws IOException {
            String c = content();
            assertTrue(c.contains("tasks.named('test')") && c.contains("useJUnitPlatform()"),
                    "Expected tasks.named('test') { useJUnitPlatform() } block.");
        }
    }

    @Nested
    @DisplayName("Formatting and structural sanity checks")
    class Structural {

        @Test
        @DisplayName("Contains a single dependencies block")
        void singleDependenciesBlock() throws IOException {
            String c = content();
            int count = c.split("\\bdependencies\\b").length - 1;
            assertTrue(count >= 1, "Should have at least one dependencies block.");
            assertTrue(count == 1, "Should not have multiple dependencies blocks; found: " + count);
        }

        @Test
        @DisplayName("No obvious placeholder tokens remain")
        void noPlaceholders() throws IOException {
            String c = content();
            assertFalse(c.contains("TODO"), "No TODO placeholders should remain in build file.");
        }
    }
}