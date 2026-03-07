# ===== build stage =====
ARG MAVEN_BASE_IMAGE=maven:3.9.9-eclipse-temurin-17
ARG JRE_BASE_IMAGE=eclipse-temurin:17-jre
FROM ${MAVEN_BASE_IMAGE} AS build
WORKDIR /workspace

# 先复制所有pom.xml，利用构建缓存
COPY pom.xml .
COPY jigu-api/pom.xml jigu-api/pom.xml
COPY jigu-application/pom.xml jigu-application/pom.xml
COPY jigu-bootstrap/pom.xml jigu-bootstrap/pom.xml
COPY jigu-common/pom.xml jigu-common/pom.xml
COPY jigu-domain/pom.xml jigu-domain/pom.xml
COPY jigu-infrastructure/pom.xml jigu-infrastructure/pom.xml

# 复制所有源码
COPY jigu-api jigu-api
COPY jigu-application jigu-application
COPY jigu-bootstrap jigu-bootstrap
COPY jigu-common jigu-common
COPY jigu-domain jigu-domain
COPY jigu-infrastructure jigu-infrastructure

# 打包构建（jigu-bootstrap 模块在 package 阶段会执行 spring-boot:repackage）
# 使用并行构建（-T1C）提升速度
RUN mvn -B -DskipTests -T1C clean package


# ===== run stage =====
FROM ${JRE_BASE_IMAGE}
WORKDIR /app

# 安装运行时工具（Debian/Ubuntu 基础镜像使用 apt）
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd -r app && useradd -r -g app app
USER app

# 复制构建产物：拷贝 jigu-bootstrap 模块 target 下所有 jar
# 在运行时选择最大的 jar（通常为 fat-jar）来启动，避免硬编码文件名导致错误
COPY --from=build /workspace/jigu-bootstrap/target/*.jar /app/

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/api/v1/health || exit 1

# JVM参数配置
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "jarfile=$(ls -S /app/*.jar 2>/dev/null | head -n1); exec java $JAVA_OPTS -jar \"$jarfile\""]