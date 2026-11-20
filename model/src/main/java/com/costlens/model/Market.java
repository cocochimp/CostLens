package com.costlens.model;

/**
 * 计费市场。三个市场的运费数学结构互不相同，禁止用同一模型统一。
 */
public enum Market {

    /** 美国：计费重 × Zone 阶梯单价，连续线性 */
    US,
    /** 日本：三边和 → Size 档 → 地帯价，阶跃查表，重量仅作上限校验 */
    JP,
    /** 东南亚：首重 + 续重向上取整，分段 + 取整 */
    SEA
}