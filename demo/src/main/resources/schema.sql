-- 用户表
CREATE TABLE IF NOT EXISTS user_pw (
    uuid VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    sso_id VARCHAR(255) UNIQUE,  -- BitSleep SSO ID
    enabled BOOLEAN DEFAULT true
);

-- 用户角色表
CREATE TABLE IF NOT EXISTS user_role (
    uuid VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (uuid, role),
    FOREIGN KEY (uuid) REFERENCES user_pw(uuid)
); 