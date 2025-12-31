package com.sss.sync.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfo {
  private Long userId;
  private String username;
  private String passwordHash;
  private String email;
  private String role;

  private Long version;
  private LocalDateTime updatedAt;
  private Boolean deleted;
}