package io.github.dhruv1503.bootusage.autoconfigure;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * SPI to mutate or augment the generated report prior to exposure/persistence.
 */
@FunctionalInterface
public interface UsageReportCustomizer {

    void customize(Map<String, Object> report, ApplicationContext context, Environment environment);
}
