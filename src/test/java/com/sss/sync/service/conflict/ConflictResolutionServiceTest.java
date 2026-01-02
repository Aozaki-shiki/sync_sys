package com.sss.sync.service.conflict;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to ensure ConflictResolutionService and related beans
 * can be created without NoUniqueBeanDefinitionException.
 */
@SpringBootTest
class ConflictResolutionServiceTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private ConflictResolutionService conflictResolutionService;

  @Autowired
  private MysqlWriteService mysqlWriteService;

  @Autowired
  private PostgresWriteService postgresWriteService;

  @Autowired
  private SqlServerWriteService sqlServerWriteService;

  @Test
  void contextLoads() {
    assertNotNull(applicationContext, "Application context should not be null");
  }

  @Test
  void conflictResolutionServiceBeanExists() {
    assertNotNull(conflictResolutionService, "ConflictResolutionService bean should exist");
  }

  @Test
  void writeServiceBeansExist() {
    assertNotNull(mysqlWriteService, "MysqlWriteService bean should exist");
    assertNotNull(postgresWriteService, "PostgresWriteService bean should exist");
    assertNotNull(sqlServerWriteService, "SqlServerWriteService bean should exist");
  }

  @Test
  void transactionManagersExist() {
    // Verify all three transaction managers exist in the context
    assertTrue(applicationContext.containsBean("mysqlTxManager"), 
               "mysqlTxManager bean should exist");
    assertTrue(applicationContext.containsBean("postgresTxManager"), 
               "postgresTxManager bean should exist");
    assertTrue(applicationContext.containsBean("readTxManager"), 
               "readTxManager bean should exist");
  }

  @Test
  void verifyNoTransactionManagerAmbiguity() {
    // This test passes if Spring can autowire ConflictResolutionService
    // without throwing NoUniqueBeanDefinitionException
    assertDoesNotThrow(() -> {
      ConflictResolutionService service = applicationContext.getBean(ConflictResolutionService.class);
      assertNotNull(service);
    }, "Should be able to get ConflictResolutionService bean without ambiguity");
  }
}
