package com.sss.sync.service.sync;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sss.sync")
public class SyncProperties {
  private boolean enabled = false; // 默认关，避免配置读取失败时自动跑同步
  private long pollIntervalMillis = 1000;
  private int batchSize = 200;

  private Scheduled scheduled = new Scheduled();

  @Data
  public static class Scheduled {
    private boolean enabled = false;
    private long fixedDelayMillis = 10000;
    private String cron = "0 0 2 * * *"; // Default: every day at 02:00 Asia/Shanghai
  }
}