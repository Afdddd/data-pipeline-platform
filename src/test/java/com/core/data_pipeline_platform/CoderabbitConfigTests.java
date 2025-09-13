package com.core.data_pipeline_platform;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for validating the CodeRabbit configuration introduced in the PR diff.
 *
 * Testing library and framework:
 * - JUnit 5 (Jupiter). If the project uses a different framework, adapt imports accordingly.
 *
 * Strategy:
 * - Prefer parsing via SnakeYAML if present on the classpath (common in many Java projects).
 * - Fallback to resilient content assertions if YAML parser is unavailable.
 */
public class CoderabbitConfigTests {

    private static String yamlText;

    @BeforeAll
    static void loadResource() throws Exception {
        try (InputStream is = CoderabbitConfigTests.class.getClassLoader()
                .getResourceAsStream("coderabbit-config.yaml")) {
            assertNotNull(is, "coderabbit-config.yaml test resource must be present");
            yamlText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(yamlText.contains("version: 1"), "YAML must define version: 1");
        }
    }

    @AfterAll
    static void cleanup() {
        yamlText = null;
    }

    private boolean snakeYamlAvailable() {
        try {
            Class.forName("org.yaml.snakeyaml.Yaml");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYamlOrNull() {
        if (\!snakeYamlAvailable()) return null;
        try {
            Class<?> yamlClass = Class.forName("org.yaml.snakeyaml.Yaml");
            Object yaml = yamlClass.getDeclaredConstructor().newInstance();
            Method load = yamlClass.getMethod("load", java.io.Reader.class);
            java.io.Reader reader = new java.io.StringReader(yamlText);
            Object result = load.invoke(yaml, reader);
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            fail("Parsed YAML root is not a map: " + (result == null ? "null" : result.getClass()));
            return null;
        } catch (Exception e) {
            fail("Failed to parse YAML with SnakeYAML: " + e.getMessage());
            return null;
        }
    }

    @Test
    @DisplayName("YAML is syntactically valid when SnakeYAML is available")
    void yamlIsValidIfParserExists() {
        Map<String, Object> root = parseYamlOrNull();
        if (root == null) {
            // No SnakeYAML: ensure file includes key structural markers as a fallback.
            assertAll(
                () -> assertTrue(yamlText.contains("review:"), "Should contain 'review:' section"),
                () -> assertTrue(yamlText.contains("ignore:"), "Should contain 'ignore:' section"),
                () -> assertTrue(yamlText.contains("rules:"), "Should contain 'rules:' section")
            );
        } else {
            assertNotNull(root, "Parsed YAML root should not be null");
            assertTrue(root.containsKey("version"), "version key missing");
            Object version = root.get("version");
            assertTrue(version instanceof Number, "version should be a number");
            assertEquals(1, ((Number) version).intValue(), "version must be 1");
        }
    }

    @Test
    @DisplayName("Validate review section: mode, auto_approve, language, scope")
    void validateReviewSection() {
        Map<String, Object> root = parseYamlOrNull();
        if (root == null) {
            // Fallback content checks
            assertTrue(yamlText.contains("mode: balanced"), "mode should be balanced");
            assertTrue(yamlText.contains("auto_approve: false"), "auto_approve should be false");
            assertTrue(yamlText.contains("language: ko"), "language should be ko");
            assertTrue(yamlText.contains("- code_quality"), "scope must include code_quality");
            assertTrue(yamlText.contains("- security"), "scope must include security");
            assertTrue(yamlText.contains("- performance"), "scope must include performance");
            assertTrue(yamlText.contains("- best_practices"), "scope must include best_practices");
            return;
        }

        Object reviewObj = root.get("review");
        assertTrue(reviewObj instanceof Map, "review should be a map");
        Map<String, Object> review = (Map<String, Object>) reviewObj;

        String mode = String.valueOf(review.get("mode"));
        assertTrue(Set.of("conservative", "balanced", "aggressive").contains(mode),
                "review.mode must be one of conservative|balanced|aggressive");
        assertEquals("balanced", mode, "review.mode should be balanced");

        Object autoApprove = review.get("auto_approve");
        assertNotNull(autoApprove, "review.auto_approve missing");
        assertTrue(autoApprove instanceof Boolean, "review.auto_approve must be boolean");
        assertEquals(false, autoApprove, "review.auto_approve must be false");

        assertEquals("ko", String.valueOf(review.get("language")), "review.language should be 'ko'");

        Object scopeObj = review.get("scope");
        assertTrue(scopeObj instanceof List, "review.scope must be a list");
        @SuppressWarnings("unchecked")
        List<Object> scope = (List<Object>) scopeObj;
        Set<String> scopeSet = scope.stream().map(String::valueOf).collect(Collectors.toSet());
        assertTrue(scopeSet.containsAll(Set.of("code_quality","security","performance","best_practices")),
                "review.scope must include code_quality, security, performance, best_practices");
    }

    @Test
    @DisplayName("Validate ignore globs for docs and build directories")
    void validateIgnoreSection() {
        Map<String, Object> root = parseYamlOrNull();
        if (root == null) {
            assertAll(
                () -> assertTrue(yamlText.contains("\"*.md\"") || yamlText.contains("'*.md'") || yamlText.contains("*.md"), "ignore must include *.md"),
                () -> assertTrue(yamlText.contains("\"*.yml\"") || yamlText.contains("'*.yml'") || yamlText.contains("*.yml"), "ignore must include *.yml"),
                () -> assertTrue(yamlText.contains("\"*.yaml\"") || yamlText.contains("'*.yaml'") || yamlText.contains("*.yaml"), "ignore must include *.yaml"),
                () -> assertTrue(yamlText.contains("\"build/**\"") || yamlText.contains("build/**"), "ignore must include build/**"),
                () -> assertTrue(yamlText.contains("\"gradle/**\"") || yamlText.contains("gradle/**"), "ignore must include gradle/**")
            );
            return;
        }

        Object ignoreObj = root.get("ignore");
        assertTrue(ignoreObj instanceof List, "ignore must be a list");
        @SuppressWarnings("unchecked")
        List<Object> ignore = (List<Object>) ignoreObj;
        Set<String> globs = ignore.stream().map(String::valueOf).collect(Collectors.toSet());
        assertTrue(globs.contains("*.md"), "ignore must include *.md");
        assertTrue(globs.contains("*.yml"), "ignore must include *.yml");
        assertTrue(globs.contains("*.yaml"), "ignore must include *.yaml");
        assertTrue(globs.contains("build/**"), "ignore must include build/**");
        assertTrue(globs.contains("gradle/**"), "ignore must include gradle/**");
    }

    @Test
    @DisplayName("Validate rules: two enabled rules with expected names and descriptions")
    void validateRulesSection() {
        Map<String, Object> root = parseYamlOrNull();
        if (root == null) {
            assertTrue(yamlText.contains("rules:"), "rules must exist");
            assertTrue(yamlText.contains("Spring Boot 모범 사례"), "Must include Spring Boot best practices rule");
            assertTrue(yamlText.contains("보안 검사"), "Must include security check rule");
            assertTrue(yamlText.contains("enabled: true"), "Rules must be enabled");
            return;
        }

        Object rulesObj = root.get("rules");
        assertTrue(rulesObj instanceof List, "rules must be a list");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) rulesObj;
        assertEquals(2, rules.size(), "Expect exactly 2 rules");

        Map<String, Object> r1 = rules.get(0);
        Map<String, Object> r2 = rules.get(1);

        assertAll("Rule 1",
            () -> assertEquals("Spring Boot 모범 사례", String.valueOf(r1.get("name"))),
            () -> assertTrue(String.valueOf(r1.get("description")).contains("Spring Boot")),
            () -> assertEquals(true, r1.get("enabled"))
        );

        assertAll("Rule 2",
            () -> assertEquals("보안 검사", String.valueOf(r2.get("name"))),
            () -> assertTrue(String.valueOf(r2.get("description")).contains("보안")),
            () -> assertEquals(true, r2.get("enabled"))
        );
    }

    @Test
    @DisplayName("Fail-fast on unexpected or missing critical keys")
    void failFastOnMissingKeys() {
        // This test ensures we explicitly check for critical keys; acts as a guardrail for future changes.
        if (\!snakeYamlAvailable()) {
            // Minimal guard using content presence.
            assertTrue(yamlText.contains("version:"), "Missing version key");
            assertTrue(yamlText.contains("review:"), "Missing review section");
            assertTrue(yamlText.contains("ignore:"), "Missing ignore section");
            assertTrue(yamlText.contains("rules:"), "Missing rules section");
            return;
        }
        Map<String, Object> root = parseYamlOrNull();
        assertNotNull(root);
        for (String key : List.of("version", "review", "ignore", "rules")) {
            assertTrue(root.containsKey(key), "Missing required top-level key: " + key);
        }
    }
}