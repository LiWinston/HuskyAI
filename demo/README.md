# Build and run the demo
## Normal—JDK environment required
```
(from root directory)
cd demo
mvn clean package
java -jar target/ChatBot-0.0.1-SNAPSHOT.jar
```

## Docker—Docker environment required
```
(from root directory)
docker build -t myapp .
```
To start the container and map port 8080 of the container to port 8080 on your host machine, use the following command:
```
docker run -d -p 8080:8080 --name myapp-container myapp
```
Explanation:
###### -d: Run the container in detached mode \(i.e., in the background\).
###### 
###### -p 8080:8080: Map port 8080 of the container to port 8080 on the host machine.
###### 
###### --name myapp-container: Assign a name to the container for easier management.
###### 
###### myapp: The name of the image you built.


# HOW TO SET API?
## DOUBAO API
in demo/src/main/resources/application.properties:
get your ak, sk from doubao, and set them in application.properties -- volc.ak, volc.sk or just follow the instruction
```
https://console.volcengine.com/ark/region:ark+cn-beijing/model?projectName=default
```

## OpenAI API
get your api key from openai, and set it in application.properties -- openai.api.key
```
https://platform.openai.com/settings/profile?tab=api-keys
```





# Tip
- sometimes people do force push, which leads to the local branch out of sync with the remote branch. To fix this, you can run the following commands:
  ```bash
  git fetch origin
  git reset --hard origin/main
  ```
  This will reset your local branch to the remote branch's state. Be careful with this command as it will discard all your local changes.
  (After all, force push is not recommended as a good practice)

## Set up local redis
docker way:
```
docker pull redis
docker run -d -p 6379:6379 --name redis redis
```
wsl way(windows):
```
brew install redis
redis-server
brew services start redis
```


## useful Remote redis CLI:
refers to https://redis.io/docs/latest/operate/oss_and_stack/install/install-redis/  to get cli environment
```
REDISCLI_AUTH=rP0r0Fl6GqEXn5ZsiBdRtiG75iarhG7M redis-cli --user red-cr87h9rtq21c739lu2n0 -h singapore-redis.render.com -p 6379 --tls
```
if not working, try to set SysVar manually like:
(for linux)
```
nano ~/.bashrc
export REDISCLI_AUTH=rP0r0Fl6GqEXn5ZsiBdRtiG75iarhG7M
source ~/.bashrc
```
(for Mac)
```
nano ~/.zshrc
export REDISCLI_AUTH=rP0r0Fl6GqEXn5ZsiBdRtiG75iarhG7M
source ~/.zshrc
```

(To save the config above, Ctrl + O, then Enter, then Ctrl + X)
then run the command above again
To check the chat history:
```
LRANGE chat:history:default_baidu_conversation 0 -1
```

To clear the chat history:
```
Flushall
```


你可以通过配置 ssh-agent 来避免每次都输入 SSH 密钥的密码。以下是步骤：

1. 启动 ssh-agent
```bash
eval "$(ssh-agent -s)"
```

2. 添加 SSH 私钥到 ssh-agent
```bash
ssh-add ~/.ssh/id_rsa
```

3. 验证是否添加成功
```bash
ssh-add -l
```

4. 如果你的 SSH 私钥不是默认的 `id_rsa`，请将 `~/.ssh/id_rsa` 替换为你的私钥路径。
5. 如果你的 SSH 密钥有密码，你需要输入密码来添加 SSH 密钥到 ssh-agent。
6. 如果你的 SSH 密钥没有密码，你可以直接添加 SSH 密钥到 ssh-agent。

自动启动 ssh-agent（可选）
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_rsa



# Tip
echo $SHELL
2. 编辑 Shell 配置文件
   打开你的配置文件，使用 nano 或其他编辑器：

对于 bash：

bash
复制代码
nano ~/.bash_profile
对于 zsh：

bash
复制代码
nano ~/.zshrc
3. 添加自动启动 ssh-agent 和加载密钥的命令
   在文件中添加以下内容：

bash
复制代码
# 启动 ssh-agent 并添加密钥
if [ -z "$SSH_AUTH_SOCK" ]; then
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_rsa
fi
4. 保存并退出
   使用 Ctrl + O 保存文件。
   使用 Ctrl + X 退出编辑器。
5. 使配置文件生效
   在终端中运行以下命令以使更改立即生效：

bash
复制代码
source ~/.bash_profile  # 对于 bash
source ~/.zshrc         # 对于 zsh


chmod +x *.sh
