package com.sss.sync.service.conflict;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sss.sync.domain.sync.ConflictRecordRow;
import com.sss.sync.infra.mapper.mysql.MysqlSyncBusinessMapper;
import com.sss.sync.infra.mapper.mysql.MysqlSyncSupportMapper;
import com.sss.sync.infra.mapper.postgres.PostgresSyncBusinessMapper;
import com.sss.sync.infra.mapper.sqlserver.SqlServerSyncBusinessMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConflictResolutionService {

  private final MysqlSyncSupportMapper mysqlSupport;
  private final MysqlSyncBusinessMapper mysqlBiz;
  private final PostgresSyncBusinessMapper pgBiz;
  private final SqlServerSyncBusinessMapper ssBiz;
  private final MysqlWriteService mysqlWriteService;
  private final PostgresWriteService postgresWriteService;
  private final SqlServerWriteService sqlServerWriteService;
  private final ObjectMapper om;

  // ISO date portion length "yyyy-MM-dd" (10 chars) - used in hasTimezoneOffset() to distinguish
  // date hyphens from timezone offset hyphens
  private static final int ISO_DATE_PORTION_LENGTH = 10;

  private static final java.time.format.DateTimeFormatter SPACE_SEPARATED_FORMATTER =
          new DateTimeFormatterBuilder()
                  .appendPattern("yyyy-MM-dd HH:mm:ss")
                  .optionalStart()
                  .appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 0, 6, true)
                  .optionalEnd()
                  .toFormatter();

  public void resolveConflict(long conflictId, String authoritativeDb, String adminUsername) {
    log.info("Resolving conflict {} with authoritative DB: {}", conflictId, authoritativeDb);

    // 1. Load conflict record
    ConflictRecordRow conflict = mysqlSupport.getConflictById(conflictId);
    if (conflict == null) {
      throw new IllegalArgumentException("Conflict not found: " + conflictId);
    }

    if ("RESOLVED".equals(conflict.getStatus())) {
      throw new IllegalStateException("Conflict already resolved: " + conflictId);
    }

    // 2. Validate authoritativeDb
    if (!Set.of("MYSQL", "POSTGRES", "SQLSERVER").contains(authoritativeDb)) {
      throw new IllegalArgumentException("Invalid authoritativeDb: " + authoritativeDb);
    }

    String tableName = conflict.getTableName();
    String pkValue = conflict.getPkValue();
    long id = Long.parseLong(pkValue);

    // 3. Fetch latest JSON from authoritative DB
    String authJson;
    if ("product_info".equals(tableName)) {
      authJson = getProductJson(authoritativeDb, id);
    } else if ("order_info".equals(tableName)) {
      authJson = getOrderJson(authoritativeDb, id);
    } else {
      throw new IllegalArgumentException("Unsupported table: " + tableName);
    }

    if (authJson == null || authJson.trim().isEmpty()) {
      throw new IllegalStateException("No data found in authoritative DB " + authoritativeDb + " for " + tableName + " id=" + id);
    }

    // 4. Parse JSON to Map
    Map<String, Object> authRow = parseJsonToRow(authJson, tableName);
    if (authRow.isEmpty()) {
      throw new IllegalStateException("Failed to parse authoritative data from " + authoritativeDb);
    }

    // 5. Strategy A: Compute max version across all DBs and set finalVersion = max + 1
    // This ensures the resolved state wins over any pending sync events that may have higher versions
    long maxVersion = computeMaxVersion(tableName, id);
    long finalVersion = maxVersion + 1;
    authRow.put("version", finalVersion);
    log.info("Setting finalVersion={} (max across DBs was {}) for {} id={}", 
             finalVersion, maxVersion, tableName, id);

    // 6. Propagate to other two databases
    List<String> targetDbs = new ArrayList<>(List.of("MYSQL", "POSTGRES", "SQLSERVER"));
    targetDbs.remove(authoritativeDb);

    for (String targetDb : targetDbs) {
      log.info("Propagating from {} to {}", authoritativeDb, targetDb);
      if ("product_info".equals(tableName)) {
        upsertProduct(targetDb, authRow);
      } else {
        upsertOrder(targetDb, authRow);
      }
    }

    // 7. Update conflict record to RESOLVED
    mysqlSupport.resolveConflict(conflictId, adminUsername, authoritativeDb);

    log.info("Conflict {} resolved successfully", conflictId);
  }

  /**
   * Computes the maximum version across all three databases for a given table and primary key.
   * If a row does not exist in a database, treats its version as 0.
   * 
   * @param tableName the table name ("product_info" or "order_info")
   * @param id the primary key value (product_id or order_id)
   * @return the maximum version across MySQL, Postgres, and SQL Server
   * @throws IllegalArgumentException if tableName is null or not supported
   */
  private long computeMaxVersion(String tableName, long id) {
    if (tableName == null) {
      throw new IllegalArgumentException("tableName cannot be null");
    }
    
    long mysqlVersion;
    long postgresVersion;
    long sqlserverVersion;

    if ("product_info".equals(tableName)) {
      mysqlVersion = getVersionOrZero(mysqlBiz.getProductVersion(id));
      postgresVersion = getVersionOrZero(pgBiz.getProductVersion(id));
      sqlserverVersion = getVersionOrZero(ssBiz.getProductVersion(id));
    } else if ("order_info".equals(tableName)) {
      mysqlVersion = getVersionOrZero(mysqlBiz.getOrderVersion(id));
      postgresVersion = getVersionOrZero(pgBiz.getOrderVersion(id));
      sqlserverVersion = getVersionOrZero(ssBiz.getOrderVersion(id));
    } else {
      throw new IllegalArgumentException("Unsupported table: " + tableName);
    }

    long maxVersion = Math.max(mysqlVersion, Math.max(postgresVersion, sqlserverVersion));
    log.debug("Version check for {} id={}: MySQL={}, Postgres={}, SQLServer={}, max={}", 
              tableName, id, mysqlVersion, postgresVersion, sqlserverVersion, maxVersion);
    
    return maxVersion;
  }

  /**
   * Converts a version Integer to long, treating null as 0.
   * 
   * @param version the version Integer (may be null if row doesn't exist)
   * @return the version as long, or 0 if version is null
   */
  private long getVersionOrZero(Integer version) {
    return (version != null) ? version : 0;
  }

  private String getProductJson(String db, long id) {
    return switch (db) {
      case "MYSQL" -> mysqlBiz.getProductAsJson(id);
      case "POSTGRES" -> pgBiz.getProductAsJson(id);
      case "SQLSERVER" -> ssBiz.getProductAsJson(id);
      default -> null;
    };
  }

  private String getOrderJson(String db, long id) {
    return switch (db) {
      case "MYSQL" -> mysqlBiz.getOrderAsJson(id);
      case "POSTGRES" -> pgBiz.getOrderAsJson(id);
      case "SQLSERVER" -> ssBiz.getOrderAsJson(id);
      default -> null;
    };
  }

  private void upsertProduct(String targetDb, Map<String, Object> row) {
    switch (targetDb) {
      case "MYSQL" -> mysqlWriteService.upsertProduct(row);
      case "POSTGRES" -> postgresWriteService.upsertProduct(row);
      case "SQLSERVER" -> sqlServerWriteService.upsertProduct(row);
    }
  }

  private void upsertOrder(String targetDb, Map<String, Object> row) {
    switch (targetDb) {
      case "MYSQL" -> mysqlWriteService.upsertOrder(row);
      case "POSTGRES" -> postgresWriteService.upsertOrder(row);
      case "SQLSERVER" -> sqlServerWriteService.upsertOrder(row);
    }
  }

  private Map<String, Object> parseJsonToRow(String json, String table) {
    if (json == null || json.isBlank()) return Collections.emptyMap();
    try {
      JsonNode n = om.readTree(json);
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

      normalizeBooleans(m);
      return m;
    } catch (Exception e) {
      log.error("Failed to parse JSON to row", e);
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

      String textValue = v.asText();
      LocalDateTime ldt = asLdt(textValue);
      if (ldt != null) {
        m.put(key, ldt);
        return;
      }
      log.debug("Failed to convert timestamp field '{}' with value '{}' to LocalDateTime, trying next candidate", c, textValue);
    }
  }

  private boolean hasTimezoneOffset(String s) {
    if (!s.contains("T")) return false;
    if (s.contains("+")) return true;
    return s.contains("-") && s.lastIndexOf('-') > ISO_DATE_PORTION_LENGTH;
  }

  private LocalDateTime asLdt(Object o) {
    if (o == null) return null;
    if (o instanceof LocalDateTime ldt) return ldt;

    String s = String.valueOf(o).trim();
    if (s.isEmpty()) return null;

    try {
      if (s.endsWith("Z")) {
        return java.time.ZonedDateTime.parse(s).toLocalDateTime();
      }
      if (hasTimezoneOffset(s)) {
        return java.time.OffsetDateTime.parse(s).toLocalDateTime();
      }
      if (s.contains("T")) {
        return LocalDateTime.parse(s);
      }
      if (s.contains(" ")) {
        return LocalDateTime.parse(s, SPACE_SEPARATED_FORMATTER);
      }
      return LocalDateTime.parse(s);
    } catch (java.time.format.DateTimeParseException e) {
      log.debug("Failed to parse timestamp string '{}': {}", s, e.getMessage());
      return null;
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
      default -> null;
    };
  }
}