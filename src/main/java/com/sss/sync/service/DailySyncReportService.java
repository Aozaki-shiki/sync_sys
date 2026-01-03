package com.sss.sync.service;

import com.sss.sync.infra.mapper.mysql.MysqlSyncAnalyticsMapper;
import com.sss.sync.web.dto.DailySyncStatsResponse;
import com.sss.sync.web.dto.SyncStatsDTO;
import com.sss.sync.web.dto.SyncStatsSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailySyncReportService {

  private final MysqlSyncAnalyticsMapper mysqlSyncAnalyticsMapper;

  public DailySyncStatsResponse getDailySyncStats(int days) {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(days - 1);

    log.info("Fetching daily sync stats from MySQL from {} to {}", startDate, endDate);

    // Get daily statistics from MySQL
    List<SyncStatsDTO> dailyStats = mysqlSyncAnalyticsMapper.getDailySyncStats(startDate, endDate);

    // Calculate summary
    Long totalSyncedChanges = mysqlSyncAnalyticsMapper.getTotalSyncedChanges(startDate, endDate);
    Long totalConflictsCreated = mysqlSyncAnalyticsMapper.getTotalConflictsCreated(startDate, endDate);
    Long totalConflictsResolved = mysqlSyncAnalyticsMapper.getTotalConflictsResolved(startDate, endDate);

    // Calculate average daily changes
    Double avgDailyChanges = dailyStats.isEmpty() ? 0.0 : 
        (double) totalSyncedChanges / dailyStats.size();

    // Calculate total failures from daily stats
    Long totalFailures = dailyStats.stream()
        .mapToLong(s -> s.getFailures() != null ? s.getFailures() : 0L)
        .sum();

    SyncStatsSummary summary = new SyncStatsSummary(
        totalSyncedChanges,
        totalConflictsCreated,
        totalConflictsResolved,
        totalFailures,
        avgDailyChanges
    );

    log.info("Retrieved {} days of stats from MySQL, total synced changes: {}", dailyStats.size(), totalSyncedChanges);

    return new DailySyncStatsResponse(dailyStats, summary);
  }
}