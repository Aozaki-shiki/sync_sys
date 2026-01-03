package com.sss.sync.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatsSummary {
  private Long totalSyncedChanges;
  private Long totalConflictsCreated;
  private Long totalConflictsResolved;
  private Long totalFailures;
  private Double avgDailyChanges;
}