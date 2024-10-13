
DOCKER_USERNAME="yongchunl"  # 替换为你的 Docker Hub 用户名
IMAGE_NAME="lms-gpt"
TAG="latest"  # 可以根据需要修改标签，例如使用版本号

# 检查redis的镜像和容器检查逻辑
echo "Checking Redis instance..."
if docker ps | grep -q "redis-test"; then
  echo "Redis is already running."
  if docker inspect redis-test | grep -q '"NetworkMode": "testnet"'; then
    echo "Redis is on the correct network (testnet)."
  else
    echo "Redis is not on testnet, restarting..."
    docker stop redis-test
    docker rm redis-test
    docker network create testnet 2>/dev/null
    docker run -d --name redis-test --network testnet --network-alias redis redis
  fi
else
  echo "No Redis instance found, starting Redis..."
  docker network create testnet 2>/dev/null
  docker run -d --name redis-test --network testnet --network-alias redis -p 6379:6379 redis
fi

# 检查本地是否已有LMS-GPT镜像
echo "Checking for local LMS-GPT image..."
if docker images | grep -q "${DOCKER_USERNAME}/${IMAGE_NAME}"; then
  echo "LMS-GPT image already exists locally."
  LOCAL_IMAGE_EXISTS=true
else
  echo "No local LMS-GPT image found. Pulling from Docker Hub..."
  LOCAL_IMAGE_EXISTS=false
fi

# 如果没有本地镜像或需要拉取更新镜像，拉取新镜像
if [ "$LOCAL_IMAGE_EXISTS" = false ]; then
  echo "Pulling the LMS-GPT image from Docker Hub..."
  docker pull ${DOCKER_USERNAME}/${IMAGE_NAME}:${TAG}
  if [ $? -ne 0 ]; then
    echo "Failed to pull the image from Docker Hub. Exiting..."
    exit 1
  fi
fi

# 检查并启动LG容器
echo "Checking for existing LG container..."
if docker ps | grep -q "LG"; then
  echo "LG container already running, stopping and removing..."
  docker stop LG
  docker rm LG
fi

# 启动新的LG容器并将其添加到testnet网络中
echo "Starting LG container..."
docker run -d --name LG --network testnet --network-alias svc -p 80:80 -p 8090:8090 -e SPRING_DATA_REDIS_HOST=redis ${DOCKER_USERNAME}/${IMAGE_NAME}:${TAG}

if [ $? -eq 0 ]; then
  echo "LG container started successfully."
else
  echo "Failed to start LG container."
  docker logs LG  # 查看容器日志
  exit 1
fi

echo "Deployment successful!"
