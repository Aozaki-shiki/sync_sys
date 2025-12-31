package com.sss.sync.infra.mapper.mysql;

import com.sss.sync.domain.entity.ConflictRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MysqlConflictRecordMapper {

  @Insert("""
    INSERT INTO conflict_record 
      (table_name, pk_value, source_db, target_db, source_payload_json, target_payload_json, status, detected_at)
    VALUES 
      (#{tableName}, #{pkValue}, #{sourceDb}, #{targetDb}, #{sourcePayloadJson}, #{targetPayloadJson}, #{status}, #{detectedAt})
  """)
  @Options(useGeneratedKeys = true, keyProperty = "conflictId", keyColumn = "conflict_id")
  int insert(ConflictRecord record);

  @Select("""
    SELECT conflict_id, table_name, pk_value, source_db, target_db, 
           source_payload_json, target_payload_json, status, detected_at, resolved_at
    FROM conflict_record
    WHERE conflict_id = #{conflictId}
  """)
  ConflictRecord selectById(Long conflictId);
}
