package io.github.dhruv1503.bootusage.endpoint;

import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import io.github.dhruv1503.bootusage.autoconfigure.UsageReportService;

@Endpoint(id = "bootusage")
public class UsageEndpoint {

    private final UsageReportService reportService;

    public UsageEndpoint(UsageReportService reportService) {
        this.reportService = reportService;
    }

    @ReadOperation
    public Map<String, Object> usage() {
        return this.reportService.generateReport();
    }
}
