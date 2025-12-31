package com.sss.sync.infra.id;

/**
 * Snowflake ID generator
 * 64-bit ID structure:
 * - 1 bit: unused (always 0)
 * - 41 bits: timestamp (milliseconds since epoch)
 * - 10 bits: worker ID
 * - 12 bits: sequence number
 */
public class SnowflakeIdGenerator {

  // Epoch start time: 2020-01-01 00:00:00 UTC
  private static final long EPOCH = 1577836800000L;

  // Bit lengths
  private static final long WORKER_ID_BITS = 10L;
  private static final long SEQUENCE_BITS = 12L;

  // Max values
  private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
  private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

  // Bit shifts
  private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
  private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

  private final long workerId;
  private long sequence = 0L;
  private long lastTimestamp = -1L;

  public SnowflakeIdGenerator(long workerId) {
    if (workerId < 0 || workerId > MAX_WORKER_ID) {
      throw new IllegalArgumentException(
          String.format("Worker ID must be between 0 and %d", MAX_WORKER_ID));
    }
    this.workerId = workerId;
  }

  public synchronized long nextId() {
    long timestamp = System.currentTimeMillis();

    if (timestamp < lastTimestamp) {
      throw new IllegalStateException(
          String.format("Clock moved backwards. Refusing to generate id for %d milliseconds",
              lastTimestamp - timestamp));
    }

    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1) & MAX_SEQUENCE;
      if (sequence == 0) {
        // Sequence overflow, wait for next millisecond
        timestamp = waitNextMillis(lastTimestamp);
      }
    } else {
      sequence = 0L;
    }

    lastTimestamp = timestamp;

    return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
        | (workerId << WORKER_ID_SHIFT)
        | sequence;
  }

  private long waitNextMillis(long lastTimestamp) {
    long timestamp = System.currentTimeMillis();
    while (timestamp <= lastTimestamp) {
      timestamp = System.currentTimeMillis();
    }
    return timestamp;
  }
}
