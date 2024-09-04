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
