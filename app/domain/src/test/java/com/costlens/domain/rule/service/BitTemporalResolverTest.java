package com.costlens.domain.rule.service;

import com.costlens.domain.rule.model.RuleVersionRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BitTemporalResolverTest {

    private final BitTemporalResolver resolver = new BitTemporalResolver();

    private static final LocalDateTime VALID_FROM = LocalDateTime.of(2023, 1, 1, 0, 0);
    private static final LocalDateTime VALID_TO = LocalDateTime.of(2023, 6, 30, 0, 0);

    private final List<RuleVersionRef> versions = List.of(
            new RuleVersionRef(1L, "R", 1, VALID_FROM, VALID_TO, LocalDateTime.of(2023, 3, 1, 0, 0)),
            new RuleVersionRef(2L, "R", 2, VALID_FROM, null, LocalDateTime.of(2023, 3, 10, 0, 0)));

    @Test
    @DisplayName("业务时点查询不受 recordedAt 影响")
    void atBusinessTime() {
        var hit = resolver.atBusinessTime(versions, LocalDateTime.of(2023, 2, 15, 0, 0));
        assertThat(hit).isPresent();
        assertThat(hit.get().versionSeq()).isEqualTo(2);
    }

    @Test
    @DisplayName("as-of 查询只看到 knownAt 之前的记录")
    void asOfFiltersByKnownAt() {
        var before = resolver.asOf(versions,
                LocalDateTime.of(2023, 2, 15, 0, 0), LocalDateTime.of(2023, 3, 5, 0, 0));
        assertThat(before).isPresent();
        assertThat(before.get().versionSeq()).isEqualTo(1);

        var after = resolver.asOf(versions,
                LocalDateTime.of(2023, 2, 15, 0, 0), LocalDateTime.of(2023, 3, 15, 0, 0));
        assertThat(after).isPresent();
        assertThat(after.get().versionSeq()).isEqualTo(2);
    }

    @Test
    @DisplayName("业务时间早于所有版本时无命中")
    void noHitBeforeValidity() {
        assertThat(resolver.atBusinessTime(versions, LocalDateTime.of(2022, 12, 1, 0, 0))).isEmpty();
    }

    @Test
    @DisplayName("空集合与空时间安全返回")
    void emptyInputsAreSafe() {
        assertThat(resolver.atBusinessTime(List.of(), LocalDateTime.now())).isEmpty();
        assertThat(resolver.atBusinessTime(versions, null)).isEmpty();
        assertThat(resolver.asOf(List.of(), LocalDateTime.now(), LocalDateTime.now())).isEmpty();
    }

    @Test
    @DisplayName("validAt 为左闭右开区间")
    void validAtIsHalfOpen() {
        RuleVersionRef ref = versions.get(0);
        assertThat(ref.validAt(VALID_FROM)).isTrue();
        assertThat(ref.validAt(VALID_TO)).isFalse();
        assertThat(ref.validAt(VALID_TO.minusSeconds(1))).isTrue();
    }
}