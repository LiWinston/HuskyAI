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

# 检查当前是否有正在运行的lms-gpt-backup容器
if docker ps | grep -q "lms-gpt-backup"; then
  echo "Stopping and removing existing backup container..."
  docker stop lms-gpt-backup
  docker rm lms-gpt-backup
fi

# 构建并启动备份服务的Docker镜像
echo "Building the LMS-GPT backup Docker image using LBDockerfile..."
docker build -f LBDockerfile -t lms-gpt-backup .

# 启动新的备份服务容器并将其添加到testnet网络中
echo "Starting the LMS-GPT backup container on testnet network..."
docker run -d --name lms-gpt-backup --network testnet --network-alias svc -p 8099:8090 -e SPRING_DATA_REDIS_HOST=redis lms-gpt-backup

# 部署完成
echo "Backup service is running on port 8099!"
