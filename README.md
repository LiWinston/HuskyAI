# HuskyAI - 多模型智能Chatbot

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0%2B-green.svg)
![React](https://img.shields.io/badge/React-18.0%2B-blue.svg)

HuskyAI是一个基于多模型聚合的智能对话系统，支持多种大语言模型的接入和管理，提供了完整的用户管理、对话历史、权限控制等功能。系统采用前后端分离架构，具有高可扩展性和灵活的部署选项。
##### 这md是Cursor自动生成的，多有胡说八道之处，不要当真。

## 🌟 主要特性

### 多模型支持
- 支持多种LLM模型的统一接入(OpenAI API兼容)
- 内置百度千帆、豆包等模型支持
- 动态模型注册与管理，支持运行时新增模型
- 模型健康检查和自动故障转移
- 多模型优先级队列，支持模型降级策略

### 用户系统
- 完整的用户注册、登录功能
- 基于JWT的无状态认证
- 细粒度的用户权限分级
- 基于IP的访问控制和频率限制
- 用户名智能推荐（Levenshtein距离算法）
- 邮箱验证和密码重置功能

### 对话功能
- 基于WebSocket的实时流式对话
- 完整的Markdown & LaTeX 公式渲染
- 支持40+编程语言的代码高亮
- 会话历史管理与检索
- 对话分享功能（支持加密分享）
- 会话自动保存与恢复

### 管理功能
- 响应式后台管理界面
- 用户管理（封禁、权限调整）
- 模型管理（添加、删除、配置）
- 对话历史查看与审计
- 系统监控与告警

## 🛠 技术栈

### 前端
- React 18 + React Router v6
- 状态管理：React Context
- UI动画：Framer Motion
- 数学公式：MathJax 3
- Markdown渲染：React Markdown + remark-gfm
- 代码高亮：react-syntax-highlighter（支持40+主题）
- HTTP请求：Axios + 请求拦截器
- WebSocket：原生WebSocket
- UI组件：
  - 自定义动画对话框
  - 响应式布局
  - 主题切换系统
  - 代码编辑器主题预览

### 后端
- Spring Boot 3.0
- 安全框架：Spring Security + JWT
- 数据访问：MyBatis + PageHelper
- 数据库：MySQL 8.0
- 缓存：Redis（可选，用于令牌和访问控制）
- 并发处理：
  - CompletableFuture异步处理
  - 自定义线程池
  - ConcurrentHashMap模型管理
- 功能特性：
  - ShedLock分布式锁
  - 全局异常处理
  - 统一响应封装
  - 请求参数校验
  - 跨域配置
  - Swagger API文档

## 📦 部署说明

### 环境要求
- JDK 17+
- Node.js 16+
- MySQL 8.0+
- Maven 3.6+
- Redis 6.0+（可选）

### 后端配置
1. 配置数据库
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis配置（可选）
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=your_redis_password

# JWT配置
jwt.secret=your_jwt_secret
jwt.expiration=86400000
```

2. 配置模型服务
```yaml
remote-services:
  service-configs:
    - url: "https://your-llm-service-1"
      name: "service1"
      apiKey: "your-api-key"
      allowedModels: 
        - "model1"
        - "model2"
      # 模型优先级配置
      priority: 1
      # 健康检查配置
      health-check:
        enabled: true
        interval: 60000
```

3. 编译运行
```bash
cd demo
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### 前端配置
1. 安装依赖
```bash
cd gpt-clone
npm install
```

2. 环境配置
```javascript
// .env.development
REACT_APP_API_BASE_URL=`http://localhost:8080`
REACT_APP_WS_URL=`ws://localhost:8080/chat/stream`
REACT_APP_ENABLE_MOCK=false

// .env.production
REACT_APP_API_BASE_URL=`http://your-production-api`
REACT_APP_WS_URL=`ws://your-production-ws`
REACT_APP_ENABLE_MOCK=false
```

3. 运行开发服务器
```bash
npm start
```

4. 生产环境构建
```bash
npm run build
```

## 🚀 待优化项目

1. 前端部分
- [ ] 移动端适配优化
- [ ] PWA支持
- [ ] 深色模式完善
- [ ] 对话导出（PDF/Markdown）
- [ ] 快捷命令支持
- [ ] 语音输入/输出
- [ ] 离线缓存优化

2. 后端部分
- [ ] 模型负载均衡
- [ ] 分布式部署支持
- [ ] 对话向量检索（Embedding）
- [ ] 更多模型接入
- [ ] 会话上下文优化
- [ ] 流量控制优化
- [ ] 性能监控系统

3. 功能扩展
- [ ] 插件系统
- [ ] 知识库接入
- [ ] 多轮对话优化
- [ ] 对话提示词管理
- [ ] 文件处理能力
- [ ] 多模态支持
- [ ] API网关集成

## 📄 开源协议

本项目采用 MIT 协议开源，详见 [LICENSE](LICENSE) 文件。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request 贡献代码。

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交改动 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📚 文档

详细文档请参考 [Wiki](../../wiki)

## 🙏 致谢

感谢所有贡献者对项目的支持！
