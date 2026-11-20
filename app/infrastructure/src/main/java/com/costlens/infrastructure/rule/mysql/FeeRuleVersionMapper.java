package com.costlens.infrastructure.rule.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.costlens.infrastructure.rule.mysql.po.FeeRuleVersionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeeRuleVersionMapper extends BaseMapper<FeeRuleVersionPO> {
}