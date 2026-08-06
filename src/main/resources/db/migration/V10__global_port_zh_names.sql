-- 业务港口中文名补全（按 UN/LOCODE 精确匹配；英文名保持 UNECE 原样）
-- 来源：业务港口中英对照表（已清洗左侧英文中的中文/符号，并映射到标准港口）

UPDATE md_global_port SET name_zh = '上海' WHERE code = 'CNSGH' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '宁波' WHERE code = 'CNNBO' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '厦门' WHERE code = 'CNXAM' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '青岛' WHERE code = 'CNQIN' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '蛇口' WHERE code = 'CNSHK' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '巴生' WHERE code = 'MYPKG' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '巴生西' WHERE code = 'MYWSP' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '巴生北' WHERE code = 'MYLPK' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '马尼拉北' WHERE code = 'PHMNN' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '海防' WHERE code = 'VNHPH' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '林查班' WHERE code = 'THLCH' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '胡志明' WHERE code = 'VNSGN' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '勿拉湾' WHERE code = 'IDBLW' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '槟城' WHERE code = 'MYPEN' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '新加坡' WHERE code = 'SGSIN' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '泗水' WHERE code = 'IDSUB' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '雅加达' WHERE code = 'IDJKT' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '三宝垄' WHERE code = 'IDSRG' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '西哈努克' WHERE code = 'KHKOS' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '曼谷' WHERE code = 'THBKK' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '老沃' WHERE code = 'PHLAO' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '宿务' WHERE code = 'PHCEB' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '达沃' WHERE code = 'PHDVO' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '桑托斯将军城' WHERE code = 'PHGES' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '卡莱' WHERE code = 'VNCLI' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '岘港' WHERE code = 'VNDAD' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '归仁' WHERE code = 'VNUIH' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '金边' WHERE code = 'KHPNH' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '拉克拉邦' WHERE code = 'THLKR' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '宋卡' WHERE code = 'THSGK' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '摩拉' WHERE code = 'BNMUA' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '巴西古当' WHERE code = 'MYPGU' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '古晋' WHERE code = 'MYKCH' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '亚庇' WHERE code = 'MYBKI' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '民都鲁' WHERE code = 'MYBTU' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '丹戎帕拉帕斯' WHERE code = 'MYTPP' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '东京' WHERE code = 'JPTYO' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '横滨' WHERE code = 'JPYOK' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '名古屋' WHERE code = 'JPNGO' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '大阪' WHERE code = 'JPOSA' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '神户' WHERE code = 'JPUKB' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '门司' WHERE code = 'JPMOJ' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '博多' WHERE code = 'JPHKT' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '釜山' WHERE code = 'KRPUS' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '仁川' WHERE code = 'KRINC' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '基隆' WHERE code = 'TWKEL' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '高雄' WHERE code = 'TWKHH' AND (name_zh IS NULL OR name_zh = '');
UPDATE md_global_port SET name_zh = '台中' WHERE code = 'TWTXG' AND (name_zh IS NULL OR name_zh = '');

-- 马尼拉主港一并补中文（对照表有马尼拉北；主港常用）
UPDATE md_global_port SET name_zh = '马尼拉' WHERE code = 'PHMNL' AND (name_zh IS NULL OR name_zh = '');
