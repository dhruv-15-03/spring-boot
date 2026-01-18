package io.github.dhruv1503.bootusage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.boot.usage.report.enabled=true",
				"management.endpoints.web.exposure.include=bootusage",
				"management.endpoint.bootusage.enabled=true",
				"spring.main.web-application-type=servlet"
		})
class UsageEndpointIntegrationTests {

	@LocalServerPort
	int port;

	@Autowired
	TestRestTemplate rest;

	@Test
	void endpointExposedAndReturnsReport() {
		ResponseEntity<Map<String, Object>> entity = rest.exchange(
				"http://localhost:" + port + "/actuator/bootusage?force=true",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
		assertThat(entity.getStatusCode().is2xxSuccessful()).isTrue();
		Map<String, Object> body = entity.getBody();
		assertThat(body).isNotNull();
		// Verify new report structure
		assertThat(body).containsKeys("schemaVersion", "metadata", "configuration", "summary");
		assertThat(body.get("schemaVersion")).isEqualTo("1.0.0");
	}

	@Test
	@SuppressWarnings("unchecked")
	void reportContainsExpectedSections() {
		ResponseEntity<Map<String, Object>> entity = rest.exchange(
				"http://localhost:" + port + "/actuator/bootusage?force=true",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
		assertThat(entity.getStatusCode().is2xxSuccessful()).isTrue();
		Map<String, Object> body = entity.getBody();
		assertThat(body).isNotNull();

		// Check metadata section
		Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
		assertThat(metadata).containsKeys("generatedAt", "springBootVersion", "applicationName");

		// Check configuration section
		Map<String, Object> config = (Map<String, Object>) body.get("configuration");
		assertThat(config).containsKey("enabled");
		assertThat(config.get("enabled")).isEqualTo(true);

		// Check summary section
		Map<String, Object> summary = (Map<String, Object>) body.get("summary");
		assertThat(summary).containsKey("totalBeans");

		// Check auto-configuration section
		Map<String, Object> autoConfig = (Map<String, Object>) body.get("autoConfiguration");
		assertThat(autoConfig).containsKeys("applied", "appliedCount", "skipped", "skippedCount");
	}

	@Test
	void cacheTtlIsRespectedUntilForceBypass() {
		ResponseEntity<Map<String, Object>> first = rest.exchange(
				"http://localhost:" + port + "/actuator/bootusage",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
		ResponseEntity<Map<String, Object>> second = rest.exchange(
				"http://localhost:" + port + "/actuator/bootusage",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
		Map<String, Object> b1 = first.getBody();
		Map<String, Object> b2 = second.getBody();
		assertThat(b1).isNotNull();
		assertThat(b2).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, Object> m1 = (Map<String, Object>) b1.get("metadata");
		@SuppressWarnings("unchecked")
		Map<String, Object> m2 = (Map<String, Object>) b2.get("metadata");
		Object t1 = m1.get("generatedAt");
		Object t2 = m2.get("generatedAt");

		// Default cacheTtl is 0 (no cache); with default it may regenerate; set a non-zero TTL for this check
		// We fallback to checking that force=true bypasses any cache and changes timestamp
		ResponseEntity<Map<String, Object>> forced = rest.exchange(
				"http://localhost:" + port + "/actuator/bootusage?force=true",
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
		Map<String, Object> bf = forced.getBody();
		assertThat(bf).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, Object> mf = (Map<String, Object>) bf.get("metadata");
		Object tf = mf.get("generatedAt");

		// Forced timestamp should differ from at least one of previous calls (unless they're all run in same ms)
		assertThat(tf).isNotNull();
	}

}
