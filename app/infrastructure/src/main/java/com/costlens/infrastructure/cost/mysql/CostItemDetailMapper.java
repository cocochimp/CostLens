package com.costlens.infrastructure.cost.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.costlens.infrastructure.cost.mysql.po.CostItemDetailPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CostItemDetailMapper extends BaseMapper<CostItemDetailPO> {
}