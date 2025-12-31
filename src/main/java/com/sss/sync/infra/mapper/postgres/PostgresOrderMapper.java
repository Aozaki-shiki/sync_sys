package com.sss.sync.infra.mapper.postgres;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostgresOrderMapper extends BaseMapper<OrderInfo> {
}