package com.costlens.domain.rule.model;

import com.costlens.common.exception.RuleVersionConflictException;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.CostType;
import com.costlens.model.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeeRuleTest {

    private static final LocalDateTime VALID_FROM = LocalDateTime.of(2023, 1, 1, 0, 0);
    private static final LocalDateTime RECORD_V1 = LocalDateTime.of(2023, 3, 1, 0, 0);
    private static final LocalDateTime RECORD_V2 = LocalDateTime.of(2023, 3, 10, 0, 0);
    private static final LocalDateTime BIZ_TIME = LocalDateTime.of(2023, 2, 15, 0, 0);

    private FeeRule rule;

    @BeforeEach
    void setUp() {
        rule = new FeeRule("AMZ_US_COMMISSION_ELECTRONICS", Platform.AMAZON_US, "ELECTRONICS", CostType.COMMISSION);
        // v1：2023-03-01 录入，费率误填 15%（实际应为 16%）
        rule.appendVersion(FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("15")),
                VALID_FROM, null, RECORD_V1);
        // v2：2023-03-10 录入更正为 16%，业务生效时间不变
        rule.appendVersion(FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("16")),
                VALID_FROM, null, RECORD_V2);
    }

    @Test
    @DisplayName("业务时点查询取最新认知：日常核算口径")
    void resolveAtUsesLatestCognition() {
        FeeRuleVersion v = rule.resolveAt(BIZ_TIME);
        assertThat(v.ref().versionSeq()).isEqualTo(2);
        assertThat(v.params().rate()).isEqualTo(Rate.ofPercent("16"));
    }

    @Test
    @DisplayName("as-of 查询复现历史决策依据：3-05 那天系统认为 1 月适用 15%")
    void asOfReproducesHistoricalCognition() {
        FeeRuleVersion v1 = rule.resolveAsOf(BIZ_TIME, LocalDateTime.of(2023, 3, 5, 0, 0));
        assertThat(v1.ref().versionSeq()).isEqualTo(1);
        assertThat(v1.params().rate()).isEqualTo(Rate.ofPercent("15"));
    }

    @Test
    @DisplayName("修正之后 as-of 查询看到新版本")
    void asOfAfterCorrectionSeesNewVersion() {
        FeeRuleVersion v2 = rule.resolveAsOf(BIZ_TIME, LocalDateTime.of(2023, 3, 15, 0, 0));
        assertThat(v2.ref().versionSeq()).isEqualTo(2);
        assertThat(v2.params().rate()).isEqualTo(Rate.ofPercent("16"));
    }

    @Test
    @DisplayName("recordedAt 必须严格递增，否则拒绝写入")
    void recordedAtMustStrictlyIncrease() {
        assertThatThrownBy(() -> rule.appendVersion(
                FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("17")),
                VALID_FROM, null, RECORD_V2))
                .isInstanceOf(RuleVersionConflictException.class);
    }

    @Test
    @DisplayName("业务生效窗口外查询抛出规则不存在，不回退默认值")
    void outsideValidityThrows() {
        assertThatThrownBy(() -> rule.resolveAt(LocalDateTime.of(2022, 12, 31, 23, 59)))
                .hasMessageContaining("未找到规则");
    }

    @Test
    @DisplayName("版本只追加，历史版本不被修改")
    void versionsAreAppendOnly() {
        assertThat(rule.versions()).hasSize(2);
        assertThat(rule.versions().get(0).params().rate()).isEqualTo(Rate.ofPercent("15"));
        assertThat(rule.versions().get(1).params().rate()).isEqualTo(Rate.ofPercent("16"));
    }
}