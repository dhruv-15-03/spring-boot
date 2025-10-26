package io.github.dhruv1503.bootusage.autoconfigure;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class UsageReportService {

    private final UsageReportProperties properties;

    public UsageReportService(UsageReportProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> generateReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("timestamp", Instant.now().toString());
        report.put("enabled", properties.isEnabled());
        report.put("includeOrigins", properties.isIncludeOrigins());
        report.put("includeConfidence", properties.isIncludeConfidence());
        report.put("detectUnusedJars", properties.isDetectUnusedJars());
        report.put("markdownSummary", properties.isMarkdownSummary());
        report.put("outputDir", properties.getOutputDir());
        report.put("policiesFailOnViolation", properties.isPoliciesFailOnViolation());
        // Placeholder sections to be enriched by analyzers/customizers in future iterations
        report.put("appliedAutoConfigurations", java.util.List.of());
        report.put("skippedAutoConfigurations", java.util.List.of());
        report.put("starters", Map.of("declared", java.util.List.of(), "used", java.util.List.of(), "unused", java.util.List.of()));
        report.put("suggestions", Map.of("confidence", properties.isIncludeConfidence(), "unusedJars", java.util.List.of()));
        report.put("beanOrigins", properties.isIncludeOrigins() ? java.util.List.of() : null);
        report.put("policies", java.util.List.of());
        report.put("metadata", Map.of("version", "0.1.0-SNAPSHOT"));
        return report;
    }
}
