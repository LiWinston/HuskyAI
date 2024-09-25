#Run this script to build the docker image and push to hub, then trigger the render deploy
#Use windows shell, WSL, or git bash to run this script, may need chmod 777 deploy_Render.sh on Mac
#Run this script in the root directory of the project

# 设定 Docker Hub 用户名和镜像名称
DOCKER_USERNAME="yongchunl"  # 替换为你的 Docker Hub 用户名
IMAGE_NAME="lms-gpt"
TAG="latest"  # 可以根据需要修改标签，例如使用版本号
RENDER_URL="https://api.render.com/deploy/srv-cronat2j1k6c739ksph0?key=s-dNsYQvuP4"

# 检查 Docker 是否安装
if ! [ -x "$(command -v docker)" ]; then
  echo 'Error: Docker is not installed. Please install Docker and try again.' >&2
  exit 1
fi

# 登录 Docker Hub
echo "Logging in to Docker Hub..."
docker login
if [ $? -ne 0 ]; then
  echo "Error: Docker login failed! Please check your credentials and try again." >&2
  exit 1
fi

# 检查并删除本地旧镜像
if docker images | grep -q "$DOCKER_USERNAME/$IMAGE_NAME"; then
  echo "Found existing local image: $DOCKER_USERNAME/$IMAGE_NAME:$TAG. Removing it..."
  docker image rm "$DOCKER_USERNAME/$IMAGE_NAME:$TAG"
  if [ $? -ne 0 ]; then
    echo "Error: Failed to remove local image. Please try manually deleting the image." >&2
    exit 1
  fi
else
  echo "No local image to remove. Continuing..."
fi

# 构建 Docker 镜像
echo "Building Docker image: $DOCKER_USERNAME/$IMAGE_NAME:$TAG..."
docker build -t "$DOCKER_USERNAME/$IMAGE_NAME:$TAG" .
if [ $? -ne 0 ]; then
  echo "Error: Docker image build failed! Please check your Dockerfile for issues." >&2
  exit 1
fi

# 推送 Docker 镜像到 Docker Hub
echo "Pushing Docker image to Docker Hub..."
docker push "$DOCKER_USERNAME/$IMAGE_NAME:$TAG"
if [ $? -ne 0 ]; then
  echo "Error: Docker image push failed! Please ensure you are logged into Docker Hub." >&2
  exit 1
fi

# 触发 Render 部署
echo "Triggering Render deployment via URL: $RENDER_URL..."
curl -X GET "$RENDER_URL"
if [ $? -ne 0 ]; then
  echo "Error: Render deployment trigger failed! Please check the API key or deployment URL." >&2
  exit 1
fi

# 完成
echo "Docker image pushed successfully and Render deployment triggered successfully!"
