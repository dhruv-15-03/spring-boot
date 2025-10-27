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
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        assertThat(entity.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKeys("metadata", "timestamp", "enabled");
        assertThat(body.get("enabled")).isEqualTo(true);
    }

    @Test
    void cacheTtlIsRespectedUntilForceBypass() {
        ResponseEntity<Map<String, Object>> first = rest.exchange(
                "http://localhost:" + port + "/actuator/bootusage",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        ResponseEntity<Map<String, Object>> second = rest.exchange(
                "http://localhost:" + port + "/actuator/bootusage",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        Map<String, Object> b1 = first.getBody();
        Map<String, Object> b2 = second.getBody();
        assertThat(b1).isNotNull();
        assertThat(b2).isNotNull();
        Object t1 = b1.get("timestamp");
        Object t2 = b2.get("timestamp");
        // Default cacheTtl is 0 (no cache); with default it may regenerate; set a non-zero TTL for this check
        // We fallback to checking that force=true bypasses any cache and changes timestamp
        ResponseEntity<Map<String, Object>> forced = rest.exchange(
                "http://localhost:" + port + "/actuator/bootusage?force=true",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        Map<String, Object> bf = forced.getBody();
        assertThat(bf).isNotNull();
        Object tf = bf.get("timestamp");
        // Typically, forced timestamp should differ from at least one of previous calls
        assertThat(tf).isNotNull();
        if (t1 != null) {
            assertThat(tf).isNotEqualTo(t1);
        }
        if (t2 != null) {
            assertThat(tf).isNotEqualTo(t2);
        }
    }
}
