#!/bin/bash

# 设定阿里云容器镜像服务配置
ALIYUN_REGISTRY="crpi-5eorj2f9lgvhj7zf.cn-hangzhou.personal.cr.aliyuncs.com"  # 阿里云容器镜像服务地址
ALIYUN_USERNAME="winstonL"  # 阿里云账号
ALIYUN_NAMESPACE="yongchunl"  # 命名空间
IMAGE_NAME="lms-gpt"
TAG="latest"
RENDER_URL="https://api.render.com/deploy/srv-csoq9paj1k6c73am1bng?key=OzLWOo0zMFY"

# 检查 Docker 是否安装
if ! [ -x "$(command -v docker)" ]; then
  echo 'Error: Docker未安装，请先安装Docker后重试。' >&2
  exit 1
fi

# 检查是否在VPC网络环境
if ping -c 1 100.100.100.200 >/dev/null 2>&1; then
  echo "检测到VPC网络环境，使用VPC域名..."
  ALIYUN_REGISTRY="crpi-5eorj2f9lgvhj7zf-vpc.cn-hangzhou.personal.cr.aliyuncs.com"
fi

# 登录阿里云容器镜像服务
echo "正在登录阿里云容器镜像服务..."
docker login --username=${ALIYUN_USERNAME} ${ALIYUN_REGISTRY}
if [ $? -ne 0 ]; then
  echo "错误：阿里云容器镜像服务登录失败！请检查凭证后重试。" >&2
  exit 1
fi

# 检查并删除本地旧镜像
LOCAL_IMAGE="${ALIYUN_REGISTRY}/${ALIYUN_NAMESPACE}/${IMAGE_NAME}:${TAG}"
if docker images | grep -q "${ALIYUN_REGISTRY}/${ALIYUN_NAMESPACE}/${IMAGE_NAME}"; then
  echo "发现已存在的本地镜像，正在尝试删除..."
  docker image rm "${LOCAL_IMAGE}"

  if [ $? -ne 0 ]; then
    echo "警告：无法删除本地镜像，将使用新的标签..."
    NEW_TAG=$(date +%Y%m%d%H%M%S)
    echo "使用新标签: $NEW_TAG"
    TAG=$NEW_TAG
    LOCAL_IMAGE="${ALIYUN_REGISTRY}/${ALIYUN_NAMESPACE}/${IMAGE_NAME}:${TAG}"
  else
    echo "已成功删除旧镜像。"
  fi
else
  echo "未发现需要删除的本地镜像，继续执行..."
fi

# 构建 Docker 镜像
echo "正在构建Docker镜像: ${LOCAL_IMAGE}..."
docker build -t "${LOCAL_IMAGE}" .
if [ $? -ne 0 ]; then
  echo "错误：Docker镜像构建失败！请检查Dockerfile是否存在问题。" >&2
  exit 1
fi

# 推送镜像到阿里云容器镜像服务
echo "正在推送镜像到阿里云容器镜像服务..."
docker push "${LOCAL_IMAGE}"
if [ $? -ne 0 ]; then
  echo "错误：推送镜像到阿里云失败！请检查网络连接和权限设置。" >&2
  exit 1
fi

# # 触发 Render 部署
# echo "正在触发 Render 部署..."
# curl -X GET "$RENDER_URL"
# if [ $? -ne 0 ]; then
#   echo "错误：Render部署触发失败！请检查API密钥或部署URL。" >&2
#   exit 1
# fi

# 完成
echo "操作完成！"
echo "镜像已成功推送到阿里云容器镜像服务: ${LOCAL_IMAGE}" 