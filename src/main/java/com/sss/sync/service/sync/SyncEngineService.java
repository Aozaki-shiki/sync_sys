package com.sss.sync.service.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sss.sync.domain.entity.ChangeLog;
import com.sss.sync.domain.entity.ConflictRecord;
import com.sss.sync.domain.entity.ProductInfo;
import com.sss.sync.infra.mapper.mysql.MysqlChangeLogMapper;
import com.sss.sync.infra.mapper.mysql.MysqlConflictRecordMapper;
import com.sss.sync.infra.mapper.mysql.MysqlProductMapper;
import com.sss.sync.infra.mapper.postgres.PostgresChangeLogMapper;
import com.sss.sync.infra.mapper.postgres.PostgresProductMapper;
import com.sss.sync.service.conflict.ConflictLinkTokenService;
import com.sss.sync.service.mail.MailProperties;
import com.sss.sync.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncEngineService {

  private final SyncProperties syncProperties;
  private final MailService mailService;
  private final MailProperties mailProperties;
  private final ConflictLinkTokenService conflictLinkTokenService;

  private final MysqlChangeLogMapper mysqlChangeLogMapper;
  private final MysqlProductMapper mysqlProductMapper;
  private final MysqlConflictRecordMapper mysqlConflictRecordMapper;

  private final PostgresChangeLogMapper postgresChangeLogMapper;
  private final PostgresProductMapper postgresProductMapper;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public void syncFromMysql() {
    if (!syncProperties.isEnabled()) {
      return;
    }

    List<ChangeLog> changeLogs = mysqlChangeLogMapper.selectUnsynced(syncProperties.getBatchSize());
    log.info("MySQL -> Postgres sync: found {} unsynced change_log", changeLogs.size());

    for (ChangeLog changeLog : changeLogs) {
      try {
        syncToPostgres(changeLog);
        mysqlChangeLogMapper.markSynced(changeLog.getLogId());
      } catch (Exception e) {
        log.error("Failed to sync log_id={} from MySQL to Postgres", changeLog.getLogId(), e);
      }
    }
  }

  public void syncFromPostgres() {
    if (!syncProperties.isEnabled()) {
      return;
    }

    List<ChangeLog> changeLogs = postgresChangeLogMapper.selectUnsynced(syncProperties.getBatchSize());
    log.info("Postgres -> MySQL sync: found {} unsynced change_log", changeLogs.size());

    for (ChangeLog changeLog : changeLogs) {
      try {
        syncToMysql(changeLog);
        postgresChangeLogMapper.markSynced(changeLog.getLogId());
      } catch (Exception e) {
        log.error("Failed to sync log_id={} from Postgres to MySQL", changeLog.getLogId(), e);
      }
    }
  }

  @Transactional(transactionManager = "postgresTxManager", rollbackFor = Exception.class)
  protected void syncToPostgres(ChangeLog changeLog) throws Exception {
    if (!"product_info".equals(changeLog.getTableName())) {
      log.warn("Skipping unsupported table: {}", changeLog.getTableName());
      return;
    }

    Map<String, Object> payload = objectMapper.readValue(changeLog.getPayloadJson(), new TypeReference<>() {});
    Long productId = Long.parseLong(String.valueOf(payload.get("product_id")));

    // Check for conflicts
    ProductInfo existing = postgresProductMapper.findById(productId);
    if (existing != null) {
      Long sourceVersion = Long.parseLong(String.valueOf(payload.get("version")));
      if (existing.getVersion() > sourceVersion) {
        // Conflict detected
        handleConflict("product_info", String.valueOf(productId), "MYSQL", "POSTGRES", changeLog.getPayloadJson(), objectMapper.writeValueAsString(existing));
        return;
      }
    }

    // Apply the change
    if ("UPDATE".equals(changeLog.getOperation()) || "INSERT".equals(changeLog.getOperation())) {
      ProductInfo product = objectMapper.convertValue(payload, ProductInfo.class);
      if (existing != null) {
        postgresProductMapper.updateProduct(product);
      } else {
        postgresProductMapper.insertProduct(product);
      }
    } else if ("DELETE".equals(changeLog.getOperation())) {
      postgresProductMapper.deleteById(productId);
    }
  }

  @Transactional(transactionManager = "mysqlTxManager", rollbackFor = Exception.class)
  protected void syncToMysql(ChangeLog changeLog) throws Exception {
    if (!"product_info".equals(changeLog.getTableName())) {
      log.warn("Skipping unsupported table: {}", changeLog.getTableName());
      return;
    }

    Map<String, Object> payload = objectMapper.readValue(changeLog.getPayloadJson(), new TypeReference<>() {});
    Long productId = Long.parseLong(String.valueOf(payload.get("product_id")));

    // Check for conflicts
    ProductInfo existing = mysqlProductMapper.findById(productId);
    if (existing != null) {
      Long sourceVersion = Long.parseLong(String.valueOf(payload.get("version")));
      if (existing.getVersion() > sourceVersion) {
        // Conflict detected
        handleConflict("product_info", String.valueOf(productId), "POSTGRES", "MYSQL", changeLog.getPayloadJson(), objectMapper.writeValueAsString(existing));
        return;
      }
    }

    // Apply the change
    if ("UPDATE".equals(changeLog.getOperation()) || "INSERT".equals(changeLog.getOperation())) {
      ProductInfo product = objectMapper.convertValue(payload, ProductInfo.class);
      if (existing != null) {
        mysqlProductMapper.updateProduct(product);
      } else {
        mysqlProductMapper.insertProduct(product);
      }
    } else if ("DELETE".equals(changeLog.getOperation())) {
      mysqlProductMapper.deleteById(productId);
    }
  }

  @Transactional(transactionManager = "mysqlTxManager", rollbackFor = Exception.class)
  protected void handleConflict(String tableName, String pkValue, String sourceDb, String targetDb, 
                                String sourcePayload, String targetPayload) {
    log.warn("Conflict detected: table={}, pk={}, source={}, target={}", tableName, pkValue, sourceDb, targetDb);

    ConflictRecord record = new ConflictRecord();
    record.setTableName(tableName);
    record.setPkValue(pkValue);
    record.setSourceDb(sourceDb);
    record.setTargetDb(targetDb);
    record.setSourcePayloadJson(sourcePayload);
    record.setTargetPayloadJson(targetPayload);
    record.setStatus("PENDING");
    record.setDetectedAt(LocalDateTime.now());

    mysqlConflictRecordMapper.insert(record);

    // Send email notification
    String token = conflictLinkTokenService.generate(record.getConflictId(), "admin");
    String link = mailProperties.getConflictViewBaseUrl() + "/conflicts/view?token=" + token;
    String subject = String.format("同步冲突 - %s (ID: %s)", tableName, pkValue);
    String body = String.format("""
      检测到同步冲突：
      
      表：%s
      主键：%s
      源库：%s
      目标库：%s
      
      点击查看详情：
      %s
      
      此链接24小时内有效。
      """, tableName, pkValue, sourceDb, targetDb, link);

    try {
      mailService.sendText(mailProperties.getAdminTo(), subject, body);
      log.info("Conflict notification email sent for conflict_id={}", record.getConflictId());
    } catch (Exception e) {
      log.error("Failed to send conflict notification email", e);
    }
  }
}
