package com.sss.sync.service.sync;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sss.sync")
public class SyncProperties {
  private boolean enabled = true;
  private long pollIntervalMillis = 1000;
  private int batchSize = 200;

  private Scheduled scheduled = new Scheduled();

  @Data
  public static class Scheduled {
    private boolean enabled = true;
    private long fixedDelayMillis = 10000;
  }
}