package io.github.dhruv1503.bootusage.autoconfigure;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Generates and caches runtime usage reports with optional customization and policy
 * evaluation.
 */
public class UsageReportService {

    private final UsageReportProperties properties;
    private final ApplicationContext applicationContext;
    private final ObjectProvider<UsageReportCustomizer> customizers;
    private final ObjectProvider<UsagePolicy> policies;
    private final Environment environment;

    private volatile Map<String, Object> lastReport;
    private volatile long lastReportEpochMillis = 0L;

    public UsageReportService(UsageReportProperties properties,
            ApplicationContext applicationContext,
            ObjectProvider<UsageReportCustomizer> customizers,
            ObjectProvider<UsagePolicy> policies,
            Environment environment) {
        this.properties = properties;
        this.applicationContext = applicationContext;
        this.customizers = customizers;
        this.policies = policies;
        this.environment = environment;
    }

    public synchronized Map<String, Object> generateReport(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && this.lastReport != null && isCacheValid(now)) {
            return this.lastReport;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("timestamp", Instant.ofEpochMilli(now).toString());
        report.put("enabled", this.properties.isEnabled());
        report.put("includeOrigins", this.properties.isIncludeOrigins());
        report.put("includeConfidence", this.properties.isIncludeConfidence());
        report.put("detectUnusedJars", this.properties.isDetectUnusedJars());
        report.put("markdownSummary", this.properties.isMarkdownSummary());
        report.put("outputDir", this.properties.getOutputDir());
        report.put("policiesFailOnViolation", this.properties.isPoliciesFailOnViolation());

        populateAutoConfiguration(report);
        populateStarters(report);
        populateBeanOrigins(report);
        populateSuggestions(report);

        // Customizers can augment/mutate the structure
        for (UsageReportCustomizer customizer : this.customizers) {
            customizer.customize(report, this.applicationContext, this.environment);
        }

        // Evaluate policies
        List<String> violations = new ArrayList<>();
        for (UsagePolicy policy : this.policies) {
            List<String> v = Objects.requireNonNullElse(policy.evaluate(report, this.applicationContext, this.environment), List.of());
            violations.addAll(v);
        }
        report.put("policies", violations);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "0.1.0-SNAPSHOT");
        report.put("metadata", metadata);

        this.lastReport = report;
        this.lastReportEpochMillis = now;
        return report;
    }

    private boolean isCacheValid(long now) {
        long ttl = this.properties.getCacheTtl();
        return ttl > 0 && (now - this.lastReportEpochMillis) < ttl;
    }

    private void populateAutoConfiguration(Map<String, Object> report) {
        ConditionEvaluationReport cer = ConditionEvaluationReport.get(this.applicationContext.getAutowireCapableBeanFactory());
        if (cer == null) {
            report.put("appliedAutoConfigurations", List.of());
            report.put("skippedAutoConfigurations", List.of());
            return;
        }
        List<String> applied = new ArrayList<>(cer.getPositiveMatches().keySet());
        List<String> skipped = new ArrayList<>(cer.getNegativeMatches().keySet());
        report.put("appliedAutoConfigurations", applied);
        report.put("skippedAutoConfigurations", skipped);
    }

    private void populateStarters(Map<String, Object> report) {
        // Best-effort placeholder; real implementation can inspect classpath POMs/jars.
        Map<String, Object> starters = new LinkedHashMap<>();
        starters.put("declared", List.of());
        starters.put("used", List.of());
        starters.put("unused", List.of());
        report.put("starters", starters);
    }

    private void populateBeanOrigins(Map<String, Object> report) {
        if (!this.properties.isIncludeOrigins()) {
            report.put("beanOrigins", null);
            return;
        }
        ConfigurableListableBeanFactory beanFactory = this.applicationContext.getAutowireCapableBeanFactory();
        List<Map<String, String>> origins = new ArrayList<>();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            String resource = beanFactory.getBeanDefinition(name).getResourceDescription();
            if (resource != null) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("bean", name);
                entry.put("location", sanitize(resource));
                origins.add(entry);
            }
        }
        report.put("beanOrigins", origins);
    }

    private String sanitize(String pathLike) {
        try {
            String userHome = System.getProperty("user.home", "");
            String cwd = Path.of("").toAbsolutePath().toString();
            String result = pathLike;
            if (!userHome.isEmpty()) {
                result = result.replace(userHome, "~");
            }
            result = result.replace(cwd, ".");
            return result;
        }
        catch (Throwable ex) {
            return pathLike;
        }
    }

    private void populateSuggestions(Map<String, Object> report) {
        Map<String, Object> suggestions = new LinkedHashMap<>();
        suggestions.put("confidence", this.properties.isIncludeConfidence());
        suggestions.put("unusedJars", List.of());
        report.put("suggestions", suggestions);
    }
}
