package org.example.dzdp_analysis.service.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ParallelEtlService {
    private static final Logger logger = LoggerFactory.getLogger(ParallelEtlService.class);

    @Autowired
    private HiveService hiveService;

    @Autowired
    private MySqlSyncService mySqlSyncService;

    @Async("etlTaskExecutor")
    public CompletableFuture<Void> executeEtlToDwdAsync(String uploadDate) {
        try {
            hiveService.executeEtlToDwd(uploadDate);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("并行执行ETL到DWD失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("etlTaskExecutor")
    public CompletableFuture<Void> executeDwsAggregationAsync(String uploadDate) {
        try {
            hiveService.executeDwsAggregation(uploadDate);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("并行执行DWS聚合失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("etlTaskExecutor")
    public CompletableFuture<Void> generateMerchantDashboardDataAsync(String uploadDate) {
        try {
            hiveService.generateMerchantDashboardData(uploadDate);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("并行生成商户仪表板数据失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("etlTaskExecutor")
    public CompletableFuture<Void> generateRankingDailyDataAsync(String uploadDate) {
        try {
            hiveService.generateRankingDailyData(uploadDate);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("并行生成每日排行榜数据失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("etlTaskExecutor")
    public CompletableFuture<Void> syncAllDataToMySqlAsync(String uploadDate) {
        try {
            mySqlSyncService.syncAllDataToMySql(uploadDate);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("并行同步数据到MySQL失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional
    public void executeParallelEtl(String uploadDate) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> etlToDwd = executeEtlToDwdAsync(uploadDate);
        CompletableFuture<Void> dwsAggregation = executeDwsAggregationAsync(uploadDate);
        CompletableFuture<Void> merchantDashboard = generateMerchantDashboardDataAsync(uploadDate);
        CompletableFuture<Void> rankingDaily = generateRankingDailyDataAsync(uploadDate);
        CompletableFuture<Void> syncToMySql = syncAllDataToMySqlAsync(uploadDate);

        // 等待所有任务完成
        CompletableFuture.allOf(etlToDwd, dwsAggregation, merchantDashboard, rankingDaily, syncToMySql).get();
        try {
            // 等待所有任务完成，设置超时时间
            CompletableFuture.allOf(etlToDwd, dwsAggregation, merchantDashboard, rankingDaily, syncToMySql)
                    .get(2, TimeUnit.HOURS); // 2小时超时

            logger.info("并行ETL任务全部完成: {}", uploadDate);
        } catch (TimeoutException e) {
            logger.error("并行ETL任务超时: {}", uploadDate);
            // 取消所有未完成的任务
            etlToDwd.cancel(true);
            dwsAggregation.cancel(true);
            merchantDashboard.cancel(true);
            rankingDaily.cancel(true);
            syncToMySql.cancel(true);

            throw new RuntimeException("并行ETL任务超时: " + uploadDate, e);
        } catch (Exception e) {
            logger.error("并行ETL任务执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("并行ETL任务失败: " + e.getMessage(), e);
        }
    }
}
