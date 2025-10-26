package io.github.dhruv1503.bootusage.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(UsageReportProperties.class)
@ConditionalOnProperty(prefix = "spring.boot.usage.report", name = "enabled", havingValue = "true")
public class UsageAnalysisAutoConfiguration {

    @Bean
    public UsageReportService usageReportService(UsageReportProperties properties) {
        return new UsageReportService(properties);
    }
}
