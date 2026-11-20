package com.costlens.domain.rule.model;

/**
 * 邮编前缀区间 → Zone。前缀必须是定长字符串，保住前导零（00501 不能变成 501）。
 */
public record ZoneRange(String zipPrefixFrom, String zipPrefixTo, int zone) {

    public ZoneRange {
        if (zipPrefixFrom == null || zipPrefixTo == null) {
            throw new IllegalArgumentException("zip prefix is null");
        }
        if (zipPrefixFrom.length() != zipPrefixTo.length()) {
            throw new IllegalArgumentException("zip prefix length mismatch");
        }
        if (zipPrefixFrom.compareTo(zipPrefixTo) > 0) {
            throw new IllegalArgumentException("zipPrefixFrom > zipPrefixTo");
        }
        if (zone < 1 || zone > 8) {
            throw new IllegalArgumentException("zone must be within 1..8, got " + zone);
        }
    }

    public boolean contains(String prefix) {
        return zipPrefixFrom.compareTo(prefix) <= 0 && zipPrefixTo.compareTo(prefix) >= 0;
    }
}