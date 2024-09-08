-- DROP DATABASE IF EXISTS 并重新创建数据库
DROP DATABASE IF EXISTS chatbot_db;
CREATE DATABASE chatbot_db;

-- 切换到新创建的数据库
\c chatbot_db;



DROP TABLE IF EXISTS UserPw CASCADE;
-- 创建用户表 UserPw
CREATE TABLE UserPw (
                        uuid VARCHAR(36) PRIMARY KEY,
                        username VARCHAR(50) NOT NULL,
                        password VARCHAR(100) NOT NULL,  -- 密码建议使用加密后的哈希值
                        role VARCHAR(20) DEFAULT 'USER'   -- 用于区分不同权限的用户
);

DROP TABLE IF EXISTS AdminInfo CASCADE;
CREATE TABLE AdminInfo(
                        uuid VARCHAR(36) PRIMARY KEY,
                        admin_level SMALLINT DEFAULT 0,
                        email VARCHAR(50) NOT NULL,
                        verified BOOLEAN DEFAULT FALSE,
                        FOREIGN KEY (uuid) REFERENCES UserPw(uuid) ON DELETE CASCADE
);

DROP TABLE IF EXISTS Cid CASCADE;
-- 创建对话表 Cid，包含带时区的时间戳字段
CREATE TABLE Cid (
                     conversationId VARCHAR(36) PRIMARY KEY,
                     firstMessage TEXT,
                     createdAt TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,  -- 对话创建时间
                     lastMessageAt TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP  -- 最后消息时间
);

DROP TABLE IF EXISTS UidCid CASCADE;
-- 创建用户与对话的关联表 UidCid
CREATE TABLE UidCid (
                        uuid VARCHAR(36),
                        conversationId VARCHAR(36),
                        PRIMARY KEY (uuid, conversationId),
                        FOREIGN KEY (uuid) REFERENCES UserPw(uuid) ON DELETE CASCADE,
                        FOREIGN KEY (conversationId) REFERENCES Cid(conversationId) ON DELETE CASCADE
);

-- -- 初始化插入一些测试数据
-- INSERT INTO UserPw (uuid, username, password, role) VALUES
--                                                   ('1111', 'user1', 'password1', 'ADMIN'),
--                                                   ('2222', 'user2', 'password2', 'USER');

-- INSERT INTO Cid (conversationId, firstMessage, createdAt, lastMessageAt) VALUES
--                                                                              ('default1_1', 'none', NOW(), NOW()),
--                                                                              ('default1_2', 'none', NOW(), NOW()),
--                                                                              ('default2_1', 'none', NOW(), NOW()),
--                                                                              ('default2_2', 'none', NOW(), NOW());
--
-- INSERT INTO UidCid (uuid, conversationId) VALUES
--                                               ('1111', 'default1_1'),
--                                               ('1111', 'default1_2'),
--                                               ('2222', 'default2_1'),
--                                               ('2222', 'default2_2');
