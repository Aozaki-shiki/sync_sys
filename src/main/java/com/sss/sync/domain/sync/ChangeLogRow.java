package com.sss.sync.domain.sync;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChangeLogRow {
  private Long changeId;
  private String dbCode;
  private String tableName;
  private String opType;
  private String pkValue;
  private Long rowVersion;
  private LocalDateTime rowUpdatedAt;
  private String payloadJson;
  private LocalDateTime createdAt;
}