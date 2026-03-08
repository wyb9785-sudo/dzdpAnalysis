package org.example.dzdp_analysis.controller.admin;

import org.example.dzdp_analysis.service.admin.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @Autowired
    private StatusService statusService;

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardStats() {
        return statusService.getDashboardStats();
    }
}