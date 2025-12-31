package com.sss.sync.infra.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MysqlOrderMapper extends BaseMapper<OrderInfo> {
}