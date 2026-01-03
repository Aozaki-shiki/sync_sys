package com.sss.sync.service.sync;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SyncScheduler {

  private final SyncProperties props;
  private final SyncEngineService engine;
  
  // Mutual exclusion lock to prevent concurrent syncOnce executions
  private final ReentrantLock syncLock = new ReentrantLock();

  @Scheduled(fixedDelayString = "${sss.sync.scheduled.fixedDelayMillis:10000}")
  public void scheduledSync() {
    if (!props.isEnabled()) return;
    if (!props.getScheduled().isEnabled()) return;
    
    if (!syncLock.tryLock()) {
      log.debug("FixedDelay trigger skipped - sync already in progress");
      return;
    }
    try {
      engine.syncOnce();
    } finally {
      syncLock.unlock();
    }
  }

  @Scheduled(cron = "${sss.sync.scheduled.cron:0 0 2 * * *}", zone = "Asia/Shanghai")
  public void cronSync() {
    if (!props.isEnabled()) return;
    
    if (!syncLock.tryLock()) {
      log.info("Cron trigger skipped - sync already in progress");
      return;
    }
    try {
      log.info("Cron-triggered sync starting");
      engine.syncOnce();
      log.info("Cron-triggered sync completed");
    } finally {
      syncLock.unlock();
    }
  }

  @PostConstruct
  public void startRealtimeLoop() {
    if (!props.isEnabled()) return;

    Thread t = new Thread(() -> {
      log.info("Starting realtime sync loop with poll interval {}ms", props.getPollIntervalMillis());
      while (!Thread.currentThread().isInterrupted()) {
        try {
          if (!syncLock.tryLock()) {
            log.debug("Realtime loop skipped - sync already in progress");
            Thread.sleep(props.getPollIntervalMillis());
            continue;
          }
          try {
            engine.syncOnce();
          } finally {
            syncLock.unlock();
          }
          Thread.sleep(props.getPollIntervalMillis());
        } catch (InterruptedException e) {
          log.info("Realtime sync loop interrupted, stopping...");
          Thread.currentThread().interrupt();
        } catch (Exception e) {
          log.error("Error in realtime sync loop, will retry after 2s", e);
          try { 
            Thread.sleep(2000); 
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }
        }
      }
      log.info("Realtime sync loop stopped");
    }, "sync-realtime-loop");

    t.setDaemon(true);
    t.start();
  }
}