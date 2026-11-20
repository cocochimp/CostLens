package com.costlens.model;

/**
 * 重算口径。双口径之差即政策变更对历史成本的影响额。
 */
public enum RecalcMode {

    /** 按订单发生时的有效规则重算：回答「当时算得对不对」 */
    AS_OF_ORIGINAL,
    /** 按当前最新规则重算：回答「按今天的规则重跑会差多少」 */
    AS_OF_CURRENT
}