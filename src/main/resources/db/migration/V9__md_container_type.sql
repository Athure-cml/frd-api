-- 箱型主数据
CREATE TABLE IF NOT EXISTS md_container_type (
    id       BIGSERIAL PRIMARY KEY,
    code     VARCHAR(32)  NOT NULL UNIQUE,
    name     VARCHAR(64)  NOT NULL,
    sort     INTEGER      NOT NULL DEFAULT 0,
    status   INTEGER      NOT NULL DEFAULT 1,
    remark   VARCHAR(256)
);

INSERT INTO md_container_type (code, name, sort, status)
VALUES
    ('20GP', '20'' General Purpose', 10, 1),
    ('40GP', '40'' General Purpose', 20, 1),
    ('40HQ', '40'' High Cube', 30, 1),
    ('45HQ', '45'' High Cube', 40, 1),
    ('20RF', '20'' Reefer', 50, 1),
    ('40RF', '40'' Reefer', 60, 1),
    ('40NOR', '40'' Non-Operating Reefer', 70, 1),
    ('20OT', '20'' Open Top', 80, 1),
    ('40OT', '40'' Open Top', 90, 1),
    ('20FR', '20'' Flat Rack', 100, 1),
    ('40FR', '40'' Flat Rack', 110, 1)
ON CONFLICT (code) DO NOTHING;
