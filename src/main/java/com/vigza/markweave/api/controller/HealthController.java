package com.vigza.markweave.api.controller;

import com.vigza.markweave.common.Result;
import com.vigza.markweave.common.Constants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Result<Map<String, Object>> health() {
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> runtime = new LinkedHashMap<>();
        payload.put("status", Constants.Health.STATUS_OK);
        payload.put("timestamp", Instant.now().toString());
        payload.put("app", resolveApplicationInfo());
        runtime.put("uptimeMillis", uptimeMillis);
        runtime.put("javaVersion", System.getProperty("java.version"));
        runtime.put("osName", System.getProperty("os.name"));
        payload.put("runtime", runtime);
        return Result.success(payload);
    }

    private Map<String, Object> resolveApplicationInfo() {
        Map<String, Object> app = new LinkedHashMap<>();
        Package appPackage = HealthController.class.getPackage();
        String implementationVersion = appPackage == null ? null : appPackage.getImplementationVersion();
        app.put("name", appPackage == null ? Constants.Health.DEFAULT_APP_NAME : appPackage.getName());
        app.put("version", implementationVersion == null ? "unknown" : implementationVersion);
        return app;
    }
}
