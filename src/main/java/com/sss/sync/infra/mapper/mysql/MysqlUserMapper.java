package com.sss.sync.infra.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MysqlUserMapper extends BaseMapper<UserInfo> {

  @Select("""
    SELECT user_id, username, password_hash, email, role, version, updated_at, deleted
    FROM user_info
    WHERE username = #{username} AND deleted = 0
    LIMIT 1
  """)
  UserInfo findByUsername(@Param("username") String username);
}