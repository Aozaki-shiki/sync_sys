package com.sss.sync.config;

import com.sss.sync.infra.id.SnowflakeIdGenerator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sss.id")
public class IdConfig {

  private long workerId = 1L;

  public long getWorkerId() {
    return workerId;
  }

  public void setWorkerId(long workerId) {
    this.workerId = workerId;
  }

  @Bean
  public SnowflakeIdGenerator snowflakeIdGenerator() {
    return new SnowflakeIdGenerator(workerId);
  }
}
