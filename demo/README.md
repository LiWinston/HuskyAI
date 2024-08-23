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