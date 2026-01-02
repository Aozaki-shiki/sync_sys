package com.sss.sync.service.conflict;

import com.sss.sync.infra.mapper.mysql.MysqlSyncBusinessMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service for MySQL write operations in conflict resolution.
 * Ensures setSkipChangeLog() and upsert operations run in the same transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MysqlWriteService {

  private final MysqlSyncBusinessMapper mysqlBiz;

  @Transactional(transactionManager = "mysqlTxManager")
  public void upsertProduct(Map<String, Object> row) {
    try {
      mysqlBiz.setSkipChangeLog();
      mysqlBiz.upsertProduct(row);
    } finally {
      mysqlBiz.clearSkipChangeLog();
    }
  }

  @Transactional(transactionManager = "mysqlTxManager")
  public void upsertOrder(Map<String, Object> row) {
    try {
      mysqlBiz.setSkipChangeLog();
      mysqlBiz.upsertOrder(row);
    } finally {
      mysqlBiz.clearSkipChangeLog();
    }
  }
}
