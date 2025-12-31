package com.sss.sync.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conflict_record")
public class ConflictRecord {
  private Long conflictId;
  private String tableName;
  private String pkValue;
  private String sourceDb;
  private String targetDb;
  private String sourcePayloadJson;
  private String targetPayloadJson;
  private String status;
  private LocalDateTime detectedAt;
  private LocalDateTime resolvedAt;
}
