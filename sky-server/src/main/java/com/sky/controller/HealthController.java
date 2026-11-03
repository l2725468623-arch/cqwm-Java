package com.sky.controller;
import com.sky.mapper.DashboardMapper;
import com.sky.result.Result;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequiredArgsConstructor
public class HealthController {
    private final DashboardMapper dashboardMapper;
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        dashboardMapper.ping();
        return Result.success(Collections.singletonMap("status", "UP"));
    }
}
