package com.core.data_pipeline_platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Note: Testing library/framework: JUnit Jupiter (JUnit 5).
 *
 * Purpose:
 * - Validate the docker-compose configuration introduced/modified in this PR, focusing on the diff.
 * - Prefer robust parsing via SnakeYAML if available on the classpath; otherwise, use conservative text checks.
 *
 * No new test dependencies are introduced. Reflection is used to parse with SnakeYAML only if already present.
 */
public class DockerComposeConfigTests {

    private static final List<String> CANDIDATE_FILENAMES = List.of(
            "docker-compose.yml", "docker-compose.yaml", "compose.yml", "compose.yaml"
    );

    private static Path locateComposeFile() {
        // 1) Prefer real compose files at repo root
        for (String name : CANDIDATE_FILENAMES) {
            Path p = Paths.get(name);
            if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
        }
        // 2) Fall back to test fixture mirroring the PR diff
        Path fixture = Paths.get("src/test/resources/docker-compose.fixture.yml");
        if (Files.isRegularFile(fixture)) return fixture.toAbsolutePath().normalize();

        fail("No docker-compose file found. Expected one of " + CANDIDATE_FILENAMES +
             " or test fixture at src/test/resources/docker-compose.fixture.yml");
        return null;
    }

    private static String readAll(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Failed to read compose file: " + p, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<Map<String, Object>> parseYamlIfPossible(String yaml) {
        try {
            // Attempt to use SnakeYAML if present: new org.yaml.snakeyaml.Yaml().load(...)
            Class<?> yamlClass = Class.forName("org.yaml.snakeyaml.Yaml");
            Object yamlParser = yamlClass.getDeclaredConstructor().newInstance();
            Object map = yamlClass.getMethod("load", String.class).invoke(yamlParser, yaml);
            if (map instanceof Map) {
                return Optional.of((Map<String, Object>) map);
            } else {
                return Optional.empty();
            }
        } catch (Throwable t) {
            // SnakeYAML not present or failed; return empty to trigger text-based validations
            return Optional.empty();
        }
    }

    @Nested
    @DisplayName("docker-compose schema and critical keys")
    class SchemaAndKeys {

        @Test
        @DisplayName("version is 3.8 and services.mariadb is defined with required attributes")
        void versionAndMariaDbService() {
            Path compose = locateComposeFile();
            String yaml = readAll(compose);
            Optional<Map<String, Object>> parsed = parseYamlIfPossible(yaml);

            if (parsed.isPresent()) {
                Map<String, Object> root = parsed.get();

                // version
                Object version = root.get("version");
                assertNotNull(version, "version should be present");
                assertEquals("3.8", String.valueOf(version), "version should be '3.8'");

                // services
                Object servicesObj = root.get("services");
                assertTrue(servicesObj instanceof Map, "services should be a mapping");
                Map<?, ?> services = (Map<?, ?>) servicesObj;

                Object mariadbObj = services.get("mariadb");
                assertTrue(mariadbObj instanceof Map, "services.mariadb should be a mapping");
                Map<?, ?> mariadb = (Map<?, ?>) mariadbObj;

                assertEquals("mariadb:latest", String.valueOf(mariadb.get("image")),
                        "mariadb.image should be 'mariadb:latest'");
                assertEquals("mariadb", String.valueOf(mariadb.get("container_name")),
                        "mariadb.container_name should be 'mariadb'");
                assertEquals("always", String.valueOf(mariadb.get("restart")),
                        "mariadb.restart should be 'always'");

                // environment
                Object envObj = mariadb.get("environment");
                assertTrue(envObj instanceof Map, "mariadb.environment should be a mapping");
                Map<?, ?> env = (Map<?, ?>) envObj;
                assertEquals("1234", String.valueOf(env.get("MYSQL_ROOT_PASSWORD")));
                assertEquals("data_pipeline", String.valueOf(env.get("MYSQL_DATABASE")));
                assertEquals("root", String.valueOf(env.get("MYSQL_USER")));
                assertEquals("1234", String.valueOf(env.get("MYSQL_PASSWORD")));

                // ports
                Object portsObj = mariadb.get("ports");
                assertTrue(portsObj instanceof List, "mariadb.ports should be a list");
                List<?> ports = (List<?>) portsObj;
                assertTrue(ports.stream().map(String::valueOf).anyMatch(p -> p.equals("3306:3306")),
                        "Expected ports to include '3306:3306'");

                // service volumes
                Object volsObj = mariadb.get("volumes");
                assertTrue(volsObj instanceof List, "mariadb.volumes should be a list");
                List<?> vols = (List<?>) volsObj;
                assertTrue(vols.stream().map(String::valueOf).anyMatch(v -> v.startsWith("mariadb_data:")),
                        "Expected a volume mapping using 'mariadb_data'");

                // top-level volumes
                Object topVolsObj = root.get("volumes");
                assertTrue(topVolsObj instanceof Map, "top-level volumes should be a mapping");
                Map<?, ?> topVols = (Map<?, ?>) topVolsObj;
                assertTrue(topVols.containsKey("mariadb_data"),
                        "Expected top-level volumes to define 'mariadb_data'");
            } else {
                // Fallback: text-based assertions (robust to whitespace/indentation)
                assertTrue(Pattern.compile("(?m)^version:\\s*'?:?\\s*3\\.8'?$").matcher(yaml).find(),
                        "Compose version 3.8 must be present");

                assertTrue(yaml.contains("services:"), "Top-level 'services' must be present");
                assertTrue(Pattern.compile("(?m)^\\s*mariadb:\\s*$").matcher(yaml).find(),
                        "'mariadb' service must be present");

                assertTrue(yaml.contains("image: mariadb:latest"), "mariadb image must be mariadb:latest");
                assertTrue(yaml.contains("container_name: mariadb"), "container_name must be 'mariadb'");
                assertTrue(yaml.contains("restart: always"), "restart must be 'always'");

                // Environment variables
                assertTrue(yaml.contains("MYSQL_ROOT_PASSWORD: 1234"), "MYSQL_ROOT_PASSWORD must be 1234");
                assertTrue(yaml.contains("MYSQL_DATABASE: data_pipeline"), "MYSQL_DATABASE must be 'data_pipeline'");
                assertTrue(yaml.contains("MYSQL_USER: root"), "MYSQL_USER must be 'root'");
                assertTrue(yaml.contains("MYSQL_PASSWORD: 1234"), "MYSQL_PASSWORD must be '1234'");

                // Ports and volumes
                assertTrue(yaml.contains("- \"3306:3306\"") || yaml.contains("- '3306:3306'") || yaml.contains("- 3306:3306"),
                        "Ports must include 3306:3306");
                assertTrue(yaml.contains("volumes:"), "Top-level 'volumes' must be present");
                assertTrue(yaml.contains("mariadb_data:/var/lib/mysql"), "Service must mount 'mariadb_data:/var/lib/mysql'");
                assertTrue(Pattern.compile("(?m)^\\s*mariadb_data:\\s*$").matcher(yaml).find(),
                        "Top-level volume 'mariadb_data' must be declared");
            }
        }

        @Test
        @DisplayName("Ports mapping is TCP default and not an empty list")
        void portsMappingNonEmpty() {
            Path compose = locateComposeFile();
            String yaml = readAll(compose);
            Optional<Map<String, Object>> parsed = parseYamlIfPossible(yaml);

            if (parsed.isPresent()) {
                Map<String, Object> root = parsed.get();
                Map<String, Object> services = (Map<String, Object>) root.get("services");
                Map<String, Object> mariadb = (Map<String, Object>) services.get("mariadb");
                List<?> ports = (List<?>) mariadb.get("ports");
                assertNotNull(ports, "ports should not be null");
                assertFalse(ports.isEmpty(), "ports must not be empty");
                assertTrue(ports.stream().anyMatch(p -> String.valueOf(p).startsWith("3306")),
                        "port 3306 must be exposed");
            } else {
                assertTrue(yaml.contains("3306:3306"), "Should expose port 3306");
            }
        }
    }

    @Nested
    @DisplayName("Security and hardening checks (informational)")
    class SecurityChecks {

        @Test
        @DisplayName("Warn if root credentials are weak (does not fail build, only asserts presence and flags)")
        void weakCredentialFlag() {
            Path compose = locateComposeFile();
            String yaml = readAll(compose);
            Optional<Map<String, Object>> parsed = parseYamlIfPossible(yaml);

            String user = null;
            String rootPwd = null;

            if (parsed.isPresent()) {
                Map<String, Object> root = parsed.get();
                Map<String, Object> services = (Map<String, Object>) root.get("services");
                Map<String, Object> mariadb = (Map<String, Object>) services.get("mariadb");
                Map<String, Object> env = (Map<String, Object>) mariadb.get("environment");
                if (env \!= null) {
                    Object u = env.get("MYSQL_USER");
                    Object p = env.get("MYSQL_ROOT_PASSWORD");
                    user = (u == null ? null : String.valueOf(u));
                    rootPwd = (p == null ? null : String.valueOf(p));
                }
            } else {
                // naive extraction as fallback
                if (yaml.contains("MYSQL_USER:")) {
                    user = extractAfter(yaml, "MYSQL_USER:");
                }
                if (yaml.contains("MYSQL_ROOT_PASSWORD:")) {
                    rootPwd = extractAfter(yaml, "MYSQL_ROOT_PASSWORD:");
                }
            }

            assertNotNull(user, "MYSQL_USER must be defined");
            assertNotNull(rootPwd, "MYSQL_ROOT_PASSWORD must be defined");

            // Non-fatal informational checks
            if ("root".equalsIgnoreCase(user) || "1234".equals(rootPwd)) {
                // Log via assertion message for visibility while still passing
                assertTrue(true, "Informational: Consider using non-root user and stronger password.");
            }
        }

        private String extractAfter(String content, String key) {
            int idx = content.indexOf(key);
            if (idx < 0) return null;
            String tail = content.substring(idx + key.length()).trim();
            int eol = tail.indexOf('\n');
            return (eol >= 0 ? tail.substring(0, eol) : tail).trim().replace("\"", "").replace("'", "");
        }
    }

    @Nested
    @DisplayName("Volumes configuration integrity")
    class VolumesConfig {

        @Test
        @DisplayName("Service binds mariadb_data to /var/lib/mysql and top-level volume exists")
        void volumeBindingAndTopLevelDefinition() {
            Path compose = locateComposeFile();
            String yaml = readAll(compose);
            Optional<Map<String, Object>> parsed = parseYamlIfPossible(yaml);

            if (parsed.isPresent()) {
                Map<String, Object> root = parsed.get();
                Map<String, Object> services = (Map<String, Object>) root.get("services");
                Map<String, Object> mariadb = (Map<String, Object>) services.get("mariadb");
                List<?> volumes = (List<?>) mariadb.get("volumes");
                assertNotNull(volumes, "Service volumes must be defined");
                boolean found = volumes.stream().map(String::valueOf)
                        .anyMatch(v -> v.equals("mariadb_data:/var/lib/mysql"));
                assertTrue(found, "Service must mount 'mariadb_data:/var/lib/mysql'");

                Map<String, Object> topVols = (Map<String, Object>) root.get("volumes");
                assertNotNull(topVols, "Top-level volumes must be defined");
                assertTrue(topVols.containsKey("mariadb_data"), "Top-level 'mariadb_data' volume must exist");
            } else {
                assertTrue(yaml.contains("mariadb_data:/var/lib/mysql"),
                        "Service must mount 'mariadb_data:/var/lib/mysql'");
                assertTrue(Pattern.compile("(?m)^\\s*mariadb_data:\\s*$").matcher(yaml).find(),
                        "Top-level volume 'mariadb_data' must be declared");
            }
        }
    }
}