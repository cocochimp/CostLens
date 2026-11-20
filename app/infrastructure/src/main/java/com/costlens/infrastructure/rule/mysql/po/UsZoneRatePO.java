package com.costlens.infrastructure.rule.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("us_zone_rate")
public class UsZoneRatePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long rateVersionId;
    private Integer zone;
    private String serviceLevel;
    private Long weightFromGram;
    private Long weightToGram;
    private Long unitPriceMinor;
    private String currency;
}