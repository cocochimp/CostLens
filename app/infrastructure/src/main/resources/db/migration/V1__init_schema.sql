-- CostLens 初始表结构
-- 约定：
--   1. 金额一律 BIGINT 存币种最小单位（USD=分，JPY=円），币种单独一列，禁止 DECIMAL 浮点混用
--   2. 规则表只追加不修改：valid_from/valid_to 是业务生效区间，recorded_at 是系统记录时点
--   3. 不建物理外键，逻辑关联 + 索引；跨表一致性由应用层聚合根保证

CREATE TABLE IF NOT EXISTS fee_rule_version
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    rule_key    VARCHAR(128) NOT NULL COMMENT '规则逻辑标识，同 key 多版本',
    version_seq INT          NOT NULL COMMENT '版本序号，从 1 递增',
    platform    VARCHAR(32)  NOT NULL COMMENT 'AMAZON_US/AMAZON_JP/SHOPEE_MY/SHOPEE_TH/SHOPEE_ID/TEMU_SEMI',
    category    VARCHAR(64)  NOT NULL DEFAULT 'ALL' COMMENT '类目，ALL 表示不分品类',
    cost_type   VARCHAR(32)  NOT NULL COMMENT 'COMMISSION/PAYMENT/STORAGE/ADVERTISING/FREIGHT',
    rule_type   VARCHAR(32)  NOT NULL COMMENT 'PERCENTAGE/PERCENTAGE_FLOOR/TIERED_BY_AMOUNT/PER_UNIT_BY_MEASURE',
    params_json JSON         NOT NULL COMMENT '规则参数',
    currency    CHAR(3)      NOT NULL COMMENT '费用币种',
    valid_from  DATETIME     NOT NULL COMMENT '业务生效起（含）',
    valid_to    DATETIME     NULL COMMENT '业务生效止（不含），NULL 表示无期限',
    recorded_at DATETIME     NOT NULL COMMENT '系统记录时点，transaction time',
    remark      VARCHAR(512) NULL COMMENT '版本说明',
    PRIMARY KEY (id),
    UNIQUE KEY uk_key_seq (rule_key, version_seq),
    UNIQUE KEY uk_key_recorded (rule_key, recorded_at),
    KEY idx_resolve (rule_key, valid_from, valid_to, recorded_at),
    KEY idx_lookup (platform, category, cost_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='平台费用规则版本（双时态，只追加）';

CREATE TABLE IF NOT EXISTS freight_rate_version
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    rate_key    VARCHAR(128) NOT NULL COMMENT '运价卡逻辑标识',
    version_seq INT          NOT NULL,
    carrier     VARCHAR(32)  NOT NULL COMMENT 'UPS_GROUND/YAMATO_TAKKYUUBIN/SHOPEE_SLS',
    market      VARCHAR(16)  NOT NULL COMMENT 'US/JP/SEA',
    currency    CHAR(3)      NOT NULL,
    params_json JSON         NOT NULL COMMENT 'dimDivisor / roundStepGrams 等',
    valid_from  DATETIME     NOT NULL,
    valid_to    DATETIME     NULL,
    recorded_at DATETIME     NOT NULL,
    remark      VARCHAR(512) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_key_seq (rate_key, version_seq),
    UNIQUE KEY uk_key_recorded (rate_key, recorded_at),
    KEY idx_resolve (rate_key, valid_from, valid_to, recorded_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='运费费率卡版本（双时态）';

CREATE TABLE IF NOT EXISTS us_zone_rate
(
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    rate_version_id  BIGINT      NOT NULL COMMENT '所属 freight_rate_version.id',
    zone             TINYINT     NOT NULL COMMENT '1-8',
    service_level    VARCHAR(32) NOT NULL COMMENT 'GROUND/3DAY/2DAY',
    weight_from_gram BIGINT      NOT NULL COMMENT '含',
    weight_to_gram   BIGINT      NOT NULL COMMENT '不含',
    unit_price_minor BIGINT      NOT NULL COMMENT '每公斤单价，币种最小单位',
    currency         CHAR(3)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_lookup (rate_version_id, zone, service_level, weight_from_gram, weight_to_gram)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='美国分区阶梯单价';

CREATE TABLE IF NOT EXISTS zone_map
(
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    rate_version_id BIGINT   NOT NULL,
    zip_prefix_from CHAR(5)  NOT NULL COMMENT '定长字符串，保住前导零',
    zip_prefix_to   CHAR(5)  NOT NULL,
    zone            TINYINT  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_lookup (rate_version_id, zip_prefix_from, zip_prefix_to)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='邮编前缀区间 → Zone';

CREATE TABLE IF NOT EXISTS jp_size_rate
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    rate_version_id BIGINT      NOT NULL,
    area            VARCHAR(16) NOT NULL COMMENT '地帯：HOKKAIDO/KANTO/KANSAI/KYUSHU...',
    size_band       VARCHAR(8)  NOT NULL COMMENT 'S60/S80/S100/S120/S140/S160/S180',
    girth_cm_max    INT         NOT NULL COMMENT '三边和上限（含）',
    weight_cap_gram BIGINT      NOT NULL COMMENT '该档重量上限',
    price_minor     BIGINT      NOT NULL,
    currency        CHAR(3)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lookup (rate_version_id, area, size_band)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='日本 Size 档价格';

CREATE TABLE IF NOT EXISTS sea_first_add_rate
(
    id                       BIGINT   NOT NULL AUTO_INCREMENT,
    rate_version_id          BIGINT   NOT NULL,
    country                  VARCHAR(8) NOT NULL COMMENT 'MY/TH/ID',
    first_weight_gram        BIGINT   NOT NULL COMMENT '首重，如 500',
    first_price_minor        BIGINT   NOT NULL,
    continuation_unit_gram   BIGINT   NOT NULL COMMENT '续重单位，如 100',
    continuation_price_minor BIGINT   NOT NULL,
    currency                 CHAR(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lookup (rate_version_id, country)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='东南亚首重续重价';

CREATE TABLE IF NOT EXISTS order_cost_sheet
(
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    sheet_id         VARCHAR(40) NOT NULL COMMENT '业务单号',
    order_no         VARCHAR(64) NOT NULL,
    platform         VARCHAR(32) NOT NULL,
    revenue_minor    BIGINT      NOT NULL COMMENT '售价收入',
    total_cost_minor BIGINT      NOT NULL COMMENT '成本合计',
    currency         CHAR(3)     NOT NULL,
    status           VARCHAR(16) NOT NULL COMMENT 'DRAFT/CONFIRMED/SUPERSEDED',
    recalc_batch_id  VARCHAR(40) NULL COMMENT '重算产物标记',
    created_at       DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sheet (sheet_id),
    UNIQUE KEY uk_order_batch (order_no, recalc_batch_id),
    KEY idx_platform_time (platform, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单成本单';

CREATE TABLE IF NOT EXISTS cost_item_detail
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    sheet_id          VARCHAR(40) NOT NULL,
    cost_type         VARCHAR(32) NOT NULL COMMENT 'FREIGHT/COMMISSION/PAYMENT/STORAGE/ADVERTISING',
    rule_type         VARCHAR(32) NOT NULL,
    rule_key          VARCHAR(128) NULL,
    rule_version_seq  INT         NULL,
    base_amount_minor BIGINT      NULL COMMENT '计费基数',
    computed_minor    BIGINT      NOT NULL COMMENT '本项计算结果',
    currency          CHAR(3)     NOT NULL,
    trace_json        JSON        NULL COMMENT '中间过程，用于钻取与复算校验',
    PRIMARY KEY (id),
    KEY idx_sheet (sheet_id),
    KEY idx_type (cost_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='费用项明细';

CREATE TABLE IF NOT EXISTS recalc_batch
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    batch_id       VARCHAR(40) NOT NULL,
    mode           VARCHAR(24) NOT NULL COMMENT 'AS_OF_ORIGINAL/AS_OF_CURRENT',
    period_from    DATETIME    NOT NULL,
    period_to      DATETIME    NOT NULL,
    shard_total    INT         NOT NULL,
    shard_finished INT         NOT NULL DEFAULT 0,
    shard_failed   INT         NOT NULL DEFAULT 0,
    status         VARCHAR(16) NOT NULL COMMENT 'CREATED/RUNNING/FINISHED/FAILED/PARTIAL',
    diff_summary   JSON        NULL COMMENT '双口径差异汇总',
    started_at     DATETIME    NULL,
    finished_at    DATETIME    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch (batch_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='重算批次';

CREATE TABLE IF NOT EXISTS pricing_scenario
(
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    scenario_id        VARCHAR(40)   NOT NULL,
    platform           VARCHAR(32)   NOT NULL,
    category           VARCHAR(64)   NOT NULL,
    target_margin_rate DECIMAL(10, 8) NOT NULL COMMENT '目标毛利率',
    solved_price_minor BIGINT        NULL COMMENT '求得的售价',
    price_cap_minor    BIGINT        NULL COMMENT '核价上限',
    feasible           TINYINT(1)    NOT NULL COMMENT '0 不可行',
    gap_minor          BIGINT        NULL COMMENT '不可行时的缺口金额',
    input_json         JSON          NOT NULL COMMENT '输入参数快照',
    trace_json         JSON          NULL COMMENT '分段枚举与求解过程',
    created_at         DATETIME      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_scenario (scenario_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='定价测算场景';