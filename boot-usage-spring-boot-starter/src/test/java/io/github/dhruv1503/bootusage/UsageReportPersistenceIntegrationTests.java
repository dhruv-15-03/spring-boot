package io.github.dhruv1503.bootusage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = {
        "spring.boot.usage.report.enabled=true",
        "spring.boot.usage.report.markdown-summary=true",
        "spring.main.web-application-type=none",
        "management.endpoints.web.exposure.include=bootusage",
        "management.endpoint.bootusage.enabled=true",
        "spring.output.ansi.enabled=NEVER"
})
@TestPropertySource(properties = {
        "spring.boot.usage.report.output-dir=build/it-boot-usage"
})
class UsageReportPersistenceIntegrationTests {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp { }

    @AfterEach
    void clean() throws Exception {
        // no-op cleanup; CI may persist files
    }

    @Test
    void writesJsonAndMarkdownOnReady() throws Exception {
        Path dir = Path.of("build/it-boot-usage");
        Path json = dir.resolve("bootusage.json");
        Path md = dir.resolve("bootusage.md");
        // Poll briefly to allow ApplicationReadyEvent listener to write files
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline && (!Files.exists(json) || !Files.exists(md))) {
            Thread.sleep(100);
        }
        assertThat(Files.exists(json)).isTrue();
        assertThat(Files.size(json)).isGreaterThan(0);
        assertThat(Files.exists(md)).isTrue();
        assertThat(Files.size(md)).isGreaterThan(0);
    }
}
