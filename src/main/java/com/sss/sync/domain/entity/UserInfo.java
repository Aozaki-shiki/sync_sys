package com.sss.sync.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_info")
public class UserInfo {
  @TableId
  private Long userId;
  private String username;
  private String passwordHash;
  private String email;
  private String role;

  private Long version;
  private LocalDateTime updatedAt;
  private Boolean deleted;
}