package com.costlens.infrastructure.rule.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("zone_map")
public class ZoneMapPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long rateVersionId;
    private String zipPrefixFrom;
    private String zipPrefixTo;
    private Integer zone;
}