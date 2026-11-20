package com.costlens.start;

import com.costlens.adaptor.input.FeeController;
import com.costlens.adaptor.input.FreightController;
import com.costlens.adaptor.input.PricingController;
import com.costlens.application.fee.scenario.FeeChargeScenario;
import com.costlens.application.freight.scenario.FreightQuoteScenario;
import com.costlens.application.pricing.scenario.PricingScenario;
import com.costlens.client.fee.FeeApi;
import com.costlens.client.freight.FreightApi;
import com.costlens.client.pricing.PricingApi;
import com.costlens.common.exception.ZoneUnresolvableException;
import com.costlens.model.CostType;
import com.costlens.model.Currency;
import com.costlens.model.InfeasibleReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 契约测试：验证请求反序列化、枚举解析、统一响应与异常码。
 */
@WebMvcTest(controllers = {FreightController.class, FeeController.class, PricingController.class})
class ControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FreightQuoteScenario freightScenario;

    @MockBean
    private FeeChargeScenario feeScenario;

    @MockBean
    private PricingScenario pricingScenario;

    @Test
    @DisplayName("运费试算返回计价因子与 trace")
    void freightQuoteContract() throws Exception {
        given(freightScenario.quote(any())).willReturn(new FreightApi.QuoteRes(
                "UPS_GROUND", 1, 2_000L, 1_800L, 8, null, null, null,
                2_400L, 0L, 2_400L, Currency.USD, Map.of("billedGrams", 2000)));

        mockMvc.perform(post("/api/freight/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"market":"US","serviceLevel":"GROUND","destZip":"90001",
                                 "actualWeightGram":1200,"lengthCm":30,"widthCm":20,"heightCm":15,
                                 "currency":"USD","quoteTime":"2023-03-15T10:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.zone").value(8))
                .andExpect(jsonPath("$.data.billedWeightGram").value(2000))
                .andExpect(jsonPath("$.data.totalMinor").value(2400));
    }

    @Test
    @DisplayName("业务异常带错误码返回，HTTP 仍为 200")
    void freightQuoteBusinessError() throws Exception {
        given(freightScenario.quote(any()))
                .willThrow(new ZoneUnresolvableException("99999"));

        mockMvc.perform(post("/api/freight/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"market":"US","serviceLevel":"GROUND","destZip":"99999",
                                 "actualWeightGram":500,"lengthCm":10,"widthCm":10,"heightCm":10,
                                 "currency":"USD","quoteTime":"2023-03-15T10:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    @DisplayName("费用计提返回逐项明细与规则版本")
    void feeChargeContract() throws Exception {
        given(feeScenario.charge(any())).willReturn(new FeeApi.ChargeRes(
                List.of(new FeeApi.ChargeItemRes(CostType.COMMISSION, "PERCENTAGE_FLOOR",
                        "R1", 1, 1_800L, Map.of())), 1_800L, Currency.USD));

        mockMvc.perform(post("/api/fee/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"platform":"AMAZON_US","category":"ELECTRONICS","baseAmountMinor":10000,
                                 "currency":"USD","pieceCount":1,"volumeCubicCm":0,"weightGrams":0,
                                 "chargeTime":"2023-03-15T10:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].ruleType").value("PERCENTAGE_FLOOR"))
                .andExpect(jsonPath("$.data.totalMinor").value(1800));
    }

    @Test
    @DisplayName("定价不可行时返回缺口金额")
    void pricingInfeasibleContract() throws Exception {
        given(pricingScenario.solve(any())).willReturn(new PricingApi.SolveRes(
                false, 1_755L, 1_500L, 255L, InfeasibleReason.CAP_EXCEEDED.name(),
                "超出核价上限", List.of()));

        mockMvc.perform(post("/api/pricing/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"platform":"AMAZON_US","category":"ELECTRONICS","fixedCostMinor":1000,
                                 "currency":"USD","targetMarginPercent":"25","priceCapMinor":1500,
                                 "pieceCount":1,"volumeCubicCm":0,"weightGrams":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feasible").value(false))
                .andExpect(jsonPath("$.data.reason").value("CAP_EXCEEDED"))
                .andExpect(jsonPath("$.data.gapMinor").value(255));
    }
}