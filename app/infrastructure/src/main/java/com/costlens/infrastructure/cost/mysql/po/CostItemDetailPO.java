package com.costlens.infrastructure.cost.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("cost_item_detail")
public class CostItemDetailPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sheetId;
    private String costType;
    private String ruleType;
    private String ruleKey;
    private Integer ruleVersionSeq;
    private Long baseAmountMinor;
    private Long computedMinor;
    private String currency;
    private String traceJson;
}