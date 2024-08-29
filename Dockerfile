# 第一阶段：使用 Maven 构建 Spring Boot 后端
# Stage 1: Build the Spring Boot backend using Maven
FROM maven:3.8.7-eclipse-temurin-17 AS backend-build

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

# 第三阶段：运行时镜像，使用 OpenJDK 和 Nginx
# Stage 3: Runtime image using OpenJDK and Nginx
FROM eclipse-temurin:17-jre AS runtime

# 设置应用的工作目录为 /app
# Set the application working directory to /app
WORKDIR /app

# 复制从后端构建阶段生成的 JAR 文件
# Copy the generated JAR file from the backend build stage
COPY --from=backend-build /app/backend/target/*.jar app.jar

# 创建目录以存放前端构建文件
# Create a directory to store frontend build files
RUN mkdir -p /app/frontend/build

# 复制从前端构建阶段生成的静态文件
# Copy the generated frontend build files from the frontend build stage
COPY --from=frontend-build /app/frontend/build /app/frontend/build

# 更新包索引并安装 Nginx
# Update package index and install Nginx
RUN apt-get update && apt-get install -y nginx

# 复制 Nginx 配置文件到正确的位置
# Copy the Nginx configuration file to the appropriate location
COPY nginx.conf /etc/nginx/nginx.conf

# 暴露 Nginx 和 Spring Boot 应用程序的端口
# Expose ports for Nginx and the Spring Boot application
EXPOSE 80
EXPOSE 8090

# 启动 Nginx 和 Spring Boot 应用程序
# Start Nginx and the Spring Boot application
CMD ["sh", "-c", "nginx && java -jar app.jar"]
# 指定 Spring Boot 运行在 8090 端口




#Instructions for building the Docker image and running the container
# docker build -t lms-gpt .
# docker run -p 80:80 -p 8090:8090 lms-gpt
