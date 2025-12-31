package com.sss.sync.domain.sync;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyncCheckpointRow {
  private Long checkpointId;
  private String sourceDb;
  private Long lastChangeId;
  private LocalDateTime updatedAt;
}