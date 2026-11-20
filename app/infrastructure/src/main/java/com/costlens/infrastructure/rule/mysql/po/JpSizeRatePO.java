package com.costlens.infrastructure.rule.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("jp_size_rate")
public class JpSizeRatePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long rateVersionId;
    private String area;
    private String sizeBand;
    private Integer girthCmMax;
    private Long weightCapGram;
    private Long priceMinor;
    private String currency;
}