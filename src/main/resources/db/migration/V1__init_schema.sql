-- 福瑞多报价系统 PostgreSQL 全量建表（Flyway V1）
-- 重建库：先执行 scripts/reset-postgres.sql，再重启服务

-- ========== 系统 ==========
CREATE TABLE sys_department (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(32)  NOT NULL UNIQUE,
    name       VARCHAR(64)  NOT NULL,
    parent_id  BIGINT       NOT NULL DEFAULT 0,
    sort       INTEGER      NOT NULL DEFAULT 0,
    status     INTEGER      NOT NULL DEFAULT 1
);

CREATE TABLE sys_permission (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(128) NOT NULL UNIQUE,
    name       VARCHAR(128) NOT NULL,
    type       VARCHAR(16)  NOT NULL,
    parent_id  BIGINT       NOT NULL DEFAULT 0,
    sort       INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE sys_role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(64)  NOT NULL,
    data_scope  VARCHAR(16)  NOT NULL,
    status      INTEGER      NOT NULL DEFAULT 1,
    remark      VARCHAR(255)
);

CREATE TABLE sys_role_permission (
    role_id       BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES sys_permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE sys_user (
    id                   BIGSERIAL PRIMARY KEY,
    username             VARCHAR(64)   NOT NULL UNIQUE,
    password_hash        VARCHAR(128)  NOT NULL,
    password_strength    INTEGER,
    password_updated_at  TIMESTAMP,
    real_name            VARCHAR(64)   NOT NULL,
    avatar               VARCHAR(512),
    phone                VARCHAR(20),
    email                VARCHAR(128),
    home_path            VARCHAR(128)  DEFAULT '/workspace',
    status               INTEGER       NOT NULL DEFAULT 1,
    dept_id              BIGINT        NOT NULL REFERENCES sys_department(id)
);

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES sys_role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sys_operation_log (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT,
    username       VARCHAR(64),
    real_name      VARCHAR(64),
    module         VARCHAR(64),
    action         VARCHAR(16)  NOT NULL,
    resource_type  VARCHAR(64),
    resource_id    VARCHAR(64),
    summary        VARCHAR(256),
    request_method VARCHAR(8),
    request_uri    VARCHAR(256),
    request_body   TEXT,
    ip_address     VARCHAR(64),
    success        BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message  VARCHAR(512),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========== 币种 / 汇率 ==========
CREATE TABLE currency (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(8)   NOT NULL UNIQUE,
    name           VARCHAR(64)  NOT NULL,
    symbol         VARCHAR(8),
    decimal_places INTEGER      NOT NULL DEFAULT 2,
    is_base        BOOLEAN      NOT NULL DEFAULT FALSE,
    sort           INTEGER      NOT NULL DEFAULT 0,
    status         INTEGER      NOT NULL DEFAULT 1,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exchange_rate (
    id              BIGSERIAL PRIMARY KEY,
    from_currency   VARCHAR(8)     NOT NULL,
    to_currency     VARCHAR(8)     NOT NULL,
    rate            NUMERIC(18, 8) NOT NULL,
    effective_date  DATE           NOT NULL,
    status          INTEGER        NOT NULL DEFAULT 1,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_exchange_rate_pair_date UNIQUE (from_currency, to_currency, effective_date)
);

-- ========== 客户 ==========
CREATE TABLE customer (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(32)  NOT NULL UNIQUE,
    name             VARCHAR(128) NOT NULL,
    contact_name     VARCHAR(64),
    phone            VARCHAR(32),
    email            VARCHAR(128),
    address          VARCHAR(256),
    remark           VARCHAR(512),
    status           INTEGER      NOT NULL DEFAULT 1,
    created_by       BIGINT,
    created_by_name  VARCHAR(64),
    dept_id          BIGINT,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========== 主数据 ==========
CREATE TABLE md_us_state (
    id       BIGSERIAL PRIMARY KEY,
    code     VARCHAR(8)  NOT NULL UNIQUE,
    name_zh  VARCHAR(64) NOT NULL
);

CREATE TABLE md_dest_city (
    id        BIGSERIAL PRIMARY KEY,
    state_id  BIGINT       NOT NULL REFERENCES md_us_state(id),
    name      VARCHAR(128) NOT NULL
);

CREATE TABLE md_dest_zip (
    id        BIGSERIAL PRIMARY KEY,
    city_id   BIGINT      NOT NULL REFERENCES md_dest_city(id),
    zip_code  VARCHAR(32) NOT NULL
);

CREATE INDEX idx_md_dest_city_state ON md_dest_city(state_id);
CREATE INDEX idx_md_dest_zip_city ON md_dest_zip(city_id);
CREATE INDEX idx_md_dest_zip_code ON md_dest_zip(zip_code);

CREATE TABLE md_global_port (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(8)   NOT NULL UNIQUE,
    name_en         VARCHAR(128) NOT NULL,
    name_zh         VARCHAR(128),
    route           VARCHAR(64),
    country_region  VARCHAR(128),
    port_type       VARCHAR(16),
    function_code   VARCHAR(16),
    locode_status   VARCHAR(8),
    data_version    VARCHAR(32)
);

CREATE TABLE md_inland_por (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(128) NOT NULL,
    pol_id  BIGINT       NOT NULL REFERENCES md_global_port(id),
    region  VARCHAR(64)
);

CREATE INDEX idx_md_inland_por_pol ON md_inland_por(pol_id);

CREATE TABLE md_data_sync_meta (
    id              BIGSERIAL PRIMARY KEY,
    sync_key        VARCHAR(64) NOT NULL UNIQUE,
    data_version    VARCHAR(32),
    last_sync_at    TIMESTAMP,
    total_records   INTEGER,
    inserted_count  INTEGER,
    updated_count   INTEGER,
    remark          VARCHAR(512)
);

-- ========== 成本库 ==========
CREATE TABLE cost_road (
    id                      BIGSERIAL PRIMARY KEY,
    valid_date              VARCHAR(32),
    supplier                VARCHAR(128),
    log_yard_name_address   VARCHAR(512),
    city                    VARCHAR(64),
    state                   VARCHAR(32),
    por                     VARCHAR(64),
    pol                     VARCHAR(64),
    base_freight            NUMERIC(14, 2),
    fsc                     NUMERIC(8, 4),
    chassis                 NUMERIC(14, 2),
    ow_tri_axle             NUMERIC(14, 2),
    split                   NUMERIC(14, 2),
    stop_off                NUMERIC(14, 2),
    all_in                  NUMERIC(14, 2),
    all_in_non_oak          NUMERIC(14, 2),
    all_in_oak              NUMERIC(14, 2),
    waiting_fee             NUMERIC(14, 2),
    redelivery              NUMERIC(14, 2),
    prepull                 NUMERIC(14, 2),
    ns_lift                 NUMERIC(14, 2),
    remark                  VARCHAR(512),
    extra_fields            JSONB,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cost_sea (
    id                    BIGSERIAL PRIMARY KEY,
    origin                VARCHAR(128),
    destination           VARCHAR(128),
    carrier               VARCHAR(128),
    spec                  VARCHAR(32),
    unit                  VARCHAR(16),
    unit_price            NUMERIC(14, 2),
    buc                   NUMERIC(14, 2),
    surcharge_valid_date  DATE,
    all_in                NUMERIC(14, 2),
    valid_date            VARCHAR(64),
    currency              VARCHAR(8),
    valid_from            DATE,
    valid_to              DATE,
    status                VARCHAR(16) DEFAULT 'draft',
    remark                VARCHAR(512),
    extra_fields          JSONB,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cost_fumigation (
    id                  BIGSERIAL PRIMARY KEY,
    port                VARCHAR(64),
    station             VARCHAR(64),
    non_oak_outdoor     NUMERIC(14, 2),
    non_oak_indoor      NUMERIC(14, 2),
    non_oak_quote_summer VARCHAR(128),
    non_oak_quote_winter VARCHAR(128),
    oak_outdoor         NUMERIC(14, 2),
    oak_indoor          NUMERIC(14, 2),
    oak_quote_summer    VARCHAR(128),
    oak_quote_winter    VARCHAR(128),
    remark              VARCHAR(512),
    extra_fields        JSONB,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cost_table_template (
    id          BIGSERIAL PRIMARY KEY,
    mode        VARCHAR(16)  NOT NULL,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(128) NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    layout      JSONB        NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ========== 报价单 ==========
CREATE TABLE quote_order (
    id                    BIGSERIAL PRIMARY KEY,
    quote_no              VARCHAR(32)    NOT NULL UNIQUE,
    customer_id           BIGINT,
    customer_name         VARCHAR(128)   NOT NULL,
    transport_mode        VARCHAR(16)    NOT NULL,
    route_summary         VARCHAR(256),
    status                VARCHAR(16)    NOT NULL DEFAULT 'DRAFT',
    total_amount          NUMERIC(18, 2) NOT NULL DEFAULT 0,
    currency              VARCHAR(8)     NOT NULL DEFAULT 'CNY',
    base_currency         VARCHAR(8)     NOT NULL DEFAULT 'CNY',
    exchange_rate         NUMERIC(18, 8),
    valid_until           DATE,
    zip_code              VARCHAR(16),
    city                  VARCHAR(128),
    state                 VARCHAR(32),
    por                   VARCHAR(64),
    pol                   VARCHAR(64),
    pod                   VARCHAR(64),
    o_f_usd               VARCHAR(128),
    ssl                   VARCHAR(128),
    trucking_non_oak_usd  NUMERIC(14, 2),
    trucking_oak_usd      NUMERIC(14, 2),
    fm_non_oak            NUMERIC(14, 2),
    fm_oak                NUMERIC(14, 2),
    doc_usd               VARCHAR(64),
    cargo_max_weight_ton  VARCHAR(128),
    sheet_remark          VARCHAR(1024),
    follow_up_by          BIGINT,
    follow_up_by_name     VARCHAR(64),
    remark                VARCHAR(512),
    created_by            BIGINT         NOT NULL,
    created_by_name       VARCHAR(64),
    dept_id               BIGINT,
    submitted_at          TIMESTAMP,
    approved_by           BIGINT,
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE quote_order_line (
    id           BIGSERIAL PRIMARY KEY,
    quote_id     BIGINT         NOT NULL REFERENCES quote_order(id) ON DELETE CASCADE,
    sort         INTEGER        NOT NULL DEFAULT 0,
    item_name    VARCHAR(128)   NOT NULL,
    spec         VARCHAR(256),
    cost_mode    VARCHAR(16)    NOT NULL DEFAULT 'MANUAL',
    cost_ref_id  BIGINT,
    quantity     NUMERIC(18, 4) NOT NULL DEFAULT 1,
    unit         VARCHAR(16),
    unit_price   NUMERIC(18, 4) NOT NULL DEFAULT 0,
    amount       NUMERIC(18, 2) NOT NULL DEFAULT 0,
    extra_json   JSONB
);

CREATE INDEX idx_quote_order_line_quote ON quote_order_line(quote_id);

CREATE TABLE quote_follow_up (
    id                BIGSERIAL PRIMARY KEY,
    quote_id          BIGINT        NOT NULL REFERENCES quote_order(id) ON DELETE CASCADE,
    follow_status     VARCHAR(32)   NOT NULL,
    content           VARCHAR(2000) NOT NULL,
    follow_up_by      BIGINT        NOT NULL,
    follow_up_by_name VARCHAR(64)   NOT NULL,
    follow_up_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quote_follow_up_quote ON quote_follow_up(quote_id);

CREATE TABLE quote_cost_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    quote_id        BIGINT       NOT NULL REFERENCES quote_order(id) ON DELETE CASCADE,
    cost_type       VARCHAR(16)  NOT NULL,
    cost_ref_id     BIGINT       NOT NULL,
    cost_version    VARCHAR(64),
    match_keys_json JSONB,
    snapshot_json   JSONB        NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quote_cost_snapshot_quote ON quote_cost_snapshot(quote_id);
