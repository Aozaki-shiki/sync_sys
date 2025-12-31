package com.sss.sync.service.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sss.mail")
public class MailProperties {
  private boolean enabled;
  private String from;
  private String adminTo;
  private String conflictViewBaseUrl;
}