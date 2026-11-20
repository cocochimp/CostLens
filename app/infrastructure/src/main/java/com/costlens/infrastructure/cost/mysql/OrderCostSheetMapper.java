package com.costlens.infrastructure.cost.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.costlens.infrastructure.cost.mysql.po.OrderCostSheetPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderCostSheetMapper extends BaseMapper<OrderCostSheetPO> {
}