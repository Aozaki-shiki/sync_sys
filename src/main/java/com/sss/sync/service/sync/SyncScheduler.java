package com.sss.sync.service.sync;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
      while (!Thread.currentThread().isInterrupted()) {
        try {
          engine.syncOnce();
          Thread.sleep(props.getPollIntervalMillis());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (Exception e) {
          e.printStackTrace();
          try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        }
      }
    }, "sync-realtime-loop");

    t.setDaemon(true);
    t.start();
  }
}