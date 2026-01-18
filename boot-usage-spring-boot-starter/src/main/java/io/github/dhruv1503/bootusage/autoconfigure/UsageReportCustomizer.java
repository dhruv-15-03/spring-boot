package io.github.dhruv1503.bootusage.autoconfigure;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * SPI for customizing the generated usage report before it's exposed or persisted.
 * <p>
 * Implementations can add, modify, or remove entries from the report map.
 * Multiple customizers are applied in order of their bean definition.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * @Component
 * public class TeamMetadataCustomizer implements UsageReportCustomizer {
 *     @Override
 *     public void customize(Map<String, Object> report,
 *             ApplicationContext context, Environment env) {
 *         Map<String, Object> teamInfo = new LinkedHashMap<>();
 *         teamInfo.put("team", env.getProperty("app.team", "unknown"));
 *         teamInfo.put("environment", env.getProperty("spring.profiles.active", "default"));
 *         report.put("teamMetadata", teamInfo);
 *     }
 * }
 * }</pre>
 *
 * @author Dhruv
 * @since 0.1.0
 */
@FunctionalInterface
public interface UsageReportCustomizer {

    /**
     * Customizes the usage report.
     *
     * @param report the report map to customize (mutable)
     * @param context the Spring application context
     * @param environment the Spring environment
     */
    void customize(Map<String, Object> report, ApplicationContext context, Environment environment);
}
