package com.sss.sync.infra.mapper.postgres;

import com.sss.sync.domain.sync.ChangeLogRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostgresSyncSupportMapper {

  @Select("""
    SELECT change_id, db_code, table_name, op_type, pk_value,
           row_version, row_updated_at,
           payload_json::text AS payload_json,
           created_at
    FROM change_log
    WHERE change_id > #{lastChangeId}
    ORDER BY change_id ASC
    LIMIT #{limit}
  """)
  List<ChangeLogRow> fetchPostgresChangeAfter(@Param("lastChangeId") long lastChangeId, @Param("limit") int limit);
}