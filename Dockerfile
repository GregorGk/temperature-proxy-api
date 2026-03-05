FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle gradle.properties ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src/ src/
COPY config/ config/
RUN ./gradlew bootJar --no-daemon -x test -x checkstyleMain -x checkstyleTest

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=docker -jar app.jar"]
