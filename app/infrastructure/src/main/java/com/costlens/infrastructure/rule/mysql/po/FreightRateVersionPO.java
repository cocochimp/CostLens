package com.costlens.infrastructure.rule.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("freight_rate_version")
public class FreightRateVersionPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String rateKey;
    private Integer versionSeq;
    private String carrier;
    private String market;
    private String currency;
    private String paramsJson;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime recordedAt;
    private String remark;
}