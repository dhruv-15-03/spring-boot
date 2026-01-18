package io.github.dhruv1503.bootusage;

import java.util.List;
import java.util.Map;

import io.github.dhruv1503.bootusage.autoconfigure.UsageAnalysisAutoConfiguration;
import io.github.dhruv1503.bootusage.autoconfigure.UsagePolicy;
import io.github.dhruv1503.bootusage.autoconfigure.UsagePolicy.PolicyResult;
import io.github.dhruv1503.bootusage.autoconfigure.UsagePolicyViolationException;
import io.github.dhruv1503.bootusage.autoconfigure.UsageReportCustomizer;
import io.github.dhruv1503.bootusage.autoconfigure.UsageReportService;
import io.github.dhruv1503.bootusage.autoconfigure.UsageEndpointAutoConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class UsagePoliciesIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					UsageAnalysisAutoConfiguration.class,
					UsageEndpointAutoConfiguration.class));

	@Test
	void startupFailsOnPolicyViolationWhenConfigured() {
		this.contextRunner
				.withUserConfiguration(PolicyConfig.class)
				.withPropertyValues(
						"spring.boot.usage.report.enabled=true",
						"spring.boot.usage.report.policies-fail-on-violation=true")
				.run((context) -> {
					assertThat(context.getStartupFailure()).isNotNull();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(UsagePolicyViolationException.class)
							.hasStackTraceContaining("Usage policy violations detected");
				});
	}

	@Test
	@SuppressWarnings("unchecked")
	void policiesAreReportedAndCustomizerAppliedWhenNonFatal() {
		this.contextRunner
				.withUserConfiguration(PolicyAndCustomizerConfig.class)
				.withPropertyValues(
						"spring.boot.usage.report.enabled=true",
						"spring.boot.usage.report.policies-fail-on-violation=false")
				.run((context) -> {
					assertThat(context).hasNotFailed();
					UsageReportService service = context.getBean(UsageReportService.class);
					Map<String, Object> report = service.generateReport(true);

					// Check policy violations are reported
					assertThat(report).containsKey("policyViolations");
					List<Map<String, Object>> violations =
							(List<Map<String, Object>>) report.get("policyViolations");
					assertThat(violations).isNotEmpty();
					assertThat(violations.get(0).get("message")).isEqualTo("always-bad");

					// Check customizer was applied
					assertThat(report).containsEntry("customKey", "customValue");
				});
	}

	@Test
	void warningsDoNotFailStartup() {
		this.contextRunner
				.withUserConfiguration(WarningPolicyConfig.class)
				.withPropertyValues(
						"spring.boot.usage.report.enabled=true",
						"spring.boot.usage.report.policies-fail-on-violation=true")
				.run((context) -> {
					// Warnings should not cause startup failure
					assertThat(context).hasNotFailed();
					UsageReportService service = context.getBean(UsageReportService.class);
					Map<String, Object> report = service.generateReport(true);

					@SuppressWarnings("unchecked")
					List<Map<String, Object>> violations =
							(List<Map<String, Object>>) report.get("policyViolations");
					assertThat(violations).hasSize(1);
					assertThat(violations.get(0).get("severity")).isEqualTo("WARNING");
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class PolicyConfig {

		@Bean
		UsagePolicy alwaysFailPolicy() {
			return (report, ctx, env) -> PolicyResult.violation("always-bad");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WarningPolicyConfig {

		@Bean
		UsagePolicy warningPolicy() {
			return (report, ctx, env) -> PolicyResult.warning("just-a-warning");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PolicyAndCustomizerConfig extends PolicyConfig {

		@Bean
		UsageReportCustomizer customizer() {
			return (report, ctx, env) -> report.put("customKey", "customValue");
		}

	}

}
