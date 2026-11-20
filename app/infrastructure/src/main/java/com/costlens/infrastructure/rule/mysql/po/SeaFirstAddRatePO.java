package com.costlens.infrastructure.rule.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sea_first_add_rate")
public class SeaFirstAddRatePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long rateVersionId;
    private String country;
    private Long firstWeightGram;
    private Long firstPriceMinor;
    private Long continuationUnitGram;
    private Long continuationPriceMinor;
    private String currency;
}