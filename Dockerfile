# 后面在服务器中安装了docker之后,执行`docker build -t partner-match-backend:v0.0.1`之后就会自动识别Dockerfile文件并执行下面的命令
# FROM表示我们这个docker镜像依赖于这个基础镜像,自动打包了maven3.5和java,这样就不用自己去安装maven和java了
FROM maven:3.5-jdk-8-alpine as builder

# WORKDIR表示工作目录,以后所有的代码都放在线上的这个工作目录中
WORKDIR /app

# 把本地要制作镜像的代码复制到这个容器中,把pom.xml复制到容器的当前目录中.和src源码复制到容器的当前目录的src文件夹中
COPY pom.xml .
COPY src ./src

# 执行maven的打包命令去打包就可以了
RUN mvn package -DskipTests

# 上面这些操作已经制作好镜像了,下面这条命令是之后要运行镜像的时候要执行的命令,以后可以对他进行覆盖,CMD(参数写死的)/ENTRYPOINT(可以附加额外参数)
CMD ["java","-jar","/app/target/partner-match-backend-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]