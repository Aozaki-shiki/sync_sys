package com.sss.sync.infra.mapper.postgres;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostgresUserMapper extends BaseMapper<UserInfo> {

  @Select("""
    SELECT user_id, username, password_hash, email, role, version, updated_at, deleted
    FROM user_info
    WHERE username = #{username} AND deleted = false
    LIMIT 1
  """)
  UserInfo findByUsername(@Param("username") String username);
}