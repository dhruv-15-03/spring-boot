package io.github.dhruv1503.bootusage.autoconfigure;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * SPI for defining usage policies that validate the generated report.
 * <p>
 * Implementations can inspect the usage report and return a list of violation
 * messages. When {@code spring.boot.usage.report.policies-fail-on-violation=true},
 * any violations will cause the application to fail at startup.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * @Component
 * public class NoDevToolsInProductionPolicy implements UsagePolicy {
 *     @Override
 *     public List<String> evaluate(Map<String, Object> report,
 *             ApplicationContext context, Environment env) {
 *         if ("prod".equals(env.getProperty("spring.profiles.active"))) {
 *             Map<String, Object> starters = (Map<String, Object>) report.get("starters");
 *             List<String> used = (List<String>) starters.get("used");
 *             if (used.stream().anyMatch(s -> s.contains("devtools"))) {
 *                 return List.of("DevTools starter is not allowed in production");
 *             }
 *         }
 *         return List.of();
 *     }
 * }
 * }</pre>
 *
 * @author Dhruv
 * @since 0.1.0
 * @see UsagePolicyEnforcer
 */
@FunctionalInterface
public interface UsagePolicy {

    /**
     * Evaluates the usage report and returns any policy violations.
     *
     * @param report the generated usage report
     * @param context the Spring application context
     * @param environment the Spring environment
     * @return a list of violation messages, or an empty list if no violations
     */
    List<String> evaluate(Map<String, Object> report, ApplicationContext context, Environment environment);
}
