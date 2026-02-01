# 使用主流维护的 OpenJDK 21 运行时镜像 (JRE 比 JDK 更小，够跑应用了)
# eclipse-temurin 是目前推荐的 OpenJDK 发行版
FROM eclipse-temurin:21-jre

# 设置工作目录
WORKDIR /app

# 复制 Maven 构建后的 jar 文件到容器中
COPY think-do-start/target/think-do-start-1.0-SNAPSHOT.jar app.jar

# 暴露 Spring Boot 默认端口
EXPOSE 8091

# 设置 JVM 参数 -XX:+UseContainerSupport 确保 JVM 感知 Docker 内存限制
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseContainerSupport"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]