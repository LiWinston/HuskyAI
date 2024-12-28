#Run this script to build the docker image and push to hub, then trigger the render deploy
#Use windows shell, WSL, or git bash to run this script, may need chmod 777 buildPushDockerHub.sh on Mac
#Run this script in the root directory of the project

# 设定 Docker Hub 用户名和镜像名称
DOCKER_USERNAME="yongchunl"  # 替换为你的 Docker Hub 用户名
IMAGE_NAME="lms-gpt"
TAG="latest"  # 可以根据需要修改标签，例如使用版本号
#RENDER_URL="https://api.render.com/deploy/srv-cronat2j1k6c739ksph0?key=s-dNsYQvuP4"

# 添加暂停函数
pause() {
    echo
    echo "按任意键继续..."
    read -n 1 -s
}

# 错误处理函数
handle_error() {
    echo
    echo "遇到错误: $1"
    pause
    exit 1
}

# 检查 Docker 是否安装
if ! [ -x "$(command -v docker)" ]; then
    handle_error "Docker未安装，请先安装Docker后重试。"
fi

# 登录 Docker Hub
echo "正在登录 Docker Hub..."
if ! docker login; then
    handle_error "Docker Hub登录失败！请检查凭证后重试。"
fi

# 检查并删除本地旧镜像
if docker images | grep -q "$DOCKER_USERNAME/$IMAGE_NAME:$TAG"; then
    echo "发现已存在的本地镜像: $DOCKER_USERNAME/$IMAGE_NAME:$TAG，正在尝试删除..."
    if ! docker image rm "$DOCKER_USERNAME/$IMAGE_NAME:$TAG"; then
        echo "警告：无法删除本地镜像，将使用新的标签..."
        # 生成一个新标签（例如带时间戳的版本号）
        NEW_TAG=$(date +%Y%m%d%H%M%S)
        echo "使用新标签: $NEW_TAG"
        TAG=$NEW_TAG
    else
        echo "已成功删除旧镜像。"
    fi
else
    echo "未发现需要删除的本地镜像，继续执行..."
fi

# 构建 Docker 镜像
echo "正在构建Docker镜像: $DOCKER_USERNAME/$IMAGE_NAME:$TAG..."
if ! docker build -t "$DOCKER_USERNAME/$IMAGE_NAME:$TAG" .; then
    handle_error "Docker镜像构建失败！请检查Dockerfile是否存在问题。"
fi

# 推送 Docker 镜像到 Docker Hub
echo "正在推送镜像到Docker Hub..."
if ! docker push "$DOCKER_USERNAME/$IMAGE_NAME:$TAG"; then
    handle_error "推送镜像到Docker Hub失败！请确保已登录Docker Hub。"
fi

# 触发 Render 部署
if [ ! -z "$RENDER_URL" ]; then
    echo "正在触发Render部署..."
    if ! curl -X GET "$RENDER_URL"; then
        handle_error "Render部署触发失败！请检查API密钥或部署URL。"
    fi
fi

# 完成
echo
echo "操作完成！"
echo "镜像已成功推送到Docker Hub: $DOCKER_USERNAME/$IMAGE_NAME:$TAG"
pause
