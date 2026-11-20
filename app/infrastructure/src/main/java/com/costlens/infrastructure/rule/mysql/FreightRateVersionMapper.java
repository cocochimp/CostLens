package com.costlens.infrastructure.rule.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.costlens.infrastructure.rule.mysql.po.FreightRateVersionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FreightRateVersionMapper extends BaseMapper<FreightRateVersionPO> {
}