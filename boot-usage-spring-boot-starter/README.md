# boot-usage-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dhruv1503/boot-usage-spring-boot-starter.svg)](https://search.maven.org/artifact/io.github.dhruv1503/boot-usage-spring-boot-starter)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Spring Boot starter that provides comprehensive runtime usage analysis and reporting. Exposes an actuator endpoint at `/actuator/bootusage` with insights **beyond what `/actuator/conditions` provides**.

> 💡 This project is maintained as a third-party starter per Spring Boot maintainers' guidance. See the [Spring Boot community starters](https://github.com/spring-projects/spring-boot/tree/main/starter#readme) for listing criteria.

## Why This Starter?

While Spring Boot's built-in `/actuator/conditions` endpoint shows auto-configuration decisions, this starter provides:

| Feature | `/actuator/conditions` | `/actuator/bootusage` |
|---------|------------------------|------------------------|
| Auto-configuration matching | ✅ | ✅ |
| **Starter-level analysis** | ❌ | ✅ |
| **Unused starter detection** | ❌ | ✅ |
| **Bean origin tracking** | ❌ | ✅ |
| **Policy enforcement SPI** | ❌ | ✅ |
| **Markdown export** | ❌ | ✅ |
| **Efficiency scoring** | ❌ | ✅ |

## Features

- 🚀 **Starter Usage Analysis**: Identifies which starters are actively used, unused, or indeterminate
- 📊 **Efficiency Scoring**: Calculates dependency efficiency percentage
- 🔍 **Bean Origin Tracking**: Maps beans to their JAR source with sanitized paths
- 📝 **Report Export**: JSON and Markdown output for CI/CD integration
- 🛡️ **Policy Enforcement**: Custom SPI to enforce architectural constraints
- ⚡ **Report Caching**: Configurable TTL for performance
- 🎯 **Schema Versioned**: Stable API with semantic versioning

## Quick Start

### 1. Add Dependency

**Gradle (Kotlin DSL):**
```kotlin
implementation("io.github.dhruv1503:boot-usage-spring-boot-starter:1.0.0")
```

**Gradle (Groovy):**
```groovy
implementation 'io.github.dhruv1503:boot-usage-spring-boot-starter:1.0.0'
```

**Maven:**
```xml
<dependency>
    <groupId>io.github.dhruv1503</groupId>
    <artifactId>boot-usage-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Enable the Feature

```properties
# Required
spring.boot.usage.report.enabled=true

# Expose the actuator endpoint
management.endpoints.web.exposure.include=bootusage
management.endpoint.bootusage.enabled=true
```

### 3. Access the Report

```bash
# Get usage report
curl http://localhost:8080/actuator/bootusage

# Force regeneration (bypass cache)
curl http://localhost:8080/actuator/bootusage?force=true
```

## Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `spring.boot.usage.report.enabled` | boolean | `false` | Enable the usage report feature |
| `spring.boot.usage.report.cache-ttl` | long | `0` | Cache TTL in milliseconds (0 = no cache) |
| `spring.boot.usage.report.include-origins` | boolean | `false` | Include bean origin tracking |
| `spring.boot.usage.report.include-confidence` | boolean | `false` | Include confidence scores for suggestions |
| `spring.boot.usage.report.detect-unused-jars` | boolean | `false` | Detect potentially unused JARs |
| `spring.boot.usage.report.markdown-summary` | boolean | `false` | Generate Markdown summary file |
| `spring.boot.usage.report.output-dir` | String | `build/boot-usage` | Directory for report files |
| `spring.boot.usage.report.policies-fail-on-violation` | boolean | `false` | Fail startup on policy violations |

## Report Structure

The report uses schema version `1.0.0` and includes:

```json
{
  "schemaVersion": "1.0.0",
  "metadata": {
    "generatedAt": "2024-01-15T10:30:00Z",
    "springBootVersion": "3.3.5",
    "applicationName": "my-app",
    "activeProfiles": ["prod"],
    "enabledFeatures": ["bean-origins", "markdown-export"]
  },
  "configuration": { ... },
  "autoConfiguration": {
    "applied": [...],
    "appliedCount": 42,
    "skipped": [...],
    "skippedCount": 15
  },
  "starters": {
    "used": [...],
    "usedCount": 5,
    "unused": [...],
    "unusedCount": 2,
    "indeterminate": [...]
  },
  "beanOrigins": [...],
  "suggestions": {
    "unusedStarters": [...],
    "optimizationTips": [...]
  },
  "summary": {
    "totalBeans": 150,
    "starterEfficiencyPercent": 85
  },
  "policyViolations": [...]
}
```

## Extensibility SPIs

### Custom Policy

Implement `UsagePolicy` to enforce architectural constraints:

```java
@Component
public class NoDevToolsInProductionPolicy implements UsagePolicy {

    @Override
    public PolicyResult evaluate(Map<String, Object> report, 
            ApplicationContext context, Environment env) {
        
        List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        
        if (activeProfiles.contains("prod")) {
            Map<String, Object> starters = (Map<String, Object>) report.get("starters");
            List<Map<String, Object>> used = (List<Map<String, Object>>) starters.get("used");
            
            boolean hasDevTools = used.stream()
                .anyMatch(s -> "spring-boot-devtools".equals(s.get("artifactId")));
            
            if (hasDevTools) {
                return PolicyResult.violation(
                    "DevTools must not be used in production");
            }
        }
        return PolicyResult.ok();
    }
}
```

### Custom Report Customizer

Implement `UsageReportCustomizer` to add custom data:

```java
@Component
public class GitInfoCustomizer implements UsageReportCustomizer {

    @Override
    public void customize(Map<String, Object> report, 
            ApplicationContext context, Environment env) {
        
        Map<String, String> gitInfo = new LinkedHashMap<>();
        gitInfo.put("branch", env.getProperty("git.branch", "unknown"));
        gitInfo.put("commit", env.getProperty("git.commit.id.abbrev", "unknown"));
        report.put("git", gitInfo);
    }
}
```

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Run with usage report
  run: ./gradlew bootRun &
  
- name: Wait for startup
  run: sleep 30
  
- name: Generate usage report
  run: curl http://localhost:8080/actuator/bootusage > usage-report.json
  
- name: Upload report
  uses: actions/upload-artifact@v3
  with:
    name: boot-usage-report
    path: usage-report.json
```

### Fail Build on Policy Violations

```properties
spring.boot.usage.report.policies-fail-on-violation=true
```

This will throw `UsagePolicyViolationException` and fail startup if any policies return violations.

## Building from Source

```bash
git clone https://github.com/dhruv-15-03/boot-usage-spring-boot-starter.git
cd boot-usage-spring-boot-starter
./gradlew build
```

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

Apache License 2.0 - see [LICENSE](LICENSE) for details.

## Acknowledgments

- Thanks to the Spring Boot team for their guidance
- Inspired by the need for better dependency hygiene tooling
