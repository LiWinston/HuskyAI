# 第一阶段：使用 Maven 构建 Spring Boot 后端
# Stage 1: Build the Spring Boot backend using Maven
FROM maven:3.9.9-amazoncorretto-21 AS backend-build

# 设置工作目录为 /app/backend
# Set the working directory to /app/backend
WORKDIR /app/backend

# 将后端的 pom.xml 文件拷贝到工作目录中以安装依赖
# Copy the pom.xml file to install dependencies
COPY demo/pom.xml .

# 预先下载后端项目所需的依赖库，利用 Docker 缓存来加速后续构建
# Pre-download backend dependencies to leverage Docker caching
RUN mvn dependency:go-offline

# 将后端应用代码拷贝到工作目录中
# Copy backend application source code to the working directory
COPY demo/src ./src

# 使用 Maven 构建后端应用程序，跳过测试
# Build the backend application using Maven, skipping tests
RUN mvn clean package -DskipTests

# 第二阶段：使用 Node.js 构建 React 前端
# Stage 2: Build the React frontend using Node.js
FROM node:18 AS frontend-build

# 设置工作目录为 /app/frontend
# Set the working directory to /app/frontend
WORKDIR /app/frontend

# 将前端的 package.json 和 package-lock.json 拷贝到工作目录中
# Copy the package.json and package-lock.json files to install dependencies
COPY gpt-clone/package.json gpt-clone/package-lock.json ./

# 安装前端项目的依赖
# Install frontend dependencies
RUN npm install

# 将所有前端应用代码拷贝到工作目录中
# Copy all frontend source code to the working directory
COPY gpt-clone/ .

# 构建前端应用
# Build the frontend application
RUN npm run build

# 第三阶段：运行时镜像，使用 Nginx 和 OpenJDK
FROM nginx:latest AS nginx

# 将前端生成的静态文件复制到 Nginx 的 web 根目录
COPY --from=frontend-build /app/frontend/build /usr/share/nginx/html

# 复制 Nginx 配置文件
COPY nginx.conf /etc/nginx/nginx.conf

RUN nginx -V && nginx -t

# 使用 OpenJDK 镜像运行 Spring Boot 应用
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app
COPY --from=backend-build /app/backend/target/*.jar app.jar

# 更新包索引并安装 Elasticsearch
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app
COPY --from=backend-build /app/backend/target/*.jar app.jar

# 更新包索引并安装 Elasticsearch
RUN apt-get update && apt-get install -y wget gnupg openjdk-11-jre-headless supervisor && \
    wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-7.17.9-amd64.deb && \
    dpkg -i elasticsearch-7.17.9-amd64.deb && \
    rm elasticsearch-7.17.9-amd64.deb && \
    mkdir -p /etc/elasticsearch && \
    echo "xpack.security.enabled: false" >> /etc/elasticsearch/elasticsearch.yml && \
    echo "discovery.type: single-node" >> /etc/elasticsearch/elasticsearch.yml && \
    chown -R elasticsearch:elasticsearch /usr/share/elasticsearch && \
    chown -R elasticsearch:elasticsearch /var/lib/elasticsearch && \
    chown -R elasticsearch:elasticsearch /etc/elasticsearch

# 设置系统限制
RUN ulimit -n 65535

# 设置 Elasticsearch 环境变量
ENV ES_JAVA_OPTS="-Xms512m -Xmx512m"
# 复制 supervisord 配置文件
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# 暴露端口
EXPOSE 80
EXPOSE 8090
EXPOSE 9200
EXPOSE 9300

# 使用 supervisord 启动 Nginx 和 Spring Boot
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]





#Instructions for building the Docker image and running the container
# docker build -t lms-gpt .
# docker run -p 80:80 -p 8090:8090 lms-gpt

#For (ngrok local 80 run) situation where service, redis are running both in docker, redis can not be accessed without adding them to the same network
# docker network create testnet
# docker run -it --name redis-test --network testnet --network-alias redis redis
# (With host properties already been set "redis")docker run -it --name LG --network testnet --network-alias svc -p 80:80 -p 8090:8090 lms-gpt
# docker run -it --name LG --network testnet --network-alias svc -p 80:80 -p 8090:8090 lms-gpt --spring.data.redis.host=redis
# docker run -it --name LG --network testnet --network-alias svc -p 80:80 -p 8090:8090 -e SPRING_DATA_REDIS_HOST=redis lms-gpt
