package com.sss.sync.infra.mapper.mysql;

import com.sss.sync.domain.sync.ChangeLogRow;
import com.sss.sync.domain.sync.ConflictRecordRow;
import com.sss.sync.domain.sync.SyncCheckpointRow;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MysqlSyncSupportMapper {

  @Select("""
    SELECT checkpoint_id, source_db, last_change_id, updated_at
    FROM sync_checkpoint
    WHERE source_db = #{sourceDb}
    LIMIT 1
  """)
  SyncCheckpointRow getCheckpoint(@Param("sourceDb") String sourceDb);

  @Update("""
    UPDATE sync_checkpoint
    SET last_change_id = #{lastChangeId}
    WHERE source_db = #{sourceDb}
  """)
  int updateCheckpoint(@Param("sourceDb") String sourceDb, @Param("lastChangeId") long lastChangeId);

  @Insert("""
    INSERT INTO conflict_record(
      table_name, pk_value,
      source_db, target_db,
      source_version, target_version,
      source_updated_at, target_updated_at,
      source_payload_json, target_payload_json,
      status, created_at
    ) VALUES (
      #{tableName}, #{pkValue},
      #{sourceDb}, #{targetDb},
      #{sourceVersion}, #{targetVersion},
      #{sourceUpdatedAt}, #{targetUpdatedAt},
      #{sourcePayloadJson}, #{targetPayloadJson},
      #{status}, NOW(3)
    )
    ON DUPLICATE KEY UPDATE
      source_db = VALUES(source_db),
      target_db = VALUES(target_db),
      source_version = VALUES(source_version),
      target_version = VALUES(target_version),
      source_updated_at = VALUES(source_updated_at),
      target_updated_at = VALUES(target_updated_at),
      source_payload_json = VALUES(source_payload_json),
      target_payload_json = VALUES(target_payload_json)
  """)
  @Options(useGeneratedKeys = true, keyProperty = "conflictId", keyColumn = "conflict_id")
  int insertConflict(ConflictRecordRow row);

  @Select("""
    SELECT change_id, db_code, table_name, op_type, pk_value,
           row_version, row_updated_at,
           CAST(payload_json AS CHAR) AS payload_json,
           created_at
    FROM change_log
    WHERE change_id > #{lastChangeId}
    ORDER BY change_id ASC
    LIMIT #{limit}
  """)
  List<ChangeLogRow> fetchMysqlChangeAfter(@Param("lastChangeId") long lastChangeId, @Param("limit") int limit);

  @Select("""
    SELECT conflict_id
    FROM conflict_record
    WHERE table_name = #{tableName}
      AND pk_value = #{pkValue}
      AND status = 'OPEN'
    ORDER BY conflict_id DESC
    LIMIT 1
  """)
  Long findOpenConflictId(@Param("tableName") String tableName, @Param("pkValue") String pkValue);

  @Insert("""
    INSERT INTO conflict_record(
      table_name, pk_value,
      source_db, target_db,
      source_version, target_version,
      source_updated_at, target_updated_at,
      source_payload_json, target_payload_json,
      status, created_at
    ) VALUES (
      #{tableName}, #{pkValue},
      #{sourceDb}, #{targetDb},
      #{sourceVersion}, #{targetVersion},
      #{sourceUpdatedAt}, #{targetUpdatedAt},
      #{sourcePayloadJson}, #{targetPayloadJson},
      #{status}, NOW(3)
    )
  """)
  @Options(useGeneratedKeys = true, keyProperty = "conflictId", keyColumn = "conflict_id")
  int insertConflictPure(ConflictRecordRow row);

  @Select("""
    SELECT conflict_id, table_name, pk_value,
           source_db, target_db,
           source_version, target_version,
           source_updated_at, target_updated_at,
           source_payload_json, target_payload_json,
           status, resolved_by, resolved_at, resolution,
           created_at
    FROM conflict_record
    WHERE conflict_id = #{conflictId}
  """)
  ConflictRecordRow getConflictById(@Param("conflictId") long conflictId);
}