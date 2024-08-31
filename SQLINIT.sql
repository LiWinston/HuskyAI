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
                                                   ('chat:history:default_baidu_conversation', 'YEEZY?'),
                                                   ('cid-456', 'Hi, I need some assistance.');

INSERT INTO UidCid (uuid, conversationId) VALUES
                                              ('1111-1111-1111-1111', 'chat:history:default_baidu_conversation'),
                                              ('1111-1111-1111-1111', 'cid-456'),
                                              ('2222-2222-2222-2222', 'cid-456');
