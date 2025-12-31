package com.sss.sync.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sss.jwt")
public class JwtProperties {
  private String issuer;
  private String secret;
  private long accessTokenExpireMinutes;
}