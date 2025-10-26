package io.github.dhruv1503.bootusage.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.boot.usage.report")
public class UsageReportProperties {

    /** Master switch to enable generation/exposure. */
    private boolean enabled = false;

    /** Cache TTL for the actuator endpoint in milliseconds (0 = no cache). */
    private long cacheTtl = 0L;

    /** Include sanitized bean origin locations. */
    private boolean includeOrigins = false;

    /** Include heuristic confidence scoring. */
    private boolean includeConfidence = false;

    /** Detect jars on the classpath that appear unused. */
    private boolean detectUnusedJars = false;

    /** Also write a Markdown summary. */
    private boolean markdownSummary = false;

    /** Output directory for the generated report. */
    private String outputDir = "build/boot-usage";

    /** Fail startup on first policy violation. */
    private boolean policiesFailOnViolation = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(long cacheTtl) { this.cacheTtl = cacheTtl; }

    public boolean isIncludeOrigins() { return includeOrigins; }
    public void setIncludeOrigins(boolean includeOrigins) { this.includeOrigins = includeOrigins; }

    public boolean isIncludeConfidence() { return includeConfidence; }
    public void setIncludeConfidence(boolean includeConfidence) { this.includeConfidence = includeConfidence; }

    public boolean isDetectUnusedJars() { return detectUnusedJars; }
    public void setDetectUnusedJars(boolean detectUnusedJars) { this.detectUnusedJars = detectUnusedJars; }

    public boolean isMarkdownSummary() { return markdownSummary; }
    public void setMarkdownSummary(boolean markdownSummary) { this.markdownSummary = markdownSummary; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public boolean isPoliciesFailOnViolation() { return policiesFailOnViolation; }
    public void setPoliciesFailOnViolation(boolean policiesFailOnViolation) { this.policiesFailOnViolation = policiesFailOnViolation; }
}
