package com.sss.sync.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySyncStatsResponse {
  private List<SyncStatsDTO> dailyStats;
  private SyncStatsSummary summary;
}