package com.sss.sync.infra.mapper.postgres;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostgresUserMapper extends BaseMapper<UserInfo> {
}