package com.sss.sync.service.conflict;

import com.sss.sync.infra.mapper.sqlserver.SqlServerSyncBusinessMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service for SQL Server write operations in conflict resolution.
 * Ensures setSkipChangeLog() and upsert operations run in the same transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlServerWriteService {

  private final SqlServerSyncBusinessMapper ssBiz;

  @Transactional(transactionManager = "readTxManager")
  public void upsertProduct(Map<String, Object> row) {
    try {
      ssBiz.setSkipChangeLog();
      ssBiz.upsertProduct(row);
    } finally {
      ssBiz.clearSkipChangeLog();
    }
  }

  @Transactional(transactionManager = "readTxManager")
  public void upsertOrder(Map<String, Object> row) {
    try {
      ssBiz.setSkipChangeLog();
      ssBiz.upsertOrder(row);
    } finally {
      ssBiz.clearSkipChangeLog();
    }
  }
}
