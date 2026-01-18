package io.github.dhruv1503.bootusage.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Boot Usage Report feature.
 * <p>
 * All properties are prefixed with {@code spring.boot.usage.report}.
 *
 * @author Dhruv
 * @since 0.1.0
 */
@ConfigurationProperties(prefix = "spring.boot.usage.report")
public class UsageReportProperties {

    /**
     * Master switch to enable the usage report feature and endpoint.
     * When enabled, a usage report will be generated on application startup
     * and the /actuator/bootusage endpoint will be available.
     */
    private boolean enabled = false;

    /**
     * Cache TTL for the actuator endpoint in milliseconds.
     * Set to 0 to disable caching (report is regenerated on each request).
     * Default: 0 (no cache).
     */
    private long cacheTtl = 0L;

    /**
     * Include sanitized bean origin locations in the report.
     * When enabled, each bean will include information about where it was defined.
     * Paths are sanitized to remove user-specific directory information.
     */
    private boolean includeOrigins = false;

    /**
     * Include heuristic confidence scoring in suggestions.
     * Adds notes about the reliability of heuristic-based detections.
     */
    private boolean includeConfidence = false;

    /**
     * Attempt best-effort detection of unused JARs on the classpath.
     * Identifies JARs that don't appear to contribute any bean definitions.
     * Note: Runtime-only JARs (logging, serialization) are filtered out.
     */
    private boolean detectUnusedJars = false;

    /**
     * Also write a human-readable Markdown summary to the output directory.
     * The Markdown file provides a formatted view of the usage report.
     */
    private boolean markdownSummary = false;

    /**
     * Output directory for persisted reports (JSON and optional Markdown).
     * Relative paths are resolved from the application's working directory.
     */
    private String outputDir = "build/boot-usage";

    /**
     * Fail startup if any usage policy returns violations.
     * When enabled, the application context will fail to start if policies detect issues.
     * Use this to enforce architectural constraints.
     */
    private boolean policiesFailOnViolation = false;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCacheTtl() {
        return this.cacheTtl;
    }

    public void setCacheTtl(long cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public boolean isIncludeOrigins() {
        return this.includeOrigins;
    }

    public void setIncludeOrigins(boolean includeOrigins) {
        this.includeOrigins = includeOrigins;
    }

    public boolean isIncludeConfidence() {
        return this.includeConfidence;
    }

    public void setIncludeConfidence(boolean includeConfidence) {
        this.includeConfidence = includeConfidence;
    }

    public boolean isDetectUnusedJars() {
        return this.detectUnusedJars;
    }

    public void setDetectUnusedJars(boolean detectUnusedJars) {
        this.detectUnusedJars = detectUnusedJars;
    }

    public boolean isMarkdownSummary() {
        return this.markdownSummary;
    }

    public void setMarkdownSummary(boolean markdownSummary) {
        this.markdownSummary = markdownSummary;
    }

    public String getOutputDir() {
        return this.outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isPoliciesFailOnViolation() {
        return this.policiesFailOnViolation;
    }

    public void setPoliciesFailOnViolation(boolean policiesFailOnViolation) {
        this.policiesFailOnViolation = policiesFailOnViolation;
    }
}
