-- 种子数据：三市场运费费率卡 + 平台费用规则
-- 数值按公开费率结构拟真，用于本地跑通与测试

-- ============ 运费费率卡 ============
INSERT INTO freight_rate_version (id, rate_key, version_seq, carrier, market, currency, params_json,
                                  valid_from, valid_to, recorded_at, remark)
VALUES (1, 'UPS_GROUND', 1, 'UPS_GROUND', 'US', 'USD',
        '{"dimDivisor":5000,"roundStepGrams":500}',
        '2023-01-01 00:00:00', NULL, '2023-01-01 00:00:00', '美国站初始运价'),
       (2, 'YAMATO_TAKKYUUBIN', 1, 'YAMATO_TAKKYUUBIN', 'JP', 'JPY',
        '{"dimDivisor":5000,"roundStepGrams":100}',
        '2023-01-01 00:00:00', NULL, '2023-01-01 00:00:00', '日本站初始运价'),
       (3, 'SHOPEE_SLS', 1, 'SHOPEE_SLS', 'SEA', 'CNY',
        '{"dimDivisor":5000,"roundStepGrams":100}',
        '2023-01-01 00:00:00', NULL, '2023-01-01 00:00:00', '东南亚专线初始运价');

-- 美国：Zone 8 / Zone 3 各三档，单价按每公斤（分）
INSERT INTO us_zone_rate (rate_version_id, zone, service_level, weight_from_gram, weight_to_gram,
                          unit_price_minor, currency)
VALUES (1, 8, 'GROUND', 0, 1000, 800, 'USD'),
       (1, 8, 'GROUND', 1000, 2000, 1000, 'USD'),
       (1, 8, 'GROUND', 2000, 5000, 1200, 'USD'),
       (1, 3, 'GROUND', 0, 1000, 400, 'USD'),
       (1, 3, 'GROUND', 1000, 2000, 500, 'USD'),
       (1, 3, 'GROUND', 2000, 5000, 600, 'USD');

INSERT INTO zone_map (rate_version_id, zip_prefix_from, zip_prefix_to, zone)
VALUES (1, '90000', '96199', 8),
       (1, '00500', '00599', 3);

-- 日本：KANTO 地帯各 Size 档价格（円）
INSERT INTO jp_size_rate (rate_version_id, area, size_band, girth_cm_max, weight_cap_gram, price_minor, currency)
VALUES (2, 'KANTO', 'S60', 60, 2000, 800, 'JPY'),
       (2, 'KANTO', 'S80', 80, 5000, 1200, 'JPY'),
       (2, 'KANTO', 'S100', 100, 10000, 1600, 'JPY'),
       (2, 'KANTO', 'S120', 120, 15000, 2100, 'JPY'),
       (2, 'KANTO', 'S140', 140, 20000, 2600, 'JPY'),
       (2, 'KANTO', 'S160', 160, 25000, 3100, 'JPY'),
       (2, 'KANTO', 'S180', 180, 30000, 3700, 'JPY');

-- 东南亚：马来西亚 / 泰国 / 印尼首重续重（CNY 分）
INSERT INTO sea_first_add_rate (rate_version_id, country, first_weight_gram, first_price_minor,
                                continuation_unit_gram, continuation_price_minor, currency)
VALUES (3, 'MY', 500, 2800, 100, 320, 'CNY'),
       (3, 'TH', 500, 3200, 100, 380, 'CNY'),
       (3, 'ID', 500, 3800, 100, 450, 'CNY');

-- ============ 平台费用规则 ============
-- Amazon 美国站电子产品佣金：15% + 每笔最低 $0.30
INSERT INTO fee_rule_version (id, rule_key, version_seq, platform, category, cost_type, rule_type,
                              params_json, currency, valid_from, valid_to, recorded_at, remark)
VALUES (1, 'AMAZON_US_ELECTRONICS_COMMISSION', 1, 'AMAZON_US', 'ELECTRONICS', 'COMMISSION',
        'PERCENTAGE_FLOOR',
        '{"costType":"COMMISSION","ruleType":"PERCENTAGE_FLOOR","rate":{"value":0.15000000},"floor":{"minorUnits":30,"currency":"USD"},"tiers":null,"measureKind":null,"unitPrice":null,"cap":null,"mutexGroup":null,"priority":0}',
        'USD', '2023-01-01 00:00:00', NULL, '2023-01-01 00:00:00', '电子类目 15%，最低 $0.30');

-- Amazon 美国站服装佣金：$250 以内 17%，超出部分 5%
INSERT INTO fee_rule_version (id, rule_key, version_seq, platform, category, cost_type, rule_type,
                              params_json, currency, valid_from, valid_to, recorded_at, remark)
VALUES (2, 'AMAZON_US_APPAREL_COMMISSION', 1, 'AMAZON_US', 'APPAREL', 'COMMISSION',
        'TIERED_BY_AMOUNT',
        '{"costType":"COMMISSION","ruleType":"TIERED_BY_AMOUNT","rate":null,"floor":null,"tiers":[{"lowerInclusive":{"minorUnits":0,"currency":"USD"},"upperExclusive":{"minorUnits":25000,"currency":"USD"},"rate":{"value":0.17000000}},{"lowerInclusive":{"minorUnits":25000,"currency":"USD"},"upperExclusive":null,"rate":{"value":0.05000000}}],"measureKind":null,"unitPrice":null,"cap":null,"mutexGroup":null,"priority":0}',
        'USD', '2023-01-01 00:00:00', NULL, '2023-01-01 00:00:00', '服装 $250 以上部分仅 5%');

-- Amazon 美国站支付手续费：3%
INSERT INTO fee_rule_version (id, rule_key, version_seq, platform, category, cost_type, rule_type,
                              params_json, currency, valid_from, valid_to, recorded_at, remark)
VALUES (3, 'AMAZON_US_ALL_PAYMENT', 1, 'AMAZON_US', 'ALL', 'PAYMENT',
        'PERCENTAGE',
        '{"costType":"PAYMENT","ruleType":"PERCENTAGE","rate":{"value":0.03000000},"floor":null,"tiers":null,"measureKind":null,"unitPrice":null,"cap":null,"mutexGroup":null,"priority":0}',
        'USD', '2023-01-01 00:00:00', NULL, '2023-01-01 00:00:00', '支付通道比例费 3%');

-- Shopee 马来站佣金：5.5%
INSERT INTO fee_rule_version (id, rule_key, version_seq, platform, category, cost_type, rule_type,
                              params_json, currency, valid_from, valid_to, recorded_at, remark)
VALUES (4, 'SHOPEE_MY_ALL_COMMISSION', 1, 'SHOPEE_MY', 'ALL', 'COMMISSION',
        'PERCENTAGE',
        '{"costType":"COMMISSION","ruleType":"PERCENTAGE","rate":{"value":0.05500000},"floor":null,"tiers":null,"measureKind":null,"unitPrice":null,"cap":null,"mutexGroup":null,"priority":0}',
        'MYR', '2023-01-01 00:00:00', NULL, '2023-01-01 00:00:00', '马来站佣金 5.5%');

-- 同一规则的第二版本：2023-07-01 起佣金调整为 6%，recorded_at 晚于 v1
INSERT INTO fee_rule_version (id, rule_key, version_seq, platform, category, cost_type, rule_type,
                              params_json, currency, valid_from, valid_to, recorded_at, remark)
VALUES (5, 'SHOPEE_MY_ALL_COMMISSION', 2, 'SHOPEE_MY', 'ALL', 'COMMISSION',
        'PERCENTAGE',
        '{"costType":"COMMISSION","ruleType":"PERCENTAGE","rate":{"value":0.06000000},"floor":null,"tiers":null,"measureKind":null,"unitPrice":null,"cap":null,"mutexGroup":null,"priority":0}',
        'MYR', '2023-07-01 00:00:00', NULL, '2023-06-20 00:00:00', '佣金 5.5% → 6%，业务时间 7-01 生效');