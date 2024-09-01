-- DROP DATABASE IF EXISTS chatbot_db;
-- CREATE DATABASE chatbot_db;
\c chatbot_db;
DROP TABLE IF EXISTS UserPw, Cid, UidCid;

-- 创建用户表 UserPw
CREATE TABLE UserPw (
                        uuid VARCHAR(36) PRIMARY KEY,
                        username VARCHAR(50) NOT NULL,
                        password VARCHAR(50) NOT NULL
);

-- 创建对话表 Cid
CREATE TABLE Cid (
                     conversationId VARCHAR(36) PRIMARY KEY,
                     firstMessage TEXT
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

INSERT INTO Cid (conversationId, firstMessage) VALUES
                                                   ('default_baidu_conversation', 'baidu chat'),
                                                   ('cid-456', 'Hi, I need some assistance.'),
                                                   ('default_openai_conversation', 'OpenAI不好使，谁有大能谁修吧: OpenAI is not working, whoever has the ability can fix it.');

INSERT INTO UidCid (uuid, conversationId) VALUES
                                              ('1111', 'default_baidu_conversation'),
                                              ('1111', 'cid-456'),
                                              ('1111', 'default_openai_conversation'),
                                              ('2222', 'cid-456');
