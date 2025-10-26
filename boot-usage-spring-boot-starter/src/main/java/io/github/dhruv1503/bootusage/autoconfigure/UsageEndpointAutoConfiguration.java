package io.github.dhruv1503.bootusage.autoconfigure;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.github.dhruv1503.bootusage.endpoint.UsageEndpoint;

@AutoConfiguration(after = UsageAnalysisAutoConfiguration.class)
@ConditionalOnClass(Endpoint.class)
@ConditionalOnBean(UsageReportService.class)
@ConditionalOnProperty(prefix = "spring.boot.usage.report", name = "enabled", havingValue = "true")
public class UsageEndpointAutoConfiguration {

    @Bean
    public UsageEndpoint usageEndpoint(UsageReportService service) {
        return new UsageEndpoint(service);
    }
}
