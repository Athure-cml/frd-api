# 可选：Render 选 Docker 运行时可用（默认推荐直接用 Java runtime + render.yaml）
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/quote-api-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=render
ENV PORT=10000
EXPOSE 10000

CMD ["sh", "-c", "java -Dserver.port=${PORT} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]
