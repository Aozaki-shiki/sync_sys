package com.sss.sync.service.conflict;

import com.sss.sync.infra.mapper.postgres.PostgresSyncBusinessMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service for PostgreSQL write operations in conflict resolution.
 * Ensures setSkipChangeLog() and upsert operations run in the same transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostgresWriteService {

  private final PostgresSyncBusinessMapper pgBiz;

  @Transactional(transactionManager = "postgresTxManager")
  public void upsertProduct(Map<String, Object> row) {
    try {
      pgBiz.setSkipChangeLog();
      pgBiz.upsertProduct(row);
    } finally {
      pgBiz.clearSkipChangeLog();
    }
  }

  @Transactional(transactionManager = "postgresTxManager")
  public void upsertOrder(Map<String, Object> row) {
    try {
      pgBiz.setSkipChangeLog();
      pgBiz.upsertOrder(row);
    } finally {
      pgBiz.clearSkipChangeLog();
    }
  }
}