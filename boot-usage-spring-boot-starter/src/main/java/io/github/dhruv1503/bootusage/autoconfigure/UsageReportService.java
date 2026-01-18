/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.dhruv1503.bootusage.autoconfigure;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import io.github.dhruv1503.bootusage.autoconfigure.StarterUsageAnalyzer.StarterAnalysisResult;
import io.github.dhruv1503.bootusage.autoconfigure.StarterUsageAnalyzer.StarterInfo;
import io.github.dhruv1503.bootusage.autoconfigure.UsagePolicy.PolicyResult;

/**
 * Generates comprehensive runtime usage reports for Spring Boot applications.
 * <p>
 * This service provides insights beyond what the standard {@code /actuator/conditions}
 * endpoint offers, including:
 * <ul>
 *   <li>Starter usage analysis (declared vs actually used vs unused)</li>
 *   <li>Bean origin tracking with sanitized paths</li>
 *   <li>Unused JAR detection</li>
 *   <li>Custom policy enforcement via SPI</li>
 *   <li>Report customization via SPI</li>
 *   <li>Report caching with configurable TTL</li>
 * </ul>
 * <p>
 * The report is structured with a consistent schema version for API stability.
 *
 * @author Dhruv Bansal
 * @since 1.0.0
 * @see UsageReportProperties
 * @see UsageReportCustomizer
 * @see UsagePolicy
 */
public class UsageReportService {

	/**
	 * Current schema version for the report structure.
	 * Increment when making breaking changes to the report format.
	 */
	public static final String SCHEMA_VERSION = "1.0.0";

	private final UsageReportProperties properties;

	private final ConfigurableApplicationContext applicationContext;

	private final ObjectProvider<UsageReportCustomizer> customizers;

	private final ObjectProvider<UsagePolicy> policies;

	private final StarterUsageAnalyzer starterAnalyzer;

	private final BeanOriginTrackingPostProcessor beanOriginTracker;

	private final Environment environment;

	private volatile Map<String, Object> cachedReport;

	private volatile long cachedReportTimestamp = 0L;

	/**
	 * Create a new {@code UsageReportService}.
	 * @param properties the usage report properties
	 * @param applicationContext the application context
	 * @param customizers optional report customizers
	 * @param policies optional usage policies
	 * @param starterAnalyzer the starter usage analyzer
	 * @param beanOriginTracker the bean origin tracking post processor
	 * @param environment the Spring environment
	 */
	public UsageReportService(UsageReportProperties properties,
			ConfigurableApplicationContext applicationContext,
			ObjectProvider<UsageReportCustomizer> customizers,
			ObjectProvider<UsagePolicy> policies,
			StarterUsageAnalyzer starterAnalyzer,
			BeanOriginTrackingPostProcessor beanOriginTracker,
			Environment environment) {
		this.properties = properties;
		this.applicationContext = applicationContext;
		this.customizers = customizers;
		this.policies = policies;
		this.starterAnalyzer = starterAnalyzer;
		this.beanOriginTracker = beanOriginTracker;
		this.environment = environment;
	}

	/**
	 * Generates the usage report, optionally using a cached version.
	 * @param force if {@code true}, bypasses the cache and regenerates the report
	 * @return the usage report as a structured map
	 */
	public synchronized Map<String, Object> generateReport(boolean force) {
		long now = System.currentTimeMillis();

		if (!force && this.cachedReport != null && isCacheValid(now)) {
			return this.cachedReport;
		}

		Map<String, Object> report = new LinkedHashMap<>();

		// Schema and metadata
		report.put("schemaVersion", SCHEMA_VERSION);
		report.put("metadata", buildMetadata(now));
		report.put("configuration", buildConfiguration());

		// Auto-configuration analysis
		Map<String, Object> autoConfigReport = buildAutoConfigurationReport();
		report.put("autoConfiguration", autoConfigReport);

		// Starter analysis (key differentiator from /actuator/conditions)
		report.put("starters", buildStarterReport());

		// Bean origins (when enabled)
		if (this.properties.isIncludeOrigins()) {
			report.put("beanOrigins", buildBeanOriginReport());
		}

		// Suggestions section
		report.put("suggestions", buildSuggestions());

		// Summary statistics
		report.put("summary", buildSummary(autoConfigReport));

		// Apply customizers before policy evaluation
		applyCustomizers(report);

		// Evaluate policies and collect violations
		report.put("policyViolations", evaluatePolicies(report));

		// Update cache
		this.cachedReport = report;
		this.cachedReportTimestamp = now;

		return report;
	}

	/**
	 * Get the report properties.
	 * @return the properties
	 */
	public UsageReportProperties getProperties() {
		return this.properties;
	}

	private boolean isCacheValid(long now) {
		long ttl = this.properties.getCacheTtl();
		return ttl > 0 && (now - this.cachedReportTimestamp) < ttl;
	}

	private Map<String, Object> buildMetadata(long timestamp) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("generatedAt", Instant.ofEpochMilli(timestamp).toString());
		metadata.put("schemaVersion", SCHEMA_VERSION);
		metadata.put("springBootVersion", getSpringBootVersion());
		metadata.put("javaVersion", System.getProperty("java.version"));
		metadata.put("applicationName", this.environment.getProperty("spring.application.name", "unknown"));

		// Active profiles
		String[] activeProfiles = this.environment.getActiveProfiles();
		metadata.put("activeProfiles", activeProfiles.length > 0 ? List.of(activeProfiles) : List.of("default"));

		// Feature flags for this report
		List<String> features = new ArrayList<>();
		if (this.properties.isIncludeOrigins()) {
			features.add("bean-origins");
		}
		if (this.properties.isDetectUnusedJars()) {
			features.add("unused-jar-detection");
		}
		if (this.properties.isIncludeConfidence()) {
			features.add("confidence-scores");
		}
		if (this.properties.isMarkdownSummary()) {
			features.add("markdown-export");
		}
		metadata.put("enabledFeatures", features);

		return metadata;
	}

	private Map<String, Object> buildConfiguration() {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put("enabled", this.properties.isEnabled());
		config.put("includeOrigins", this.properties.isIncludeOrigins());
		config.put("includeConfidence", this.properties.isIncludeConfidence());
		config.put("detectUnusedJars", this.properties.isDetectUnusedJars());
		config.put("markdownSummary", this.properties.isMarkdownSummary());
		config.put("outputDir", this.properties.getOutputDir());
		config.put("policiesFailOnViolation", this.properties.isPoliciesFailOnViolation());
		config.put("cacheTtlMs", this.properties.getCacheTtl());
		return config;
	}

	private Map<String, Object> buildAutoConfigurationReport() {
		Map<String, Object> result = new LinkedHashMap<>();

		ConditionEvaluationReport cer = ConditionEvaluationReport.get(
				this.applicationContext.getBeanFactory());

		if (cer == null) {
			result.put("applied", List.of());
			result.put("appliedCount", 0);
			result.put("skipped", List.of());
			result.put("skippedCount", 0);
			result.put("skippedDetails", List.of());
			result.put("exclusions", List.of());
			return result;
		}

		List<String> applied = new ArrayList<>();
		List<String> skipped = new ArrayList<>();
		List<Map<String, Object>> skippedDetails = new ArrayList<>();

		for (Map.Entry<String, ConditionAndOutcomes> entry : cer.getConditionAndOutcomesBySource().entrySet()) {
			String className = entry.getKey();
			ConditionAndOutcomes outcomes = entry.getValue();

			if (outcomes.isFullMatch()) {
				applied.add(className);
			}
			else {
				skipped.add(className);
				skippedDetails.add(buildSkippedDetail(className, outcomes));
			}
		}

		result.put("applied", applied);
		result.put("appliedCount", applied.size());
		result.put("skipped", skipped);
		result.put("skippedCount", skipped.size());
		result.put("skippedDetails", skippedDetails);
		result.put("exclusions", new ArrayList<>(cer.getExclusions()));

		return result;
	}

	private Map<String, Object> buildSkippedDetail(String className, ConditionAndOutcomes outcomes) {
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("className", className);

		List<String> reasons = new ArrayList<>();
		for (ConditionAndOutcome cao : outcomes) {
			if (!cao.getOutcome().isMatch()) {
				reasons.add(cao.getOutcome().getMessage());
			}
		}
		detail.put("reasons", reasons);

		return detail;
	}

	private Map<String, Object> buildStarterReport() {
		Map<String, Object> result = new LinkedHashMap<>();

		StarterAnalysisResult analysis = this.starterAnalyzer.analyze();

		// Convert to serializable format
		result.put("used", formatStarters(analysis.usedStarters()));
		result.put("usedCount", analysis.usedStarters().size());
		result.put("unused", formatStarters(analysis.unusedStarters()));
		result.put("unusedCount", analysis.unusedStarters().size());
		result.put("indeterminate", formatStarters(analysis.indeterminateStarters()));
		result.put("indeterminateCount", analysis.indeterminateStarters().size());
		result.put("totalDetected", analysis.totalStarters());

		// Analysis metadata
		Map<String, Object> analysisMetadata = new LinkedHashMap<>();
		analysisMetadata.put("matchedAutoConfigurations", analysis.matchedAutoConfigCount());
		analysisMetadata.put("excludedAutoConfigurations", analysis.excludedAutoConfigCount());
		result.put("analysisMetadata", analysisMetadata);

		return result;
	}

	private List<Map<String, Object>> formatStarters(List<StarterInfo> starters) {
		List<Map<String, Object>> result = new ArrayList<>();
		for (StarterInfo starter : starters) {
			Map<String, Object> starterMap = new LinkedHashMap<>();
			starterMap.put("name", starter.name());
			starterMap.put("coordinate", starter.coordinate());
			starterMap.put("groupId", starter.groupId());
			starterMap.put("artifactId", starter.artifactId());
			starterMap.put("version", starter.version());
			starterMap.put("category", starter.category());
			starterMap.put("location", starter.location());
			starterMap.put("status", starter.status().name());
			result.add(starterMap);
		}
		return result;
	}

	private List<Map<String, Object>> buildBeanOriginReport() {
		List<Map<String, Object>> origins = new ArrayList<>();

		// Get origins from the BeanPostProcessor
		Map<String, String> beanOrigins = this.beanOriginTracker.getBeanOrigins();

		ConfigurableListableBeanFactory beanFactory = this.applicationContext.getBeanFactory();

		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition bd = beanFactory.getBeanDefinition(beanName);

			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("bean", beanName);

			// Get the resource description from bean definition
			String resource = bd.getResourceDescription();
			if (resource != null) {
				entry.put("definitionLocation", sanitizePath(resource));
			}

			// Get the code source from runtime tracking
			String codeSource = beanOrigins.get(beanName);
			if (codeSource != null) {
				entry.put("codeSource", codeSource);
			}

			// Bean class
			String beanClassName = bd.getBeanClassName();
			if (beanClassName != null) {
				entry.put("type", beanClassName);
			}

			// Scope
			String scope = bd.getScope();
			entry.put("scope", (scope != null && !scope.isEmpty()) ? scope : "singleton");

			origins.add(entry);
		}

		return origins;
	}

	private Map<String, Object> buildSuggestions() {
		Map<String, Object> suggestions = new LinkedHashMap<>();

		StarterAnalysisResult analysis = this.starterAnalyzer.analyze();

		// Unused starter suggestions
		List<Map<String, Object>> unusedStarterSuggestions = new ArrayList<>();
		for (StarterInfo starter : analysis.unusedStarters()) {
			Map<String, Object> suggestion = new LinkedHashMap<>();
			suggestion.put("type", "UNUSED_STARTER");
			suggestion.put("artifact", starter.coordinate());
			suggestion.put("message", String.format(
					"Starter '%s' appears unused. Consider removing it from your build file.",
					starter.artifactId()));
			if (this.properties.isIncludeConfidence()) {
				suggestion.put("confidence", "MEDIUM");
				suggestion.put("confidenceNote",
						"Based on auto-configuration matching. May be a transitive dependency.");
			}
			unusedStarterSuggestions.add(suggestion);
		}
		suggestions.put("unusedStarters", unusedStarterSuggestions);

		// General optimization tips based on detected patterns
		List<String> optimizationTips = new ArrayList<>();

		if (analysis.unusedStarters().size() > 2) {
			optimizationTips.add("Multiple unused starters detected. Review your dependencies for potential cleanup.");
		}

		if (analysis.indeterminateStarters().size() > analysis.usedStarters().size()) {
			optimizationTips.add("Many starters could not be analyzed. Consider using explicit imports.");
		}

		suggestions.put("optimizationTips", optimizationTips);

		if (this.properties.isIncludeConfidence()) {
			suggestions.put("confidenceNote",
					"Suggestions are heuristic-based and may include false positives. Always verify before removing dependencies.");
		}

		return suggestions;
	}

	private Map<String, Object> buildSummary(Map<String, Object> autoConfigReport) {
		Map<String, Object> summary = new LinkedHashMap<>();

		// Bean counts
		summary.put("totalBeans", this.applicationContext.getBeanDefinitionCount());
		summary.put("singletonBeans", countBeansByScope("singleton"));
		summary.put("prototypeBeans", countBeansByScope("prototype"));

		// Auto-configuration counts
		summary.put("appliedAutoConfigurations", autoConfigReport.get("appliedCount"));
		summary.put("skippedAutoConfigurations", autoConfigReport.get("skippedCount"));

		// Starter counts
		StarterAnalysisResult analysis = this.starterAnalyzer.analyze();
		summary.put("totalStarters", analysis.totalStarters());
		summary.put("usedStarters", analysis.usedStarters().size());
		summary.put("unusedStarters", analysis.unusedStarters().size());
		summary.put("indeterminateStarters", analysis.indeterminateStarters().size());

		// Efficiency score (simple heuristic)
		int total = analysis.totalStarters();
		int unused = analysis.unusedStarters().size();
		if (total > 0) {
			double efficiency = ((double) (total - unused) / total) * 100;
			summary.put("starterEfficiencyPercent", Math.round(efficiency));
		}

		return summary;
	}

	private int countBeansByScope(String scope) {
		int count = 0;
		ConfigurableListableBeanFactory beanFactory = this.applicationContext.getBeanFactory();
		for (String name : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition bd = beanFactory.getBeanDefinition(name);
			String beanScope = bd.getScope();
			if ((beanScope == null || beanScope.isEmpty()) && "singleton".equals(scope)) {
				count++;
			}
			else if (scope.equals(beanScope)) {
				count++;
			}
		}
		return count;
	}

	private void applyCustomizers(Map<String, Object> report) {
		for (UsageReportCustomizer customizer : this.customizers) {
			try {
				customizer.customize(report, this.applicationContext, this.environment);
			}
			catch (Exception ex) {
				// Log and continue with other customizers
			}
		}
	}

	private List<Map<String, Object>> evaluatePolicies(Map<String, Object> report) {
		List<Map<String, Object>> allIssues = new ArrayList<>();

		for (UsagePolicy policy : this.policies) {
			try {
				PolicyResult result = policy.evaluate(report, this.applicationContext, this.environment);
				if (result == null) {
					continue;
				}

				// Add violations
				for (String violation : result.violations()) {
					Map<String, Object> issue = new LinkedHashMap<>();
					issue.put("policy", policy.getClass().getSimpleName());
					issue.put("severity", "VIOLATION");
					issue.put("message", violation);
					allIssues.add(issue);
				}

				// Add warnings
				for (String warning : result.warnings()) {
					Map<String, Object> issue = new LinkedHashMap<>();
					issue.put("policy", policy.getClass().getSimpleName());
					issue.put("severity", "WARNING");
					issue.put("message", warning);
					allIssues.add(issue);
				}
			}
			catch (Exception ex) {
				// Log and continue with other policies
				Map<String, Object> issue = new LinkedHashMap<>();
				issue.put("policy", policy.getClass().getSimpleName());
				issue.put("severity", "ERROR");
				issue.put("message", "Policy evaluation failed: " + ex.getMessage());
				allIssues.add(issue);
			}
		}

		return allIssues;
	}

	private String sanitizePath(String pathLike) {
		try {
			String userHome = System.getProperty("user.home", "");
			String cwd = Path.of("").toAbsolutePath().toString();
			String result = pathLike;

			if (!userHome.isEmpty()) {
				result = result.replace(userHome, "~");
			}
			result = result.replace(cwd, ".");

			// Normalize Windows paths
			result = result.replace("\\", "/");

			// Strip common build tool cache paths for readability
			result = result.replaceAll("/\\.gradle/caches/[^/]+/", "/.gradle/.../");
			result = result.replaceAll("/\\.m2/repository/", "/.m2/.../");

			return result;
		}
		catch (Exception ex) {
			return pathLike;
		}
	}

	private String getSpringBootVersion() {
		try {
			Package pkg = org.springframework.boot.SpringBootVersion.class.getPackage();
			String version = (pkg != null) ? pkg.getImplementationVersion() : null;
			return (version != null) ? version : "unknown";
		}
		catch (Exception ex) {
			return "unknown";
		}
	}

}
