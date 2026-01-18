package io.github.dhruv1503.bootusage.autoconfigure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Writes the generated usage report to disk on ApplicationReadyEvent.
 * <p>
 * Produces both a JSON report and optionally a human-readable Markdown summary.
 * The Markdown report is designed to be easily readable in GitHub/GitLab and
 * provides actionable insights for dependency optimization.
 *
 * @author Dhruv Bansal
 * @since 1.0.0
 * @see UsageReportService
 * @see UsageReportProperties
 */
public class UsageReportPersistence {

	private static final Log logger = LogFactory.getLog(UsageReportPersistence.class);

	private final UsageReportService reportService;

	private final UsageReportProperties properties;

	private final ObjectWriter jsonWriter;

	/**
	 * Create a new {@code UsageReportPersistence}.
	 * @param reportService the usage report service
	 * @param properties the usage report properties
	 */
	public UsageReportPersistence(UsageReportService reportService, UsageReportProperties properties) {
		this.reportService = reportService;
		this.properties = properties;
		this.jsonWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
	}

	/**
	 * Write the usage report to disk when the application is ready.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void writeReport() {
		if (!this.properties.isEnabled()) {
			return;
		}

		try {
			Path dir = Path.of(this.properties.getOutputDir());
			Files.createDirectories(dir);
			Map<String, Object> report = this.reportService.generateReport(false);

			// Write JSON report
			Path jsonPath = dir.resolve("bootusage.json");
			byte[] jsonBytes = this.jsonWriter.writeValueAsBytes(report);
			Files.write(jsonPath, jsonBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			logger.info("Boot usage report written to: " + jsonPath.toAbsolutePath());

			// Write Markdown summary if enabled
			if (this.properties.isMarkdownSummary()) {
				Path mdPath = dir.resolve("bootusage.md");
				String markdown = generateMarkdown(report);
				Files.writeString(mdPath, markdown, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				logger.info("Boot usage markdown summary written to: " + mdPath.toAbsolutePath());
			}
		}
		catch (IOException ex) {
			logger.warn("Failed to persist usage report", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private String generateMarkdown(Map<String, Object> report) {
		StringBuilder sb = new StringBuilder();

		sb.append("# 📊 Spring Boot Usage Report\n\n");

		// Metadata section
		Map<String, Object> metadata = (Map<String, Object>) report.get("metadata");
		if (metadata != null) {
			sb.append("## Overview\n\n");
			sb.append("| Property | Value |\n");
			sb.append("|----------|-------|\n");
			appendTableRow(sb, "Generated At", metadata.get("generatedAt"));
			appendTableRow(sb, "Schema Version", metadata.get("schemaVersion"));
			appendTableRow(sb, "Spring Boot Version", metadata.get("springBootVersion"));
			appendTableRow(sb, "Java Version", metadata.get("javaVersion"));
			appendTableRow(sb, "Application Name", metadata.get("applicationName"));

			List<String> profiles = (List<String>) metadata.get("activeProfiles");
			if (profiles != null && !profiles.isEmpty()) {
				appendTableRow(sb, "Active Profiles", String.join(", ", profiles));
			}

			List<String> features = (List<String>) metadata.get("enabledFeatures");
			if (features != null && !features.isEmpty()) {
				appendTableRow(sb, "Enabled Features", String.join(", ", features));
			}
			sb.append("\n");
		}

		// Summary section
		Map<String, Object> summary = (Map<String, Object>) report.get("summary");
		if (summary != null) {
			sb.append("## 📈 Summary Statistics\n\n");
			sb.append("| Metric | Count |\n");
			sb.append("|--------|------:|\n");
			appendTableRow(sb, "Total Beans", summary.get("totalBeans"));
			appendTableRow(sb, "Singleton Beans", summary.get("singletonBeans"));
			appendTableRow(sb, "Prototype Beans", summary.get("prototypeBeans"));
			appendTableRow(sb, "Applied Auto-Configurations", summary.get("appliedAutoConfigurations"));
			appendTableRow(sb, "Skipped Auto-Configurations", summary.get("skippedAutoConfigurations"));
			sb.append("\n");

			sb.append("### Starter Usage\n\n");
			sb.append("| Category | Count |\n");
			sb.append("|----------|------:|\n");
			appendTableRow(sb, "Total Detected", summary.get("totalStarters"));
			appendTableRow(sb, "Used", summary.get("usedStarters"));
			appendTableRow(sb, "Unused", summary.get("unusedStarters"));
			appendTableRow(sb, "Indeterminate", summary.get("indeterminateStarters"));

			Object efficiency = summary.get("starterEfficiencyPercent");
			if (efficiency != null) {
				appendTableRow(sb, "Efficiency Score", efficiency + "%");
			}
			sb.append("\n");
		}

		// Starter Analysis section
		Map<String, Object> starters = (Map<String, Object>) report.get("starters");
		if (starters != null) {
			sb.append("## 🚀 Starter Analysis\n\n");

			// Used starters
			List<Map<String, Object>> used = (List<Map<String, Object>>) starters.get("used");
			if (used != null && !used.isEmpty()) {
				sb.append("### ✅ Active Starters (").append(used.size()).append(")\n\n");
				sb.append("| Starter | Category | Version |\n");
				sb.append("|---------|----------|--------:|\n");
				for (Map<String, Object> starter : used) {
					sb.append("| `").append(starter.get("artifactId")).append("` | ");
					sb.append(starter.get("category")).append(" | ");
					sb.append(starter.get("version")).append(" |\n");
				}
				sb.append("\n");
			}

			// Unused starters
			List<Map<String, Object>> unused = (List<Map<String, Object>>) starters.get("unused");
			if (unused != null && !unused.isEmpty()) {
				sb.append("### ⚠️ Potentially Unused Starters (").append(unused.size()).append(")\n\n");
				sb.append("These starters don't appear to have any active auto-configurations:\n\n");
				sb.append("| Starter | Category | Suggestion |\n");
				sb.append("|---------|----------|------------|\n");
				for (Map<String, Object> starter : unused) {
					sb.append("| `").append(starter.get("artifactId")).append("` | ");
					sb.append(starter.get("category")).append(" | ");
					sb.append("Consider removing if not needed |\n");
				}
				sb.append("\n");
			}

			// Indeterminate starters
			List<Map<String, Object>> indeterminate = (List<Map<String, Object>>) starters.get("indeterminate");
			if (indeterminate != null && !indeterminate.isEmpty()) {
				sb.append("### ❓ Indeterminate Starters (").append(indeterminate.size()).append(")\n\n");
				sb.append("Usage could not be determined for these starters:\n\n");
				for (Map<String, Object> starter : indeterminate) {
					sb.append("- `").append(starter.get("coordinate")).append("`\n");
				}
				sb.append("\n");
			}
		}

		// Suggestions section
		Map<String, Object> suggestions = (Map<String, Object>) report.get("suggestions");
		if (suggestions != null) {
			List<Map<String, Object>> unusedStarterSuggestions =
					(List<Map<String, Object>>) suggestions.get("unusedStarters");
			List<String> tips = (List<String>) suggestions.get("optimizationTips");

			if ((unusedStarterSuggestions != null && !unusedStarterSuggestions.isEmpty()) ||
					(tips != null && !tips.isEmpty())) {
				sb.append("## 💡 Optimization Suggestions\n\n");

				if (tips != null && !tips.isEmpty()) {
					for (String tip : tips) {
						sb.append("- ").append(tip).append("\n");
					}
					sb.append("\n");
				}

				String confidenceNote = (String) suggestions.get("confidenceNote");
				if (confidenceNote != null) {
					sb.append("> **Note:** ").append(confidenceNote).append("\n\n");
				}
			}
		}

		// Policy Violations section
		List<Map<String, Object>> violations = (List<Map<String, Object>>) report.get("policyViolations");
		if (violations != null && !violations.isEmpty()) {
			sb.append("## ❌ Policy Issues\n\n");

			// Group by severity
			boolean hasViolations = violations.stream()
					.anyMatch(v -> "VIOLATION".equals(v.get("severity")));
			boolean hasWarnings = violations.stream()
					.anyMatch(v -> "WARNING".equals(v.get("severity")));

			if (hasViolations) {
				sb.append("### Violations\n\n");
				for (Map<String, Object> v : violations) {
					if ("VIOLATION".equals(v.get("severity"))) {
						sb.append("- 🔴 **").append(v.get("policy")).append("**: ");
						sb.append(v.get("message")).append("\n");
					}
				}
				sb.append("\n");
			}

			if (hasWarnings) {
				sb.append("### Warnings\n\n");
				for (Map<String, Object> v : violations) {
					if ("WARNING".equals(v.get("severity"))) {
						sb.append("- 🟡 **").append(v.get("policy")).append("**: ");
						sb.append(v.get("message")).append("\n");
					}
				}
				sb.append("\n");
			}
		}

		// Footer
		sb.append("---\n\n");
		sb.append("*Generated by [boot-usage-spring-boot-starter]");
		sb.append("(https://github.com/dhruv-15-03/boot-usage-spring-boot-starter)* ");
		sb.append("| Schema v").append(report.getOrDefault("schemaVersion", "1.0.0")).append("\n");

		return sb.toString();
	}

	private void appendTableRow(StringBuilder sb, String label, Object value) {
		sb.append("| ").append(label).append(" | ");
		sb.append(value != null ? value : "-").append(" |\n");
	}

}
