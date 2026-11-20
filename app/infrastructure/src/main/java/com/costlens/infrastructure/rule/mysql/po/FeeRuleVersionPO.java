package com.costlens.infrastructure.rule.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * fee_rule_version 持久化对象。只追加，不 UPDATE。
 */
@Data
@TableName("fee_rule_version")
public class FeeRuleVersionPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleKey;
    private Integer versionSeq;
    private String platform;
    private String category;
    private String costType;
    private String ruleType;
    private String paramsJson;
    private String currency;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime recordedAt;
    private String remark;
}