package org.example.dzdp_analysis.controller.admin;

import org.example.dzdp_analysis.repository.dao.admin.UserRepository;
import org.example.dzdp_analysis.repository.dao.data.DataQualityReportRepository;
import org.example.dzdp_analysis.repository.entity.admin.User;
import org.example.dzdp_analysis.repository.entity.data.DataQualityReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/list")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "获取用户列表失败",
                    "message", e.getMessage()
            ));
        }
    }
}