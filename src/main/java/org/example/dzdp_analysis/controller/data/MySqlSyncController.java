// 创建MySQL同步控制器
package org.example.dzdp_analysis.controller.data;

import org.example.dzdp_analysis.service.data.DataProcessService;
import org.example.dzdp_analysis.service.data.HiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class MySqlSyncController {

    @Autowired
    private DataProcessService dataProcessService;
    @Autowired
    private HiveService hiveService;
    @PostMapping("/mysql")
    public ResponseEntity<?> syncToMySql(@RequestParam String uploadDate) {
        try {
            Map<String, Object> result = dataProcessService.syncToMySql(uploadDate);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "MySQL同步失败: " + e.getMessage()));
        }
    }

    @PostMapping("/recreate-tables")
    public ResponseEntity<?> recreateTables() {
        try {
            hiveService.forceRecreateAllTables();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Hive表重建完成"
            ));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "表重建失败: " + e.getMessage()));
        }
    }

    // 添加查看表结构的方法
    @GetMapping("/table-structures")
    public ResponseEntity<?> getTableStructures() {
        try {
            Map<String, Object> structures = hiveService.getAllTableStructures();
            return ResponseEntity.ok(structures);
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取表结构失败: " + e.getMessage()));
        }
    }

    @GetMapping("/table-structure/{tableName}")
    public ResponseEntity<?> getTableStructure(@PathVariable String tableName) {
        try {
            Map<String, Object> structure = hiveService.getActualTableStructure(tableName);
            return ResponseEntity.ok(structure);
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取表结构失败: " + e.getMessage()));
        }
    }
    //  添加调试接口
    @GetMapping("/inspect-data/{uploadDate}")
    public ResponseEntity<?> inspectData(@PathVariable String uploadDate,
                                         @RequestParam(defaultValue = "5") int limit) {
        try {
            Map<String, Object> inspection = hiveService.inspectDataFile(uploadDate, limit);
            return ResponseEntity.ok(inspection);
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "数据检查失败: " + e.getMessage()));
        }
    }
    // 添加智能验证接口
    @GetMapping("/validate-smart")
    public ResponseEntity<?> validateTableStructureSmart() {
        try {
            hiveService.validateTableStructureSmart();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "智能表结构验证通过"
            ));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "智能表结构验证失败: " + e.getMessage()));
        }
    }
}