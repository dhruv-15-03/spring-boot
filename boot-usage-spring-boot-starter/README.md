# boot-usage-spring-boot-starter

Experimental starter that provides runtime usage reporting and an actuator endpoint `/actuator/bootusage`.

This project is maintained as a third-party starter per Spring Boot maintainers' guidance. If it gains traction, it can be listed in the [community starters](https://github.com/spring-projects/spring-boot/tree/main/starter#readme).

## Features
- Property-gated: `spring.boot.usage.report.enabled=true`
- Generates a structured usage report at runtime (placeholder data for now)
- Exposes report at `/actuator/bootusage`

## Getting started
Add the dependency (replace version as needed):

```gradle
implementation("io.github.dhruv1503:boot-usage-spring-boot-starter:0.1.0-SNAPSHOT")
```

Expose the endpoint and enable the feature:

```properties
spring.boot.usage.report.enabled=true
management.endpoints.web.exposure.include=bootusage
management.endpoint.bootusage.enabled=true
```

Call the endpoint:

```
GET /actuator/bootusage
```

Optional query parameter `force=true` bypasses cache:

```
GET /actuator/bootusage?force=true
```

Optional properties:
- `spring.boot.usage.report.cache-ttl`: millis, 0 = no cache
- `spring.boot.usage.report.include-origins`: default false
- `spring.boot.usage.report.include-confidence`: default false
- `spring.boot.usage.report.detect-unused-jars`: default false
- `spring.boot.usage.report.markdown-summary`: default false
- `spring.boot.usage.report.output-dir`: default `build/boot-usage`
- `spring.boot.usage.report.policies.fail-on-violation`: default false

## Status
- Initial scaffold with endpoint and properties; internals to be iterated.

## License
Apache License 2.0
