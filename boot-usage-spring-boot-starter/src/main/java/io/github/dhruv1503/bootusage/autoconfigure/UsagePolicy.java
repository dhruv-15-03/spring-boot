package io.github.dhruv1503.bootusage.autoconfigure;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * SPI to validate a usage report and return violation descriptions.
 */
@FunctionalInterface
public interface UsagePolicy {

    List<String> evaluate(Map<String, Object> report, ApplicationContext context, Environment environment);
}
