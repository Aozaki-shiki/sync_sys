package com.sss.sync.service.sync;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SyncScheduler {

  private final SyncProperties syncProperties;
  private final SyncEngineService syncEngineService;

  private ExecutorService executor;
  private final AtomicBoolean running = new AtomicBoolean(false);

  @PostConstruct
  public void init() {
    if (!syncProperties.isEnabled()) {
      log.info("Sync engine is disabled");
      return;
    }

    log.info("Starting sync engine (poll interval: {}ms)", syncProperties.getPollIntervalMillis());
    executor = Executors.newSingleThreadExecutor();
    running.set(true);

    // Real-time polling thread
    executor.submit(() -> {
      while (running.get()) {
        try {
          syncEngineService.syncFromMysql();
          syncEngineService.syncFromPostgres();
          Thread.sleep(syncProperties.getPollIntervalMillis());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          log.error("Error in sync polling loop", e);
        }
      }
    });
  }

  @PreDestroy
  public void shutdown() {
    if (executor != null) {
      running.set(false);
      executor.shutdown();
      log.info("Sync engine stopped");
    }
  }

  @Scheduled(fixedDelayString = "${sss.datasource.sync.scheduled.fixedDelayMillis:10000}")
  public void scheduledSync() {
    if (!syncProperties.isEnabled() || !syncProperties.getScheduled().isEnabled()) {
      return;
    }

    log.debug("Running scheduled sync");
    try {
      syncEngineService.syncFromMysql();
      syncEngineService.syncFromPostgres();
    } catch (Exception e) {
      log.error("Error in scheduled sync", e);
    }
  }
}
