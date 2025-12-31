package com.sss.sync.service.sync;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SyncScheduler {

  private final SyncProperties props;
  private final SyncEngineService engine;

  @Scheduled(fixedDelayString = "${sss.sync.scheduled.fixedDelayMillis:10000}")
  public void scheduledSync() {
    if (!props.isEnabled()) return;
    if (!props.getScheduled().isEnabled()) return;
    engine.syncOnce();
  }

  @PostConstruct
  public void startRealtimeLoop() {
    if (!props.isEnabled()) return;

    Thread t = new Thread(() -> {
      log.info("Starting realtime sync loop with poll interval {}ms", props.getPollIntervalMillis());
      while (!Thread.currentThread().isInterrupted()) {
        try {
          engine.syncOnce();
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