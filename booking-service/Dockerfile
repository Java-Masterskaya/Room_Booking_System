# ======== STAGE 1: Сборка ========
FROM amazoncorretto:25-alpine AS builder
WORKDIR /application
COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ======== STAGE 2: Запуск ========
FROM amazoncorretto:25-alpine

# Установка curl для healthcheck
RUN apk add --no-cache curl

# Создание непривилегированного пользователя
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /application

# Копирование слоёв
COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/application/ ./

# Права на файлы
RUN chown -R appuser:appgroup /application

# Переключаемся на непривилегированного пользователя
USER appuser

# JVM настройки для контейнера
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]