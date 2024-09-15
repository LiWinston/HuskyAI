#!/bin/bash

# 检查redis镜像是否存在，若不存在则拉取
if ! docker images | grep -q "redis"; then
  echo "Redis image not found, pulling the latest redis image..."
  docker pull redis
else
  echo "Redis image already exists."
fi

# 检查当前是否有redis实例在运行
if docker ps | grep -q "redis-test"; then
  # 检查redis是否在testnet网络上
  if docker inspect redis-test | grep -q '"NetworkMode": "testnet"'; then
    echo "Redis is already running on testnet network."
  else
    echo "Redis is running but not on testnet, restarting on the correct network..."
    docker stop redis-test
    docker rm redis-test
    docker network create testnet
    docker run -d --name redis-test --network testnet --network-alias redis redis
  fi
else
  echo "No Redis instance found, starting a new Redis container..."
  # 创建testnet网络并启动redis
  docker network create testnet
  docker run -d --name redis-test --network testnet --network-alias redis -p 6379:6379 redis
fi

# 检查当前是否有正在运行的LG容器
if docker ps | grep -q "LG"; then
  echo "Stopping and removing existing LG container..."
  docker stop LG
  docker rm LG
fi

# 构建并启动新镜像
echo "Building the LMS-GPT docker image..."
docker build -t lms-gpt .

# 启动新的LG容器并将其添加到testnet网络中
echo "Starting the LG container on testnet network..."
docker run -d --name LG --network testnet --network-alias svc -p 80:80 -p 8090:8090 -e SPRING_DATA_REDIS_HOST=redis lms-gpt

echo "Deployment successful!"
