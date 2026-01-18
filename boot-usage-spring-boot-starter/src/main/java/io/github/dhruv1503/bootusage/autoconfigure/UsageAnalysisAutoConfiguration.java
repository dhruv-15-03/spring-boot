package io.github.dhruv1503.bootusage.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Auto-configuration for the Boot Usage Report feature.
 * <p>
 * This provides runtime analysis of Spring Boot application usage including:
 * <ul>
 *   <li>Starter dependency detection and usage analysis</li>
 *   <li>Auto-configuration evaluation (beyond what /actuator/conditions provides)</li>
 *   <li>Bean origin tracking with sanitized paths</li>
 *   <li>Unused JAR detection on classpath</li>
 *   <li>Custom policy enforcement SPI</li>
 *   <li>Report persistence to disk in multiple formats</li>
 * </ul>
 * <p>
 * This feature differs from the built-in {@code /actuator/conditions} endpoint by
 * providing starter-level analysis, unused JAR detection, policy enforcement,
 * and actionable optimization suggestions.
 *
 * @author Dhruv Bansal
 * @since 1.0.0
 * @see UsageReportService
 * @see UsageReportProperties
 */
@AutoConfiguration
@EnableConfigurationProperties(UsageReportProperties.class)
@ConditionalOnProperty(prefix = "spring.boot.usage.report", name = "enabled", havingValue = "true")
public class UsageAnalysisAutoConfiguration {

    /**
     * Creates the {@link BeanOriginTrackingPostProcessor} for tracking bean origins.
     * <p>
     * This bean post processor captures the code source (JAR file or directory)
     * for each bean, enabling the usage report to identify which dependencies
     * contribute which beans.
     * @return the bean origin tracking post processor
     */
    @Bean
    public static BeanOriginTrackingPostProcessor beanOriginTrackingPostProcessor() {
        return new BeanOriginTrackingPostProcessor();
    }

    /**
     * Creates the {@link StarterUsageAnalyzer} for analyzing starter dependencies.
     * @param context the application context
     * @return the starter usage analyzer
     */
    @Bean
    @ConditionalOnMissingBean
    public StarterUsageAnalyzer starterUsageAnalyzer(ConfigurableApplicationContext context) {
        ConditionEvaluationReport conditionReport = ConditionEvaluationReport.get(
                context.getBeanFactory());
        return new StarterUsageAnalyzer(conditionReport);
    }

    /**
     * Creates the {@link UsageReportService} for generating usage reports.
     * @param properties the usage report properties
     * @param context the application context
     * @param customizers optional report customizers
     * @param policies optional usage policies
     * @param starterAnalyzer the starter usage analyzer
     * @param beanOriginTracker the bean origin tracking post processor
     * @param environment the Spring environment
     * @return the usage report service
     */
    @Bean
    @ConditionalOnMissingBean
    public UsageReportService usageReportService(UsageReportProperties properties,
            ConfigurableApplicationContext context,
            ObjectProvider<UsageReportCustomizer> customizers,
            ObjectProvider<UsagePolicy> policies,
            StarterUsageAnalyzer starterAnalyzer,
            BeanOriginTrackingPostProcessor beanOriginTracker,
            Environment environment) {
        return new UsageReportService(properties, context, customizers, policies,
                starterAnalyzer, beanOriginTracker, environment);
    }

    /**
     * Creates the {@link UsagePolicyEnforcer} for enforcing usage policies.
     * @param service the usage report service
     * @param properties the usage report properties
     * @return the usage policy enforcer
     */
    @Bean
    @ConditionalOnMissingBean
    public UsagePolicyEnforcer usagePolicyEnforcer(UsageReportService service,
            UsageReportProperties properties) {
        return new UsagePolicyEnforcer(service, properties);
    }

    /**
     * Creates the {@link UsageReportPersistence} for persisting reports to disk.
     * @param service the usage report service
     * @param properties the usage report properties
     * @return the usage report persistence
     */
    @Bean
    @ConditionalOnMissingBean
    public UsageReportPersistence usageReportPersistence(UsageReportService service,
            UsageReportProperties properties) {
        return new UsageReportPersistence(service, properties);
    }

}
