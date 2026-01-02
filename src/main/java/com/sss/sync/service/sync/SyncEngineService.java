package com.sss.sync.service.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sss.sync.domain.sync.ChangeLogRow;
import com.sss.sync.domain.sync.ConflictRecordRow;
import com.sss.sync.domain.sync.SyncCheckpointRow;
import com.sss.sync.infra.mapper.mysql.MysqlSyncBusinessMapper;
import com.sss.sync.infra.mapper.mysql.MysqlSyncSupportMapper;
import com.sss.sync.infra.mapper.postgres.PostgresSyncBusinessMapper;
import com.sss.sync.infra.mapper.postgres.PostgresSyncSupportMapper;
import com.sss.sync.infra.mapper.sqlserver.SqlServerSyncBusinessMapper;
import com.sss.sync.service.conflict.ConflictLinkTokenService;
import com.sss.sync.service.mail.MailProperties;
import com.sss.sync.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncEngineService {

  private final SyncProperties props;

  // change_log source + checkpoint/conflict (checkpoint/conflict 统一写 MySQL)
  private final MysqlSyncSupportMapper mysqlSupport;
  private final PostgresSyncSupportMapper pgSupport;

  // target upsert/meta
  private final MysqlSyncBusinessMapper mysqlBiz;
  private final PostgresSyncBusinessMapper pgBiz;
  private final SqlServerSyncBusinessMapper ssBiz;

  // mail + link token
  private final MailService mailService;
  private final MailProperties mailProps;
  private final ConflictLinkTokenService linkTokenService;

  private final ObjectMapper om;
  
  // Static formatter for space-separated timestamp format with optional fractional seconds
  // Configured for PostgreSQL's microsecond precision (up to 6 digits after decimal point)
  // The 0-6 range allows: no fractions, .1, .12, .123, .1234, .12345, or .123456
  private static final DateTimeFormatter SPACE_SEPARATED_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd HH:mm:ss")
      .optionalStart()
      .appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 0, 6, true)
      .optionalEnd()
      .toFormatter();
  
  // Length of ISO date portion "yyyy-MM-dd" - used to distinguish date hyphens from timezone offset hyphens
  private static final int ISO_DATE_PORTION_LENGTH = 10;

  public void syncOnce() {
    if (!props.isEnabled()) return;
    syncFromMysql();
    syncFromPostgres();
  }

  private void syncFromMysql() {
    SyncCheckpointRow cp = mysqlSupport.getCheckpoint("MYSQL");
    long last = cp == null ? 0 : Optional.ofNullable(cp.getLastChangeId()).orElse(0L);

    List<ChangeLogRow> logs = mysqlSupport.fetchMysqlChangeAfter(last, props.getBatchSize());
    if (logs.isEmpty()) return;

    for (ChangeLogRow log : logs) {
      handleChange("MYSQL", log);
      last = log.getChangeId();
    }
    mysqlSupport.updateCheckpoint("MYSQL", last);
  }

  private void syncFromPostgres() {
    SyncCheckpointRow cp = mysqlSupport.getCheckpoint("POSTGRES");
    long last = cp == null ? 0 : Optional.ofNullable(cp.getLastChangeId()).orElse(0L);

    List<ChangeLogRow> logs = pgSupport.fetchPostgresChangeAfter(last, props.getBatchSize());
    if (logs.isEmpty()) return;

    for (ChangeLogRow log : logs) {
      handleChange("POSTGRES", log);
      last = log.getChangeId();
    }
    mysqlSupport.updateCheckpoint("POSTGRES", last);
  }

  private void handleChange(String sourceDb, ChangeLogRow log) {
    String table = log.getTableName();
    if (!"product_info".equals(table) && !"order_info".equals(table)) return;
    if ("DELETE".equalsIgnoreCase(log.getOpType())) return; // 本包先不处理 delete

    Map<String, Object> srcRow = parsePayloadToRow(log.getPayloadJson(), table);
    normalizeBooleans(srcRow);
    if (srcRow.isEmpty()) return;

    if ("product_info".equals(table)) {
      if ("MYSQL".equals(sourceDb)) {
        upsertProductWithConflict(srcRow, "MYSQL", "POSTGRES");
        upsertProductWithConflict(srcRow, "MYSQL", "SQLSERVER");
      } else {
        upsertProductWithConflict(srcRow, "POSTGRES", "MYSQL");
        upsertProductWithConflict(srcRow, "POSTGRES", "SQLSERVER");
      }
    } else {
      if ("MYSQL".equals(sourceDb)) {
        upsertOrderWithConflict(srcRow, "MYSQL", "POSTGRES");
        upsertOrderWithConflict(srcRow, "MYSQL", "SQLSERVER");
      } else {
        upsertOrderWithConflict(srcRow, "POSTGRES", "MYSQL");
        upsertOrderWithConflict(srcRow, "POSTGRES", "SQLSERVER");
      }
    }
  }

  private void upsertProductWithConflict(Map<String, Object> srcRow, String sourceDb, String targetDb) {
    long id = asLong(srcRow.get("productId"));
    Long srcVer = asLongObj(srcRow.get("version"));
    LocalDateTime srcUpd = asLdt(srcRow.get("updatedAt"));

    Map<String, Object> meta = getTargetMeta_Product(targetDb, id);
    if (meta != null && !meta.isEmpty()) {
      Long tgtVer = asLongObj(meta.get("version"));
      LocalDateTime tgtUpd = asLdt(meta.get("updatedAt"));
      if (isConflict(tgtVer, tgtUpd, srcVer, srcUpd)) {
        recordAndNotify("product_info", String.valueOf(id), sourceDb, targetDb,
          srcVer, tgtVer, srcUpd, tgtUpd,
          jsonOf(srcRow), getTargetJson_Product(targetDb, id));
        return;
      }
    }
    doUpsertProduct(targetDb, srcRow);
  }

  private void upsertOrderWithConflict(Map<String, Object> srcRow, String sourceDb, String targetDb) {
    long id = asLong(srcRow.get("orderId"));
    Long srcVer = asLongObj(srcRow.get("version"));
    LocalDateTime srcUpd = asLdt(srcRow.get("updatedAt"));

    Map<String, Object> meta = getTargetMeta_Order(targetDb, id);
    if (meta != null && !meta.isEmpty()) {
      Long tgtVer = asLongObj(meta.get("version"));
      LocalDateTime tgtUpd = asLdt(meta.get("updatedAt"));
      if (isConflict(tgtVer, tgtUpd, srcVer, srcUpd)) {
        recordAndNotify("order_info", String.valueOf(id), sourceDb, targetDb,
          srcVer, tgtVer, srcUpd, tgtUpd,
          jsonOf(srcRow), getTargetJson_Order(targetDb, id));
        return;
      }
    }
    doUpsertOrder(targetDb, srcRow);
  }

  private boolean isConflict(Long tgtVer, LocalDateTime tgtUpd, Long srcVer, LocalDateTime srcUpd) {
    if (tgtVer == null || tgtUpd == null || srcVer == null || srcUpd == null) return true;
    if (tgtVer > srcVer) return true;
    return tgtVer.equals(srcVer) && tgtUpd.isAfter(srcUpd);
  }

  // 只展示 recordAndNotify 方法，其他不变
  private void recordAndNotify(String table, String pk, String sourceDb, String targetDb,
                               Long srcVer, Long tgtVer, LocalDateTime srcUpd, LocalDateTime tgtUpd,
                               String srcJson, String tgtJson) {

    // 1) 先查是否已经有 OPEN 冲突（幂等：避免重复发邮件、避免重复插入）
    Long existingId = mysqlSupport.findOpenConflictId(table, pk);
    System.out.println("[DEBUG] srcJson=" + srcJson);
    System.out.println("[DEBUG] tgtJson=" + tgtJson);
    if (existingId != null) {
      // 已有 open 冲突：不再重复发邮件（你想重复发也可以改成发）
      return;
    }

    // 2) 再 insert
    ConflictRecordRow cr = new ConflictRecordRow();
    cr.setTableName(table);
    cr.setPkValue(pk);
    cr.setSourceDb(sourceDb);
    cr.setTargetDb(targetDb);
    cr.setSourceVersion(srcVer);
    cr.setTargetVersion(tgtVer);
    cr.setSourceUpdatedAt(srcUpd);
    cr.setTargetUpdatedAt(tgtUpd);
    cr.setSourcePayloadJson(srcJson);
    cr.setTargetPayloadJson(tgtJson);
    cr.setStatus("OPEN");

    mysqlSupport.insertConflictPure(cr);

    // 3) 取到 conflictId（insert 后应当有；兜底再查一次）
    Long conflictId = cr.getConflictId();
    if (conflictId == null) {
      conflictId = mysqlSupport.findOpenConflictId(table, pk);
    }
    if (conflictId == null) {
      // 还拿不到说明数据库/约束异常，直接退出避免 NPE
      return;
    }

    // 4) 发邮件
    String token = linkTokenService.generate(conflictId, "admin");
    String url = mailProps.getConflictViewBaseUrl() + "/conflicts/view?token=" + token;

    mailService.sendText(
            mailProps.getAdminTo(),
            "[sss-sync] 数据同步冲突告警 conflictId=" + conflictId,
            "检测到数据同步冲突。\n\n"
                    + "table=" + table + ", pk=" + pk + "\n"
                    + "source=" + sourceDb + ", target=" + targetDb + "\n\n"
                    + "查看详情（PC/移动端通用）：\n" + url + "\n"
    );
  }

  private Map<String, Object> getTargetMeta_Product(String targetDb, long id) {
    return switch (targetDb) {
      case "MYSQL" -> mysqlBiz.getProductMeta(id);
      case "POSTGRES" -> pgBiz.getProductMeta(id);
      case "SQLSERVER" -> ssBiz.getProductMeta(id);
      default -> null;
    };
  }

  private Map<String, Object> getTargetMeta_Order(String targetDb, long id) {
    return switch (targetDb) {
      case "MYSQL" -> mysqlBiz.getOrderMeta(id);
      case "POSTGRES" -> pgBiz.getOrderMeta(id);
      case "SQLSERVER" -> ssBiz.getOrderMeta(id);
      default -> null;
    };
  }

  private String getTargetJson_Product(String targetDb, long id) {
    return switch (targetDb) {
      case "MYSQL" -> mysqlBiz.getProductAsJson(id);
      case "POSTGRES" -> pgBiz.getProductAsJson(id);
      case "SQLSERVER" -> ssBiz.getProductAsJson(id);
      default -> "{}";
    };
  }

  private String getTargetJson_Order(String targetDb, long id) {
    return switch (targetDb) {
      case "MYSQL" -> mysqlBiz.getOrderAsJson(id);
      case "POSTGRES" -> pgBiz.getOrderAsJson(id);
      case "SQLSERVER" -> ssBiz.getOrderAsJson(id);
      default -> "{}";
    };
  }

  private void doUpsertProduct(String targetDb, Map<String, Object> row) {
    switch (targetDb) {
      case "MYSQL" -> mysqlBiz.upsertProduct(row);
      case "POSTGRES" -> pgBiz.upsertProduct(row);
      case "SQLSERVER" -> ssBiz.upsertProduct(row);
    }
  }

  private void doUpsertOrder(String targetDb, Map<String, Object> row) {
    switch (targetDb) {
      case "MYSQL" -> mysqlBiz.upsertOrder(row);
      case "POSTGRES" -> pgBiz.upsertOrder(row);
      case "SQLSERVER" -> ssBiz.upsertOrder(row);
    }
  }

  private Map<String, Object> parsePayloadToRow(String payloadJson, String table) {
    if (payloadJson == null || payloadJson.isBlank()) return Collections.emptyMap();
    try {
      JsonNode n = om.readTree(payloadJson);
      Map<String, Object> m = new HashMap<>();

      if ("product_info".equals(table)) {
        put(m, "productId", n, "product_id", "productId");
        put(m, "productName", n, "product_name", "productName");
        put(m, "categoryId", n, "category_id", "categoryId");
        put(m, "supplierId", n, "supplier_id", "supplierId");
        put(m, "price", n, "price");
        put(m, "stock", n, "stock");
        put(m, "description", n, "description");
        putTimestamp(m, "listedAt", n, "listed_at", "listedAt");
        put(m, "version", n, "version");
        putTimestamp(m, "updatedAt", n, "updated_at", "updatedAt");
        put(m, "deleted", n, "deleted");
      } else {
        put(m, "orderId", n, "order_id", "orderId");
        put(m, "userId", n, "user_id", "userId");
        put(m, "productId", n, "product_id", "productId");
        put(m, "quantity", n, "quantity");
        put(m, "orderStatus", n, "order_status", "orderStatus");
        putTimestamp(m, "orderedAt", n, "ordered_at", "orderedAt");
        put(m, "shippingAddress", n, "shipping_address", "shippingAddress");
        put(m, "version", n, "version");
        putTimestamp(m, "updatedAt", n, "updated_at", "updatedAt");
        put(m, "deleted", n, "deleted");
      }
      return m;
    } catch (Exception e) {
      return Collections.emptyMap();
    }
  }

  private void put(Map<String, Object> m, String key, JsonNode n, String... candidates) {
    for (String c : candidates) {
      JsonNode v = n.get(c);
      if (v == null || v.isNull()) continue;
      if (v.isNumber()) m.put(key, v.numberValue());
      else if (v.isBoolean()) m.put(key, v.booleanValue());
      else m.put(key, v.asText());
      return;
    }
  }

  private void putTimestamp(Map<String, Object> m, String key, JsonNode n, String... candidates) {
    for (String c : candidates) {
      JsonNode v = n.get(c);
      if (v == null || v.isNull()) continue;
      
      // Convert the value to LocalDateTime
      String textValue = v.asText();
      LocalDateTime ldt = asLdt(textValue);
      if (ldt != null) {
        m.put(key, ldt);
        return;
      }
      // If conversion failed, try next candidate
      log.debug("Failed to convert timestamp field '{}' with value '{}' to LocalDateTime, trying next candidate", c, textValue);
    }
    // Log if all candidates failed
    if (log.isDebugEnabled()) {
      log.debug("All candidates {} failed to convert for timestamp field '{}'", Arrays.toString(candidates), key);
    }
  }

  private long asLong(Object o) {
    if (o == null) return 0;
    if (o instanceof Number num) return num.longValue();
    return Long.parseLong(String.valueOf(o));
  }

  private Long asLongObj(Object o) {
    if (o == null) return null;
    if (o instanceof Number num) return num.longValue();
    return Long.parseLong(String.valueOf(o));
  }

  /**
   * Checks if a timestamp string contains a timezone offset indicator.
   * 
   * @param s The timestamp string to check
   * @return true if the string has a timezone offset (+ or - after the date portion)
   */
  private boolean hasTimezoneOffset(String s) {
    if (!s.contains("T")) return false;
    if (s.contains("+")) return true;
    // Only treat '-' as timezone offset if it appears after the date portion
    return s.contains("-") && s.lastIndexOf('-') > ISO_DATE_PORTION_LENGTH;
  }

  private LocalDateTime asLdt(Object o) {
    if (o == null) return null;
    if (o instanceof LocalDateTime ldt) return ldt;
    
    String s = String.valueOf(o).trim();
    if (s.isEmpty()) return null;
    
    try {
      // Try parsing ISO format with offset/timezone first (e.g., "2025-01-15T10:30:00+08:00" or "2025-01-15T10:30:00.123Z")
      if (s.endsWith("Z")) {
        return java.time.ZonedDateTime.parse(s).toLocalDateTime();
      }
      if (hasTimezoneOffset(s)) {
        return java.time.OffsetDateTime.parse(s).toLocalDateTime();
      }
      
      // Try parsing as ISO LocalDateTime (e.g., "2025-01-15T10:30:00" or "2025-01-15T10:30:00.123")
      if (s.contains("T")) {
        return LocalDateTime.parse(s);
      }
      
      // Try parsing space-separated format (e.g., "2025-01-15 10:30:00" or "2025-01-15 10:30:00.123")
      if (s.contains(" ")) {
        return LocalDateTime.parse(s, SPACE_SEPARATED_FORMATTER);
      }
      
      // Fallback: try parsing directly
      return LocalDateTime.parse(s);
    } catch (java.time.format.DateTimeParseException e) {
      // If all parsing fails, return null to avoid breaking the sync
      log.debug("Failed to parse timestamp string '{}': {}", s, e.getMessage());
      return null;
    }
  }

  private String jsonOf(Object obj) {
    try {
      return om.writeValueAsString(obj);
    } catch (Exception e) {
      // 兜底：把所有值转成字符串，保证可落库且可读
      if (obj instanceof Map<?, ?> m) {
        Map<String, Object> safe = new LinkedHashMap<>();
        for (var entry : m.entrySet()) {
          safe.put(String.valueOf(entry.getKey()), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
        }
        try {
          return om.writeValueAsString(safe);
        } catch (Exception ignored) {
          // 最终兜底：至少返回合法 JSON 字符串
          return "{\"_error\":\"json serialization failed\"}";
        }
      }
      return "{\"_error\":\"json serialization failed\"}";
    }
  }
  private void normalizeBooleans(Map<String, Object> row) {
    row.put("deleted", toBoolean(row.get("deleted")));
  }

  private Boolean toBoolean(Object v) {
    if (v == null) return null;
    if (v instanceof Boolean b) return b;
    if (v instanceof Number n) return n.intValue() != 0;

    String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
    if (s.isEmpty()) return null;
    return switch (s) {
      case "1", "true", "t", "yes", "y", "on" -> true;
      case "0", "false", "f", "no", "n", "off" -> false;
      default -> null; // 保守：解析不了就交给数据库报错暴露
    };
  }
}