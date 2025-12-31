package com.sss.sync.infra.mapper.mysql;

import com.sss.sync.domain.entity.ChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MysqlChangeLogMapper {

  @Select("""
    SELECT log_id, table_name, operation, pk_value, payload_json, changed_at, synced
    FROM change_log
    WHERE synced = 0
    ORDER BY log_id ASC
    LIMIT #{batchSize}
  """)
  List<ChangeLog> selectUnsynced(@Param("batchSize") int batchSize);

  @Update("UPDATE change_log SET synced = 1 WHERE log_id = #{logId}")
  int markSynced(@Param("logId") Long logId);
}
