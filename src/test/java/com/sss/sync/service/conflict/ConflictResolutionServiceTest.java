package com.sss.sync.service.conflict;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sss.sync.domain.sync.ConflictRecordRow;
import com.sss.sync.infra.mapper.mysql.MysqlSyncBusinessMapper;
import com.sss.sync.infra.mapper.mysql.MysqlSyncSupportMapper;
import com.sss.sync.infra.mapper.postgres.PostgresSyncBusinessMapper;
import com.sss.sync.infra.mapper.sqlserver.SqlServerSyncBusinessMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConflictResolutionServiceTest {

  @Mock
  private MysqlSyncSupportMapper mysqlSupport;
  @Mock
  private MysqlSyncBusinessMapper mysqlBiz;
  @Mock
  private PostgresSyncBusinessMapper pgBiz;
  @Mock
  private SqlServerSyncBusinessMapper ssBiz;
  @Mock
  private MysqlWriteService mysqlWriteService;
  @Mock
  private PostgresWriteService postgresWriteService;
  @Mock
  private SqlServerWriteService sqlServerWriteService;

  private ConflictResolutionService service;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
    service = new ConflictResolutionService(
        mysqlSupport,
        mysqlBiz,
        pgBiz,
        ssBiz,
        mysqlWriteService,
        postgresWriteService,
        sqlServerWriteService,
        objectMapper
    );
  }

  @Test
  void testResolveConflict_ProductInfo_VersionIsMaxPlusOne() {
    // Given: Conflict for product_info with id=1
    long conflictId = 100L;
    long productId = 1L;
    ConflictRecordRow conflict = new ConflictRecordRow();
    conflict.setConflictId(conflictId);
    conflict.setTableName("product_info");
    conflict.setPkValue(String.valueOf(productId));
    conflict.setStatus("PENDING");

    // Current versions in each DB
    when(mysqlBiz.getProductVersion(productId)).thenReturn(25);
    when(pgBiz.getProductVersion(productId)).thenReturn(30);
    when(ssBiz.getProductVersion(productId)).thenReturn(28);

    // Authoritative DB is POSTGRES
    String authJson = """
        {
          "product_id": 1,
          "product_name": "Test Product",
          "category_id": 10,
          "supplier_id": 5,
          "price": 99.99,
          "stock": 100,
          "description": "Test description",
          "listed_at": "2024-01-01T10:00:00",
          "version": 30,
          "updated_at": "2024-01-01T12:00:00",
          "deleted": false
        }
        """;

    when(mysqlSupport.getConflictById(conflictId)).thenReturn(conflict);
    when(pgBiz.getProductAsJson(productId)).thenReturn(authJson);

    // When: Resolve conflict choosing POSTGRES as authoritative
    service.resolveConflict(conflictId, "POSTGRES", "admin");

    // Then: Capture the upserted rows
    ArgumentCaptor<Map<String, Object>> mysqlCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Map<String, Object>> sqlserverCaptor = ArgumentCaptor.forClass(Map.class);

    verify(mysqlWriteService).upsertProduct(mysqlCaptor.capture());
    verify(sqlServerWriteService).upsertProduct(sqlserverCaptor.capture());
    verify(postgresWriteService, never()).upsertProduct(any()); // Postgres is authoritative, not updated

    // Verify version is max(25, 30, 28) + 1 = 31
    Map<String, Object> mysqlRow = mysqlCaptor.getValue();
    Map<String, Object> sqlserverRow = sqlserverCaptor.getValue();

    assertEquals(31L, mysqlRow.get("version"), "MySQL version should be 31");
    assertEquals(31L, sqlserverRow.get("version"), "SQL Server version should be 31");

    // Verify conflict is marked as resolved
    verify(mysqlSupport).resolveConflict(conflictId, "admin", "POSTGRES");
  }

  @Test
  void testResolveConflict_OrderInfo_VersionIsMaxPlusOne() {
    // Given: Conflict for order_info with id=2
    long conflictId = 101L;
    long orderId = 2L;
    ConflictRecordRow conflict = new ConflictRecordRow();
    conflict.setConflictId(conflictId);
    conflict.setTableName("order_info");
    conflict.setPkValue(String.valueOf(orderId));
    conflict.setStatus("PENDING");

    // Current versions in each DB
    when(mysqlBiz.getOrderVersion(orderId)).thenReturn(15);
    when(pgBiz.getOrderVersion(orderId)).thenReturn(12);
    when(ssBiz.getOrderVersion(orderId)).thenReturn(18);

    // Authoritative DB is SQLSERVER
    String authJson = """
        {
          "order_id": 2,
          "user_id": 100,
          "product_id": 1,
          "quantity": 5,
          "order_status": "SHIPPED",
          "ordered_at": "2024-01-01T10:00:00",
          "shipping_address": "123 Main St",
          "version": 18,
          "updated_at": "2024-01-01T12:00:00",
          "deleted": false
        }
        """;

    when(mysqlSupport.getConflictById(conflictId)).thenReturn(conflict);
    when(ssBiz.getOrderAsJson(orderId)).thenReturn(authJson);

    // When: Resolve conflict choosing SQLSERVER as authoritative
    service.resolveConflict(conflictId, "SQLSERVER", "admin");

    // Then: Capture the upserted rows
    ArgumentCaptor<Map<String, Object>> mysqlCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Map<String, Object>> pgCaptor = ArgumentCaptor.forClass(Map.class);

    verify(mysqlWriteService).upsertOrder(mysqlCaptor.capture());
    verify(postgresWriteService).upsertOrder(pgCaptor.capture());
    verify(sqlServerWriteService, never()).upsertOrder(any()); // SQLServer is authoritative, not updated

    // Verify version is max(15, 12, 18) + 1 = 19
    Map<String, Object> mysqlRow = mysqlCaptor.getValue();
    Map<String, Object> pgRow = pgCaptor.getValue();

    assertEquals(19L, mysqlRow.get("version"), "MySQL version should be 19");
    assertEquals(19L, pgRow.get("version"), "Postgres version should be 19");

    // Verify conflict is marked as resolved
    verify(mysqlSupport).resolveConflict(conflictId, "admin", "SQLSERVER");
  }

  @Test
  void testResolveConflict_NullVersionTreatedAsZero() {
    // Given: Product conflict where some DBs have null version (row doesn't exist)
    long conflictId = 102L;
    long productId = 3L;
    ConflictRecordRow conflict = new ConflictRecordRow();
    conflict.setConflictId(conflictId);
    conflict.setTableName("product_info");
    conflict.setPkValue(String.valueOf(productId));
    conflict.setStatus("PENDING");

    // MySQL has version 10, others have null (row doesn't exist)
    when(mysqlBiz.getProductVersion(productId)).thenReturn(10);
    when(pgBiz.getProductVersion(productId)).thenReturn(null);
    when(ssBiz.getProductVersion(productId)).thenReturn(null);

    // Authoritative DB is MYSQL
    String authJson = """
        {
          "product_id": 3,
          "product_name": "New Product",
          "category_id": 5,
          "supplier_id": 2,
          "price": 49.99,
          "stock": 50,
          "description": "New item",
          "listed_at": "2024-01-01T10:00:00",
          "version": 10,
          "updated_at": "2024-01-01T12:00:00",
          "deleted": false
        }
        """;

    when(mysqlSupport.getConflictById(conflictId)).thenReturn(conflict);
    when(mysqlBiz.getProductAsJson(productId)).thenReturn(authJson);

    // When: Resolve conflict choosing MYSQL as authoritative
    service.resolveConflict(conflictId, "MYSQL", "admin");

    // Then: Capture the upserted rows
    ArgumentCaptor<Map<String, Object>> pgCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Map<String, Object>> sqlserverCaptor = ArgumentCaptor.forClass(Map.class);

    verify(postgresWriteService).upsertProduct(pgCaptor.capture());
    verify(sqlServerWriteService).upsertProduct(sqlserverCaptor.capture());
    verify(mysqlWriteService, never()).upsertProduct(any()); // MySQL is authoritative, not updated

    // Verify version is max(10, 0, 0) + 1 = 11
    Map<String, Object> pgRow = pgCaptor.getValue();
    Map<String, Object> sqlserverRow = sqlserverCaptor.getValue();

    assertEquals(11L, pgRow.get("version"), "Postgres version should be 11");
    assertEquals(11L, sqlserverRow.get("version"), "SQL Server version should be 11");

    // Verify conflict is marked as resolved
    verify(mysqlSupport).resolveConflict(conflictId, "admin", "MYSQL");
  }

  @Test
  void testResolveConflict_AllVersionsZero() {
    // Given: Order conflict where all DBs have null version (new rows)
    long conflictId = 103L;
    long orderId = 4L;
    ConflictRecordRow conflict = new ConflictRecordRow();
    conflict.setConflictId(conflictId);
    conflict.setTableName("order_info");
    conflict.setPkValue(String.valueOf(orderId));
    conflict.setStatus("PENDING");

    // All DBs have null version
    when(mysqlBiz.getOrderVersion(orderId)).thenReturn(null);
    when(pgBiz.getOrderVersion(orderId)).thenReturn(null);
    when(ssBiz.getOrderVersion(orderId)).thenReturn(null);

    // Authoritative DB is POSTGRES
    String authJson = """
        {
          "order_id": 4,
          "user_id": 200,
          "product_id": 2,
          "quantity": 3,
          "order_status": "PENDING",
          "ordered_at": "2024-01-01T10:00:00",
          "shipping_address": "456 Oak St",
          "version": 1,
          "updated_at": "2024-01-01T12:00:00",
          "deleted": false
        }
        """;

    when(mysqlSupport.getConflictById(conflictId)).thenReturn(conflict);
    when(pgBiz.getOrderAsJson(orderId)).thenReturn(authJson);

    // When: Resolve conflict choosing POSTGRES as authoritative
    service.resolveConflict(conflictId, "POSTGRES", "admin");

    // Then: Capture the upserted rows
    ArgumentCaptor<Map<String, Object>> mysqlCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Map<String, Object>> sqlserverCaptor = ArgumentCaptor.forClass(Map.class);

    verify(mysqlWriteService).upsertOrder(mysqlCaptor.capture());
    verify(sqlServerWriteService).upsertOrder(sqlserverCaptor.capture());

    // Verify version is max(0, 0, 0) + 1 = 1
    Map<String, Object> mysqlRow = mysqlCaptor.getValue();
    Map<String, Object> sqlserverRow = sqlserverCaptor.getValue();

    assertEquals(1L, mysqlRow.get("version"), "MySQL version should be 1");
    assertEquals(1L, sqlserverRow.get("version"), "SQL Server version should be 1");

    // Verify conflict is marked as resolved
    verify(mysqlSupport).resolveConflict(conflictId, "admin", "POSTGRES");
  }
}
