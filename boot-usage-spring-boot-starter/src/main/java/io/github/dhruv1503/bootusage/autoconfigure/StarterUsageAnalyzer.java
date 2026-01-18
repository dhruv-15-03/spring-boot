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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Analyzes Spring Boot starters used in the application.
 * <p>
 * Discovers starters from the classpath, resolves their Maven coordinates,
 * and classifies them as used, unused, or indeterminate based on auto-configuration
 * matching.
 *
 * @author Dhruv Bansal
 * @since 1.0.0
 */
public class StarterUsageAnalyzer {

	private static final Log logger = LogFactory.getLog(StarterUsageAnalyzer.class);

	private static final String AUTO_CONFIGURATION_LOCATION = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

	private static final Pattern COORDINATE_PATTERN = Pattern.compile(
			"([a-zA-Z0-9._-]+):([a-zA-Z0-9._-]+)(?::([a-zA-Z0-9._-]+))?");

	/**
	 * Mapping of starter artifact IDs to their expected auto-configuration classes.
	 * This helps determine which starters are actually being used.
	 */
	private static final Map<String, List<String>> STARTER_AUTOCONFIG_MAPPING = new LinkedHashMap<>();

	/**
	 * Mapping of keywords in starter names to categories.
	 */
	private static final Map<String, String> CATEGORY_KEYWORDS = new LinkedHashMap<>();

	static {
		// Initialize starter to auto-configuration mappings
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-web", List.of(
				"org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration",
				"org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-webflux", List.of(
				"org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-data-jpa", List.of(
				"org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
				"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-data-mongodb", List.of(
				"org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
				"org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-data-redis", List.of(
				"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-security", List.of(
				"org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
				"org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-actuator", List.of(
				"org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration",
				"org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-validation", List.of(
				"org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-cache", List.of(
				"org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-aop", List.of(
				"org.springframework.boot.autoconfigure.aop.AopAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-jdbc", List.of(
				"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
				"org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-amqp", List.of(
				"org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-mail", List.of(
				"org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-quartz", List.of(
				"org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration"));
		STARTER_AUTOCONFIG_MAPPING.put("spring-boot-starter-batch", List.of(
				"org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration"));

		// Initialize category keywords
		CATEGORY_KEYWORDS.put("web", "Web");
		CATEGORY_KEYWORDS.put("data", "Data Access");
		CATEGORY_KEYWORDS.put("security", "Security");
		CATEGORY_KEYWORDS.put("actuator", "Observability");
		CATEGORY_KEYWORDS.put("cache", "Caching");
		CATEGORY_KEYWORDS.put("amqp", "Messaging");
		CATEGORY_KEYWORDS.put("kafka", "Messaging");
		CATEGORY_KEYWORDS.put("activemq", "Messaging");
		CATEGORY_KEYWORDS.put("artemis", "Messaging");
		CATEGORY_KEYWORDS.put("mail", "Integration");
		CATEGORY_KEYWORDS.put("batch", "Batch Processing");
		CATEGORY_KEYWORDS.put("quartz", "Scheduling");
		CATEGORY_KEYWORDS.put("test", "Testing");
		CATEGORY_KEYWORDS.put("devtools", "Developer Tools");
		CATEGORY_KEYWORDS.put("validation", "Validation");
		CATEGORY_KEYWORDS.put("aop", "AOP");
		CATEGORY_KEYWORDS.put("oauth", "Security");
		CATEGORY_KEYWORDS.put("logging", "Logging");
		CATEGORY_KEYWORDS.put("cloud", "Cloud");
	}

	private final ConditionEvaluationReport conditionReport;

	/**
	 * Create a new {@code StarterUsageAnalyzer}.
	 * @param conditionReport the condition evaluation report
	 */
	public StarterUsageAnalyzer(ConditionEvaluationReport conditionReport) {
		this.conditionReport = conditionReport;
	}

	/**
	 * Analyze all starters on the classpath.
	 * @return analysis result containing starter classifications
	 */
	public StarterAnalysisResult analyze() {
		Set<StarterInfo> detectedStarters = discoverStarters();
		Set<String> matchedAutoConfigs = getMatchedAutoConfigurations();
		Set<String> excludedAutoConfigs = getExcludedAutoConfigurations();

		List<StarterInfo> usedStarters = new ArrayList<>();
		List<StarterInfo> unusedStarters = new ArrayList<>();
		List<StarterInfo> indeterminateStarters = new ArrayList<>();

		for (StarterInfo starter : detectedStarters) {
			UsageStatus status = determineUsageStatus(starter, matchedAutoConfigs, excludedAutoConfigs);
			starter = new StarterInfo(starter.name(), starter.groupId(), starter.artifactId(),
					starter.version(), starter.location(), starter.category(), status);

			switch (status) {
				case USED -> usedStarters.add(starter);
				case UNUSED -> unusedStarters.add(starter);
				case INDETERMINATE -> indeterminateStarters.add(starter);
			}
		}

		return new StarterAnalysisResult(
				Collections.unmodifiableList(usedStarters),
				Collections.unmodifiableList(unusedStarters),
				Collections.unmodifiableList(indeterminateStarters),
				matchedAutoConfigs.size(),
				excludedAutoConfigs.size());
	}

	/**
	 * Discover all starter JARs on the classpath.
	 */
	private Set<StarterInfo> discoverStarters() {
		Set<StarterInfo> starters = new LinkedHashSet<>();
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

		try {
			// Look for spring-boot-starter JARs
			Resource[] resources = resolver.getResources("classpath*:META-INF/MANIFEST.MF");
			for (Resource resource : resources) {
				try {
					URL url = resource.getURL();
					StarterInfo starter = extractStarterInfo(url);
					if (starter != null) {
						starters.add(starter);
					}
				}
				catch (IOException ex) {
					logger.debug("Failed to process resource: " + resource, ex);
				}
			}

			// Also scan for starters using pom.properties
			Resource[] pomResources = resolver.getResources("classpath*:META-INF/maven/**/pom.properties");
			for (Resource resource : pomResources) {
				try {
					StarterInfo starter = extractStarterFromPom(resource);
					if (starter != null && !containsStarter(starters, starter.artifactId())) {
						starters.add(starter);
					}
				}
				catch (IOException ex) {
					logger.debug("Failed to process pom.properties: " + resource, ex);
				}
			}
		}
		catch (IOException ex) {
			logger.warn("Failed to scan classpath for starters", ex);
		}

		return starters;
	}

	private boolean containsStarter(Set<StarterInfo> starters, String artifactId) {
		return starters.stream().anyMatch(s -> s.artifactId().equals(artifactId));
	}

	@Nullable
	private StarterInfo extractStarterInfo(URL manifestUrl) {
		String urlPath = manifestUrl.toString();

		// Check if this is a JAR URL
		if (!urlPath.contains(".jar")) {
			return null;
		}

		try {
			// Extract JAR location
			String jarPath;
			if (urlPath.startsWith("jar:")) {
				int jarEnd = urlPath.indexOf("!/");
				jarPath = (jarEnd > 0) ? urlPath.substring(4, jarEnd) : urlPath.substring(4);
			}
			else {
				int metaInfIndex = urlPath.indexOf("/META-INF");
				jarPath = (metaInfIndex > 0) ? urlPath.substring(0, metaInfIndex) : urlPath;
			}

			// Check if it's a starter JAR
			String jarName = extractJarName(jarPath);
			if (jarName == null || !isStarterJar(jarName)) {
				return null;
			}

			// Try to resolve Maven coordinates
			MavenCoordinates coords = resolveMavenCoordinates(manifestUrl);
			String groupId = (coords != null) ? coords.groupId() : "unknown";
			String artifactId = (coords != null) ? coords.artifactId() : jarName.replace(".jar", "");
			String version = (coords != null) ? coords.version() : extractVersionFromJarName(jarName);

			String category = determineCategory(artifactId);
			String sanitizedLocation = sanitizeLocation(jarPath);

			return new StarterInfo(artifactId, groupId, artifactId, version, sanitizedLocation,
					category, UsageStatus.INDETERMINATE);
		}
		catch (Exception ex) {
			logger.debug("Failed to extract starter info from: " + urlPath, ex);
			return null;
		}
	}

	@Nullable
	private StarterInfo extractStarterFromPom(Resource resource) throws IOException {
		try (InputStream is = resource.getInputStream()) {
			Properties props = new Properties();
			props.load(is);

			String groupId = props.getProperty("groupId");
			String artifactId = props.getProperty("artifactId");
			String version = props.getProperty("version");

			if (!StringUtils.hasText(artifactId) || !isStarterArtifact(artifactId)) {
				return null;
			}

			String location = sanitizeLocation(resource.getURL().toString());
			String category = determineCategory(artifactId);

			return new StarterInfo(artifactId, groupId, artifactId, version, location,
					category, UsageStatus.INDETERMINATE);
		}
	}

	@Nullable
	private MavenCoordinates resolveMavenCoordinates(URL manifestUrl) {
		try {
			String urlPath = manifestUrl.toString();
			if (urlPath.startsWith("jar:")) {
				JarURLConnection connection = (JarURLConnection) manifestUrl.openConnection();
				JarFile jarFile = connection.getJarFile();

				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (entry.getName().endsWith("pom.properties")) {
						try (InputStream is = jarFile.getInputStream(entry)) {
							Properties props = new Properties();
							props.load(is);
							return new MavenCoordinates(
									props.getProperty("groupId"),
									props.getProperty("artifactId"),
									props.getProperty("version"));
						}
					}
				}
			}
		}
		catch (IOException ex) {
			logger.debug("Failed to resolve Maven coordinates", ex);
		}
		return null;
	}

	@Nullable
	private String extractJarName(String path) {
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash < 0) {
			lastSlash = path.lastIndexOf('\\');
		}
		return (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
	}

	private boolean isStarterJar(String jarName) {
		return jarName.contains("spring-boot-starter") ||
				jarName.contains("-starter-") ||
				jarName.endsWith("-starter.jar");
	}

	private boolean isStarterArtifact(String artifactId) {
		return artifactId.contains("spring-boot-starter") ||
				artifactId.contains("-starter") ||
				artifactId.endsWith("-starter");
	}

	@Nullable
	private String extractVersionFromJarName(String jarName) {
		// Try to extract version from JAR name like "spring-boot-starter-web-3.2.0.jar"
		Pattern versionPattern = Pattern.compile("-(\\d+\\.\\d+\\.\\d+[^.]*)\\.jar$");
		Matcher matcher = versionPattern.matcher(jarName);
		return matcher.find() ? matcher.group(1) : null;
	}

	private String determineCategory(String artifactId) {
		String lowerArtifact = artifactId.toLowerCase();
		for (Map.Entry<String, String> entry : CATEGORY_KEYWORDS.entrySet()) {
			if (lowerArtifact.contains(entry.getKey())) {
				return entry.getValue();
			}
		}
		return "Core";
	}

	private String sanitizeLocation(String location) {
		// Remove user home directory and system-specific paths
		String userHome = System.getProperty("user.home");
		if (userHome != null) {
			location = location.replace(userHome, "~");
		}

		// Normalize Windows paths
		location = location.replace("\\", "/");

		// Extract just the repository path for Maven local repo
		int repoIndex = location.indexOf("/repository/");
		if (repoIndex > 0) {
			return "~/.m2" + location.substring(repoIndex);
		}

		return location;
	}

	private UsageStatus determineUsageStatus(StarterInfo starter, Set<String> matchedConfigs,
			Set<String> excludedConfigs) {
		List<String> expectedConfigs = STARTER_AUTOCONFIG_MAPPING.get(starter.artifactId());

		if (expectedConfigs != null) {
			// Check if any expected auto-config is matched
			for (String config : expectedConfigs) {
				if (matchedConfigs.contains(config)) {
					return UsageStatus.USED;
				}
				if (excludedConfigs.contains(config)) {
					return UsageStatus.UNUSED;
				}
			}
			// Known starter but no configs matched
			return UsageStatus.UNUSED;
		}

		// For unknown starters, try heuristic matching
		String artifactKeyword = extractKeyword(starter.artifactId());
		if (artifactKeyword != null) {
			for (String matched : matchedConfigs) {
				if (matched.toLowerCase().contains(artifactKeyword)) {
					return UsageStatus.USED;
				}
			}
		}

		return UsageStatus.INDETERMINATE;
	}

	@Nullable
	private String extractKeyword(String artifactId) {
		// Extract the main keyword from starter name
		// e.g., "spring-boot-starter-data-redis" -> "redis"
		if (artifactId.startsWith("spring-boot-starter-")) {
			String suffix = artifactId.substring("spring-boot-starter-".length());
			int lastDash = suffix.lastIndexOf('-');
			return (lastDash > 0) ? suffix.substring(lastDash + 1) : suffix;
		}
		return null;
	}

	private Set<String> getMatchedAutoConfigurations() {
		Set<String> matched = new HashSet<>();
		if (this.conditionReport == null) {
			return matched;
		}

		Map<String, ConditionEvaluationReport.ConditionAndOutcomes> conditionAndOutcomes =
				this.conditionReport.getConditionAndOutcomesBySource();

		for (Map.Entry<String, ConditionEvaluationReport.ConditionAndOutcomes> entry : conditionAndOutcomes.entrySet()) {
			String source = entry.getKey();
			if (source.contains("AutoConfiguration") && isAllMatched(entry.getValue())) {
				matched.add(source);
			}
		}

		return matched;
	}

	private boolean isAllMatched(ConditionEvaluationReport.ConditionAndOutcomes outcomes) {
		for (ConditionEvaluationReport.ConditionAndOutcome outcome : outcomes) {
			if (!outcome.getOutcome().isMatch()) {
				return false;
			}
		}
		return true;
	}

	private Set<String> getExcludedAutoConfigurations() {
		Set<String> excluded = new HashSet<>();
		if (this.conditionReport != null && this.conditionReport.getExclusions() != null) {
			excluded.addAll(this.conditionReport.getExclusions());
		}
		return excluded;
	}

	/**
	 * Usage status of a starter.
	 */
	public enum UsageStatus {
		/** Starter's auto-configurations are active. */
		USED,
		/** Starter's auto-configurations are not active. */
		UNUSED,
		/** Cannot determine if starter is used. */
		INDETERMINATE
	}

	/**
	 * Information about a detected starter.
	 */
	public record StarterInfo(
			String name,
			String groupId,
			String artifactId,
			String version,
			String location,
			String category,
			UsageStatus status) {

		/**
		 * Get the Maven coordinate string.
		 * @return coordinate in format "groupId:artifactId:version"
		 */
		public String coordinate() {
			return groupId + ":" + artifactId + ":" + version;
		}
	}

	/**
	 * Result of starter analysis.
	 */
	public record StarterAnalysisResult(
			List<StarterInfo> usedStarters,
			List<StarterInfo> unusedStarters,
			List<StarterInfo> indeterminateStarters,
			int matchedAutoConfigCount,
			int excludedAutoConfigCount) {

		/**
		 * Get total number of detected starters.
		 * @return total starter count
		 */
		public int totalStarters() {
			return usedStarters.size() + unusedStarters.size() + indeterminateStarters.size();
		}
	}

	/**
	 * Maven coordinates.
	 */
	private record MavenCoordinates(String groupId, String artifactId, String version) {
	}

}
