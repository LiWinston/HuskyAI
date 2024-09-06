-- DROP DATABASE IF EXISTS 并重新创建数据库
DROP DATABASE IF EXISTS chatbot_db;
CREATE DATABASE chatbot_db;

-- 切换到新创建的数据库
\c chatbot_db;

-- 删除现有表（如果存在）
DROP TABLE IF EXISTS UserPw, Cid, UidCid;

-- 创建用户表 UserPw
CREATE TABLE UserPw (
                        uuid VARCHAR(36) PRIMARY KEY,
                        username VARCHAR(50) NOT NULL,
                        password VARCHAR(50) NOT NULL
);

-- 创建对话表 Cid，包含带时区的时间戳字段
CREATE TABLE Cid (
                     conversationId VARCHAR(36) PRIMARY KEY,
                     firstMessage TEXT,
                     createdAt TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,  -- 对话创建时间
                     lastMessageAt TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP  -- 最后消息时间
);

-- 创建用户与对话的关联表 UidCid
CREATE TABLE UidCid (
                        uuid VARCHAR(36),
                        conversationId VARCHAR(36),
                        PRIMARY KEY (uuid, conversationId),
                        FOREIGN KEY (uuid) REFERENCES UserPw(uuid) ON DELETE CASCADE,
                        FOREIGN KEY (conversationId) REFERENCES Cid(conversationId) ON DELETE CASCADE
);

-- 初始化插入一些测试数据
INSERT INTO UserPw (uuid, username, password) VALUES
                                                  ('1111', 'user1', 'password1'),
                                                  ('2222', 'user2', 'password2');

INSERT INTO Cid (conversationId, firstMessage, createdAt, lastMessageAt) VALUES
                                                                             ('default1_1', 'none', NOW(), NOW()),
                                                                             ('default1_2', 'none', NOW(), NOW()),
                                                                             ('default2_1', 'none', NOW(), NOW()),
                                                                             ('default2_2', 'none', NOW(), NOW());

INSERT INTO UidCid (uuid, conversationId) VALUES
                                              ('1111', 'default1_1'),
                                              ('1111', 'default1_2'),
                                              ('2222', 'default2_1'),
                                              ('2222', 'default2_2');
