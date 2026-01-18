package io.github.dhruv1503.bootusage.autoconfigure;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Enforces usage policies during application startup.
 * <p>
 * When {@code spring.boot.usage.report.policies-fail-on-violation=true}, this component
 * will fail the application context if any {@link UsagePolicy} returns violations.
 * <p>
 * This enables teams to enforce architectural constraints at startup, such as:
 * <ul>
 *   <li>Forbidding certain starters in production</li>
 *   <li>Requiring specific auto-configurations to be active</li>
 *   <li>Detecting and rejecting unused dependencies</li>
 *   <li>Enforcing minimum starter efficiency thresholds</li>
 * </ul>
 * <p>
 * Policies with severity {@code VIOLATION} will cause startup failure when
 * fail-on-violation is enabled. Policies with severity {@code WARNING} will
 * be logged but will not fail startup.
 *
 * @author Dhruv Bansal
 * @since 1.0.0
 * @see UsagePolicy
 * @see UsagePolicyViolationException
 */
public class UsagePolicyEnforcer implements SmartInitializingSingleton {

	private static final Log logger = LogFactory.getLog(UsagePolicyEnforcer.class);

	private final UsageReportService service;

	private final UsageReportProperties properties;

	/**
	 * Create a new {@code UsagePolicyEnforcer}.
	 * @param service the usage report service
	 * @param properties the usage report properties
	 */
	public UsagePolicyEnforcer(UsageReportService service, UsageReportProperties properties) {
		this.service = service;
		this.properties = properties;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterSingletonsInstantiated() {
		if (!this.properties.isPoliciesFailOnViolation()) {
			logger.debug("Policy enforcement disabled (policies-fail-on-violation=false)");
			return;
		}

		Map<String, Object> report = this.service.generateReport(true);
		Object policyViolations = report.get("policyViolations");

		if (!(policyViolations instanceof List<?> list) || list.isEmpty()) {
			logger.debug("All usage policies passed successfully");
			return;
		}

		// Separate violations from warnings
		StringBuilder violations = new StringBuilder();
		StringBuilder warnings = new StringBuilder();
		int violationCount = 0;
		int warningCount = 0;

		for (Object item : list) {
			if (item instanceof Map<?, ?> issue) {
				String severity = String.valueOf(issue.getOrDefault("severity", "VIOLATION"));
				String policy = String.valueOf(issue.getOrDefault("policy", "Unknown"));
				String message = String.valueOf(issue.getOrDefault("message", ""));

				if ("WARNING".equals(severity)) {
					warnings.append("  - [").append(policy).append("] ").append(message).append("\n");
					warningCount++;
				}
				else {
					violations.append("  - [").append(policy).append("] ").append(message).append("\n");
					violationCount++;
				}
			}
		}

		// Log warnings
		if (warningCount > 0) {
			logger.warn("Usage policy warnings (" + warningCount + "):\n" + warnings);
		}

		// Fail on violations
		if (violationCount > 0) {
			String errorMessage = String.format(
					"Usage policy violations detected (%d violation(s)):\n%s\n" +
					"To disable this check, set spring.boot.usage.report.policies-fail-on-violation=false",
					violationCount, violations);
			logger.error(errorMessage);
			throw new UsagePolicyViolationException(errorMessage, violationCount);
		}

		logger.info("All usage policies passed (" + warningCount + " warning(s))");
	}

}
