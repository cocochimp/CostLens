package com.costlens.common.exception;

import java.time.LocalDateTime;

public class RuleVersionConflictException extends CostLensException {

    public RuleVersionConflictException(String ruleKey, LocalDateTime recordedAt, LocalDateTime maxRecorded) {
        super(ErrorCodes.RULE_VERSION_CONFLICT,
                "版本记录时点冲突: ruleKey=" + ruleKey + ", recordedAt=" + recordedAt
                        + ", 已存在更晚或相同的记录时点=" + maxRecorded);
    }

    public RuleVersionConflictException(String message) {
        super(ErrorCodes.RULE_VERSION_CONFLICT, message);
    }
}