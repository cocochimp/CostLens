package com.costlens.domain.freight;

import com.costlens.common.exception.OversizeException;
import com.costlens.common.exception.ZoneUnresolvableException;
import com.costlens.domain.freight.model.FreightQuote;
import com.costlens.domain.freight.model.ShipmentContext;
import com.costlens.domain.freight.service.FreightRatingService;
import com.costlens.domain.freight.service.SurchargeCalculator;
import com.costlens.domain.freight.service.strategy.DimWeightZoneStrategy;
import com.costlens.domain.freight.service.strategy.FirstAddWeightStrategy;
import com.costlens.domain.freight.service.strategy.GirthSizeBandStrategy;
import com.costlens.domain.shared.model.JpSizeBand;
import com.costlens.domain.shared.model.Length3;
import com.costlens.domain.shared.model.Weight;
import com.costlens.model.Currency;
import com.costlens.model.Market;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FreightRatingServiceTest {

    private final FreightRatingService service = new FreightRatingService(List.of(
            new DimWeightZoneStrategy(SurchargeCalculator.NONE),
            new GirthSizeBandStrategy(SurchargeCalculator.NONE),
            new FirstAddWeightStrategy(SurchargeCalculator.NONE)));

    @Nested
    @DisplayName("美国：计费重 × Zone 阶梯单价")
    class Us {

        @Test
        @DisplayName("体积重生效：30×20×15cm 的 1200g 实重按 2000g 计费")
        void dimWeightTakesOver() {
            ShipmentContext ctx = ShipmentContext.builder(Market.US, Currency.USD)
                    .destZip("90001")
                    .actualWeight(Weight.ofGrams(1200))
                    .dimensions(new Length3(30, 20, 15))
                    .build();

            FreightQuote quote = service.quote(ctx, TestSnapshots.us());

            // 9000cm³ × 1000 ÷ 5000 = 1800g 体积重 > 1200g 实重 → 进位到 2000g
            assertThat(quote.billedWeightGram()).isEqualTo(2000L);
            assertThat(quote.dimWeightGram()).isEqualTo(1800L);
            assertThat(quote.zone()).isEqualTo(8);
            // $12.00/kg × 2000g ÷ 1000 = $24.00 = 2400 分
            assertThat(quote.baseFreight().minorUnits()).isEqualTo(2400L);
            assertThat(quote.total().minorUnits()).isEqualTo(2400L);
        }

        @Test
        @DisplayName("实重大于体积重时按实重计费")
        void actualWeightWins() {
            ShipmentContext ctx = ShipmentContext.builder(Market.US, Currency.USD)
                    .destZip("90001")
                    .actualWeight(Weight.ofGrams(2400))
                    .dimensions(new Length3(10, 10, 10))
                    .build();

            FreightQuote quote = service.quote(ctx, TestSnapshots.us());
            assertThat(quote.billedWeightGram()).isEqualTo(2500L);
            assertThat(quote.baseFreight().minorUnits()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("邮编前导零保留：00501 → Zone 3")
        void leadingZeroZipKept() {
            ShipmentContext ctx = ShipmentContext.builder(Market.US, Currency.USD)
                    .destZip("00501")
                    .actualWeight(Weight.ofGrams(500))
                    .dimensions(new Length3(10, 10, 10))
                    .build();

            assertThat(service.quote(ctx, TestSnapshots.us()).zone()).isEqualTo(3);
        }

        @Test
        @DisplayName("邮编无法分区时直接失败，不猜默认 Zone")
        void unknownZipFails() {
            ShipmentContext ctx = ShipmentContext.builder(Market.US, Currency.USD)
                    .destZip("12345")
                    .actualWeight(Weight.ofGrams(500))
                    .dimensions(new Length3(10, 10, 10))
                    .build();

            assertThatThrownBy(() -> service.quote(ctx, TestSnapshots.us()))
                    .isInstanceOf(ZoneUnresolvableException.class);
        }
    }

    @Nested
    @DisplayName("日本：三边和 → Size 档")
    class Jp {

        @Test
        @DisplayName("三边和档位边界：含上界")
        void girthBoundaryInclusive() {
            assertThat(JpSizeBand.ofGirth(59)).isEqualTo(JpSizeBand.S60);
            assertThat(JpSizeBand.ofGirth(60)).isEqualTo(JpSizeBand.S60);
            assertThat(JpSizeBand.ofGirth(61)).isEqualTo(JpSizeBand.S80);
            assertThat(JpSizeBand.ofGirth(79)).isEqualTo(JpSizeBand.S80);
            assertThat(JpSizeBand.ofGirth(80)).isEqualTo(JpSizeBand.S80);
            assertThat(JpSizeBand.ofGirth(81)).isEqualTo(JpSizeBand.S100);
        }

        @Test
        @DisplayName("重量不参与计价，只做上限校验")
        void weightIsCapOnly() {
            ShipmentContext light = jpCtx(Weight.ofGrams(1000), new Length3(26, 27, 27));
            ShipmentContext heavy = jpCtx(Weight.ofGrams(4500), new Length3(26, 27, 27));

            FreightQuote q1 = service.quote(light, TestSnapshots.jp());
            FreightQuote q2 = service.quote(heavy, TestSnapshots.jp());

            // 同为 S80，重量差 4.5 倍但价格完全相同
            assertThat(q1.sizeBand()).isEqualTo(JpSizeBand.S80);
            assertThat(q2.sizeBand()).isEqualTo(JpSizeBand.S80);
            assertThat(q1.baseFreight().minorUnits()).isEqualTo(q2.baseFreight().minorUnits());
        }

        @Test
        @DisplayName("超过档位重量上限则升档")
        void overweightUpgradesBand() {
            // S80 上限 5kg，实重 6kg → S100（上限 10kg）
            ShipmentContext ctx = jpCtx(Weight.ofGrams(6000), new Length3(26, 27, 27));
            FreightQuote quote = service.quote(ctx, TestSnapshots.jp());

            assertThat(quote.sizeBand()).isEqualTo(JpSizeBand.S100);
            assertThat(quote.baseFreight().minorUnits()).isEqualTo(1600L);
        }

        @Test
        @DisplayName("超过 S180 直接拒绝")
        void oversizeRejected() {
            assertThatThrownBy(() -> JpSizeBand.ofGirth(181))
                    .isInstanceOf(OversizeException.class);
        }

        private ShipmentContext jpCtx(Weight weight, Length3 dims) {
            return ShipmentContext.builder(Market.JP, Currency.JPY)
                    .area("KANTO").actualWeight(weight).dimensions(dims).build();
        }
    }

    @Nested
    @DisplayName("东南亚：首重 + 续重向上取整")
    class Sea {

        @Test
        @DisplayName("续重个数按整数克计算，0.7kg 得 2 个续重")
        void continuationUnits() {
            FreightQuote quote = service.quote(seaCtx(Weight.ofKilograms("0.7")), TestSnapshots.sea());
            assertThat(quote.continuationUnits()).isEqualTo(2L);
            // ¥28.00 + 2 × ¥3.20 = ¥34.40 = 3440 分
            assertThat(quote.total().minorUnits()).isEqualTo(3440L);
        }

        @Test
        @DisplayName("不足首重只收首重价")
        void belowFirstWeight() {
            FreightQuote quote = service.quote(seaCtx(Weight.ofGrams(300)), TestSnapshots.sea());
            assertThat(quote.continuationUnits()).isZero();
            assertThat(quote.total().minorUnits()).isEqualTo(2800L);
        }

        @Test
        @DisplayName("恰好首重不加续重")
        void exactlyFirstWeight() {
            FreightQuote quote = service.quote(seaCtx(Weight.ofGrams(500)), TestSnapshots.sea());
            assertThat(quote.continuationUnits()).isZero();
            assertThat(quote.total().minorUnits()).isEqualTo(2800L);
        }

        @Test
        @DisplayName("多件累加后不得多算一个续重")
        void multiPieceNoExtraUnit() {
            // 0.2kg + 0.4kg = 0.6kg，整数克累加精确 600g
            Weight total = Weight.ofKilograms("0.2").plus(Weight.ofKilograms("0.4"));
            FreightQuote quote = service.quote(seaCtx(total), TestSnapshots.sea());
            assertThat(total.grams()).isEqualTo(600L);
            assertThat(quote.continuationUnits()).isEqualTo(1L);
            assertThat(quote.total().minorUnits()).isEqualTo(3120L);
        }

        @Test
        @DisplayName("不同国家费率不同")
        void countryRateDiffers() {
            FreightQuote my = service.quote(seaCtx(Weight.ofGrams(600)), TestSnapshots.sea());
            FreightQuote th = service.quote(
                    ShipmentContext.builder(Market.SEA, Currency.CNY)
                            .country("TH").actualWeight(Weight.ofGrams(600))
                            .dimensions(new Length3(10, 10, 10)).build(),
                    TestSnapshots.sea());

            assertThat(th.total().minorUnits()).isGreaterThan(my.total().minorUnits());
        }

        private ShipmentContext seaCtx(Weight weight) {
            return ShipmentContext.builder(Market.SEA, Currency.CNY)
                    .country("MY").actualWeight(weight).dimensions(new Length3(10, 10, 10)).build();
        }
    }

    @Test
    @DisplayName("费率卡市场与上下文不一致时拒绝计算")
    void marketMismatchRejected() {
        ShipmentContext ctx = ShipmentContext.builder(Market.US, Currency.USD)
                .destZip("90001").actualWeight(Weight.ofGrams(500))
                .dimensions(new Length3(10, 10, 10)).build();

        assertThatThrownBy(() -> service.quote(ctx, TestSnapshots.jp()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}