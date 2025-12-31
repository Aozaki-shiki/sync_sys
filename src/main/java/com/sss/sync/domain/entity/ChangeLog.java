package com.sss.sync.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("change_log")
public class ChangeLog {
  @TableId
  private Long logId;
  private String tableName;
  private String operation;
  private String pkValue;
  private String payloadJson;
  private LocalDateTime changedAt;
  private Boolean synced;
}
