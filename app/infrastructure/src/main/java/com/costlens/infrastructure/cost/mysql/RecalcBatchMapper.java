package com.costlens.infrastructure.cost.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.costlens.infrastructure.cost.mysql.po.RecalcBatchPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecalcBatchMapper extends BaseMapper<RecalcBatchPO> {
}