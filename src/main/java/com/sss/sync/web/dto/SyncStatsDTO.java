package com.sss.sync.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatsDTO {
  private LocalDate statDate;
  private Long syncedChanges;
  private Long conflictsCreated;
  private Long conflictsResolved;
  private Long failures;
  private Double avgProcessingTime; // in milliseconds
}
