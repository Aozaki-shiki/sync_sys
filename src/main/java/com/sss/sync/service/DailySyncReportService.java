package com.sss.sync.service;

import com.sss.sync.infra.mapper.read.ReadSyncAnalyticsMapper;
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
  
  private final ReadSyncAnalyticsMapper syncAnalyticsMapper;
  
  public DailySyncStatsResponse getDailySyncStats(int days) {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(days - 1);
    
    log.info("Fetching daily sync stats from {} to {}", startDate, endDate);
    
    // Get daily statistics
    List<SyncStatsDTO> dailyStats = syncAnalyticsMapper.getDailySyncStats(startDate, endDate);
    
    // Calculate summary
    Long totalSyncedChanges = syncAnalyticsMapper.getTotalSyncedChanges(startDate, endDate);
    Long totalConflictsCreated = syncAnalyticsMapper.getTotalConflictsCreated(startDate, endDate);
    Long totalConflictsResolved = syncAnalyticsMapper.getTotalConflictsResolved(startDate, endDate);
    
    // Calculate average daily changes
    Double avgDailyChanges = dailyStats.isEmpty() ? 0.0 : 
        (double) totalSyncedChanges / dailyStats.size();
    
    SyncStatsSummary summary = new SyncStatsSummary(
        totalSyncedChanges,
        totalConflictsCreated,
        totalConflictsResolved,
        0L, // failures - not tracked in current schema
        avgDailyChanges
    );
    
    log.info("Retrieved {} days of stats, total synced changes: {}", dailyStats.size(), totalSyncedChanges);
    
    return new DailySyncStatsResponse(dailyStats, summary);
  }
}
