# 使用 Maven 构建阶段
FROM maven:3.8.7-eclipse-temurin-17 AS build

# 设置工作目录
WORKDIR /app

# 将项目的 pom.xml 文件拷贝到容器中
COPY demo/pom.xml .

# 下载所有的依赖库，这样做可以利用 Docker 缓存
RUN mvn dependency:go-offline

# 将应用代码拷贝到容器中
COPY demo/src ./src

# 使用 Maven 构建应用程序
RUN mvn clean package

# 使用 OpenJDK 运行时镜像作为基础镜像
FROM eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 将构建的 Jar 文件从构建阶段拷贝到当前阶段
COPY --from=build /app/target/ChatBot-0.0.1-SNAPSHOT.jar .

# 暴露应用运行的端口
EXPOSE 8080

# 运行应用程序
CMD ["java", "-jar", "ChatBot-0.0.1-SNAPSHOT.jar"]
