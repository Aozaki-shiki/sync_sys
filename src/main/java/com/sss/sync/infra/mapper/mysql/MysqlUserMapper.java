package com.sss.sync.infra.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MysqlUserMapper extends BaseMapper<UserInfo> {
}