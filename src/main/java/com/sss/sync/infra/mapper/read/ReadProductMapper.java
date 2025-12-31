package com.sss.sync.infra.mapper.read;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sss.sync.domain.entity.ProductInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReadProductMapper extends BaseMapper<ProductInfo> {
}