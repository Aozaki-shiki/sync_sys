package com.sss.sync.domain.sync;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConflictRecordRow {
  private Long conflictId;
  private String tableName;
  private String pkValue;

  private String sourceDb;
  private String targetDb;

  private Long sourceVersion;
  private Long targetVersion;
  private LocalDateTime sourceUpdatedAt;
  private LocalDateTime targetUpdatedAt;

  private String sourcePayloadJson;
  private String targetPayloadJson;

  private String status;
  private String resolvedBy;
  private LocalDateTime resolvedAt;
  private String resolution;

  private LocalDateTime createdAt;
}