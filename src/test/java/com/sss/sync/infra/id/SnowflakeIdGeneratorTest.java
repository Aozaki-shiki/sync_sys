package com.sss.sync.infra.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

  @Test
  void testGenerateUniqueIds() {
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
    Set<Long> ids = new HashSet<>();
    
    // Generate 10000 IDs and ensure they're all unique
    for (int i = 0; i < 10000; i++) {
      long id = generator.nextId();
      assertTrue(id > 0, "ID should be positive");
      assertTrue(ids.add(id), "ID should be unique: " + id);
    }
    
    assertEquals(10000, ids.size());
  }

  @Test
  void testWorkerIdValidation() {
    // Valid worker IDs
    assertDoesNotThrow(() -> new SnowflakeIdGenerator(0));
    assertDoesNotThrow(() -> new SnowflakeIdGenerator(1));
    assertDoesNotThrow(() -> new SnowflakeIdGenerator(1023));
    
    // Invalid worker IDs
    assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1));
    assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1024));
  }

  @Test
  void testSequentialGeneration() {
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
    
    long id1 = generator.nextId();
    long id2 = generator.nextId();
    long id3 = generator.nextId();
    
    // IDs should be different
    assertNotEquals(id1, id2);
    assertNotEquals(id2, id3);
    assertNotEquals(id1, id3);
  }

  @Test
  void testMultipleWorkers() {
    SnowflakeIdGenerator generator1 = new SnowflakeIdGenerator(1);
    SnowflakeIdGenerator generator2 = new SnowflakeIdGenerator(2);
    
    long id1 = generator1.nextId();
    long id2 = generator2.nextId();
    
    // IDs from different workers should be different
    assertNotEquals(id1, id2);
  }
}
