package com.costlens.application;

import com.costlens.application.cost.assembler.CostAssembler;
import com.costlens.application.cost.scenario.CostSheetScenario;
import com.costlens.application.cost.scenario.RecalcScenario;
import com.costlens.application.fee.assembler.FeeAssembler;
import com.costlens.application.fee.scenario.FeeChargeScenario;
import com.costlens.application.freight.assembler.FreightAssembler;
import com.costlens.application.freight.scenario.FreightQuoteScenario;
import com.costlens.application.pricing.scenario.PricingScenario;
import com.costlens.application.rule.scenario.RuleScenario;
import com.costlens.client.cost.CostApi;
import com.costlens.client.fee.FeeApi;
import com.costlens.client.freight.FreightApi;
import com.costlens.client.pricing.PricingApi;
import com.costlens.domain.fee.service.FeeChargeService;
import com.costlens.domain.freight.service.FreightRatingService;
import com.costlens.domain.freight.service.SurchargeCalculator;
import com.costlens.domain.freight.service.strategy.DimWeightZoneStrategy;
import com.costlens.domain.freight.service.strategy.FirstAddWeightStrategy;
import com.costlens.domain.freight.service.strategy.GirthSizeBandStrategy;
import com.costlens.domain.pricing.service.PricingSolver;
import com.costlens.model.CostType;
import com.costlens.model.Currency;
import com.costlens.model.Market;
import com.costlens.model.RecalcMode;
import com.costlens.model.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端业务链路验证（不依赖数据库）。
 * 覆盖：三市场运费 → 平台费用 → 成本单 → 定价反推 → 批次生命周期。
 */
class EndToEndCostFlowTest {

    private FreightQuoteScenario freightScenario;
    private FeeChargeScenario feeScenario;
    private CostSheetScenario costSheetScenario;
    private PricingScenario pricingScenario;
    private RecalcScenario recalcScenario;
    private InMemoryRepositories.RecordingNotifier notifier;

    @BeforeEach
    void setUp() {
        var feeRepo = new InMemoryRepositories.FeeRuleRepo();
        var cardRepo = new InMemoryRepositories.FreightRateCardRepo();
        var sheetRepo = new InMemoryRepositories.CostSheetRepo();
        var batchRepo = new InMemoryRepositories.RecalcBatchRepo();
        notifier = new InMemoryRepositories.RecordingNotifier();

        cardRepo.save(Fixtures.usCard());
        cardRepo.save(Fixtures.jpCard());
        cardRepo.save(Fixtures.seaCard());
        feeRepo.save(Fixtures.commissionWithFloor());
        feeRepo.save(Fixtures.paymentRule());
        feeRepo.save(Fixtures.apparelTiered());
        feeRepo.save(Fixtures.shopeeMyCommission());

        SurchargeCalculator surcharge = SurchargeCalculator.NONE;
        FreightRatingService ratingService = new FreightRatingService(List.of(
                new DimWeightZoneStrategy(surcharge),
                new GirthSizeBandStrategy(surcharge),
                new FirstAddWeightStrategy(surcharge)));

        freightScenario = new FreightQuoteScenario(cardRepo, ratingService, new FreightAssembler());
        feeScenario = new FeeChargeScenario(feeRepo, new FeeChargeService(), new FeeAssembler());
        costSheetScenario = new CostSheetScenario(freightScenario, feeScenario, sheetRepo,
                notifier, new CostAssembler());
        pricingScenario = new PricingScenario(feeRepo, new PricingSolver());
        recalcScenario = new RecalcScenario(batchRepo,
                new InMemoryRepositories.InMemoryIdempotencyGuard(), new CostAssembler());
    }

    @Test
    @DisplayName("美国：体积重生效，计费重 2000g × $12/kg = $24.00")
    void usFreight() {
        FreightApi.QuoteRes res = freightScenario.quote(new FreightApi.QuoteReq(
                Market.US, null, "GROUND", "90001", null, null,
                1_200L, 30, 20, 15, Currency.USD, Fixtures.BIZ));

        assertThat(res.rateKey()).isEqualTo("UPS_GROUND");
        assertThat(res.dimWeightGram()).isEqualTo(1_800L);
        assertThat(res.billedWeightGram()).isEqualTo(2_000L);
        assertThat(res.zone()).isEqualTo(8);
        assertThat(res.totalMinor()).isEqualTo(2_400L);
    }

    @Test
    @DisplayName("日本：三边和 80cm 落 S80，重量不参与计价")
    void jpFreight() {
        FreightApi.QuoteRes res = freightScenario.quote(new FreightApi.QuoteReq(
                Market.JP, null, null, null, "KANTO", null,
                3_000L, 26, 27, 27, Currency.JPY, Fixtures.BIZ));

        assertThat(res.girthCm()).isEqualTo(80);
        assertThat(res.sizeBand()).isEqualTo("S80");
        assertThat(res.totalMinor()).isEqualTo(1_200L);
    }

    @Test
    @DisplayName("东南亚：0.7kg 得 2 个续重，¥28 + 2×¥3.20 = ¥34.40")
    void seaFreight() {
        FreightApi.QuoteRes res = freightScenario.quote(new FreightApi.QuoteReq(
                Market.SEA, null, null, null, null, "MY",
                700L, 10, 10, 10, Currency.CNY, Fixtures.BIZ));

        assertThat(res.continuationUnits()).isEqualTo(2L);
        assertThat(res.totalMinor()).isEqualTo(3_440L);
    }

    @Test
    @DisplayName("费用计提：$100 电子类目 = 佣金 $15.00 + 支付 $3.00")
    void feeCharge() {
        FeeApi.ChargeRes res = feeScenario.charge(new FeeApi.ChargeReq(
                Platform.AMAZON_US, "ELECTRONICS", 10_000L, Currency.USD, 1, 0L, 0L, Fixtures.BIZ));

        assertThat(res.items()).hasSize(2);
        assertThat(res.totalMinor()).isEqualTo(1_800L);
    }

    @Test
    @DisplayName("成本单：$25 收入 → 总成本 $14.50 → 净利率 42%")
    void costSheetEndToEnd() {
        CostApi.SheetRes res = costSheetScenario.generate(new CostApi.SheetReq(
                "SHEET-1", "ORD-1", Platform.AMAZON_US, "ELECTRONICS",
                2_500L, Currency.USD, Fixtures.BIZ,
                new CostApi.FreightSpec(Market.US, "GROUND", "90001", null, null,
                        500L, 10, 10, 10),
                List.of(new CostApi.FixedCostSpec(CostType.ADVERTISING, 600L, "CPC"))));

        // 运费 400 + 佣金 375 + 支付 75 + 采购 600 = 1450
        assertThat(res.totalCostMinor()).isEqualTo(1_450L);
        assertThat(res.netProfitMinor()).isEqualTo(1_050L);
        assertThat(res.netMarginPercent()).isEqualTo("42");
        assertThat(res.status()).isEqualTo("CONFIRMED");

        // 读回校验
        CostApi.SheetRes readBack = costSheetScenario.query("SHEET-1");
        assertThat(readBack.totalCostMinor()).isEqualTo(1_450L);
    }

    @Test
    @DisplayName("定价反推：固定成本 $10、佣金 15% + 支付 3%、目标毛利 25%")
    void pricing() {
        PricingApi.SolveRes res = pricingScenario.solve(new PricingApi.SolveReq(
                Platform.AMAZON_US, "ELECTRONICS", 1_000L, Currency.USD,
                "25", null, 1, 0L, 0L));

        assertThat(res.feasible()).isTrue();
        // 1000 / (1 − 0.15 − 0.03 − 0.25) = 1754.3…→ 1755
        assertThat(res.solvedPriceMinor()).isEqualTo(1_755L);
    }

    @Test
    @DisplayName("定价反推：核价上限低于所需售价 → 输出缺口而非硬凑")
    void pricingWithCap() {
        PricingApi.SolveRes res = pricingScenario.solve(new PricingApi.SolveReq(
                Platform.AMAZON_US, "ELECTRONICS", 1_000L, Currency.USD,
                "25", 1_500L, 1, 0L, 0L));

        assertThat(res.feasible()).isFalse();
        assertThat(res.reason()).isEqualTo("CAP_EXCEEDED");
        assertThat(res.gapMinor()).isEqualTo(255L);
    }

    @Test
    @DisplayName("双时态：佣金 5.5% → 6%，按业务生效时间切换版本")
    void bitemporalFeeRule() {
        FeeRuleFixtureHolder holder = new FeeRuleFixtureHolder();
        RuleScenario scenario = new RuleScenario(holder.repo());

        var v1 = scenario.asOf("SHOPEE_MY_ALL_COMMISSION",
                java.time.LocalDateTime.of(2023, 3, 15, 0, 0), null);
        var v2 = scenario.asOf("SHOPEE_MY_ALL_COMMISSION",
                java.time.LocalDateTime.of(2023, 8, 15, 0, 0), null);

        assertThat(v1.versionSeq()).isEqualTo(1);
        assertThat(v2.versionSeq()).isEqualTo(2);
    }

    @Test
    @DisplayName("重算批次：3 分片，1 失败 → PARTIAL 且可查询")
    void recalcBatchLifecycle() {
        CostApi.RecalcRes created = recalcScenario.create(new CostApi.RecalcReq(
                RecalcMode.AS_OF_ORIGINAL,
                java.time.LocalDateTime.of(2023, 1, 1, 0, 0),
                java.time.LocalDateTime.of(2023, 1, 31, 0, 0), 3));
        assertThat(created.status()).isEqualTo("RUNNING");

        recalcScenario.reportShard(created.batchId(), 0, true);
        recalcScenario.reportShard(created.batchId(), 1, false);
        CostApi.RecalcRes last = recalcScenario.reportShard(created.batchId(), 2, true);

        assertThat(last.status()).isEqualTo("PARTIAL");
        assertThat(last.shardFailed()).isEqualTo(1);
        assertThat(last.remainingShards()).isZero();
    }

    @Test
    @DisplayName("分片重复上报被幂等闸门拦截")
    void duplicateShardReportRejected() {
        CostApi.RecalcRes created = recalcScenario.create(new CostApi.RecalcReq(
                RecalcMode.AS_OF_CURRENT,
                java.time.LocalDateTime.of(2023, 1, 1, 0, 0),
                java.time.LocalDateTime.of(2023, 1, 31, 0, 0), 2));

        recalcScenario.reportShard(created.batchId(), 0, true);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> recalcScenario.reportShard(created.batchId(), 0, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重复上报");
    }

    @Test
    @DisplayName("成本单确认后广播事件，且事件内容与成本单一致")
    void costSheetConfirmationIsBroadcast() {
        costSheetScenario.generate(new CostApi.SheetReq(
                "SHEET-2", "ORD-2", Platform.AMAZON_US, "ELECTRONICS",
                2_500L, Currency.USD, Fixtures.BIZ,
                new CostApi.FreightSpec(Market.US, "GROUND", "90001", null, null,
                        500L, 10, 10, 10),
                List.of(new CostApi.FixedCostSpec(CostType.ADVERTISING, 600L, "CPC"))));

        assertThat(notifier.events()).hasSize(1);
        assertThat(notifier.events().get(0)).isEqualTo("SHEET-2|ORD-2|1450|USD");
    }

    /** 单独的规则仓储持有者，避免与主仓储的规则互相干扰 */
    private static final class FeeRuleFixtureHolder {
        private final InMemoryRepositories.FeeRuleRepo repo = new InMemoryRepositories.FeeRuleRepo();

        private FeeRuleFixtureHolder() {
            repo.save(Fixtures.shopeeMyCommission());
        }

        private InMemoryRepositories.FeeRuleRepo repo() {
            return repo;
        }
    }
}