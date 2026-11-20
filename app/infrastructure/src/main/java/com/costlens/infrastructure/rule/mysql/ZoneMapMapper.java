package com.costlens.infrastructure.rule.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.costlens.infrastructure.rule.mysql.po.ZoneMapPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ZoneMapMapper extends BaseMapper<ZoneMapPO> {
}