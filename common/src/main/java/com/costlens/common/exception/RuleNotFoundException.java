package com.costlens.common.exception;

import java.time.LocalDateTime;

public class RuleNotFoundException extends CostLensException {

    public RuleNotFoundException(String ruleKey, LocalDateTime bizTime) {
        super(ErrorCodes.RULE_NOT_FOUND,
                "未找到规则: ruleKey=" + ruleKey + ", bizTime=" + bizTime);
    }

    public RuleNotFoundException(String message) {
        super(ErrorCodes.RULE_NOT_FOUND, message);
    }
}