FROM eclipse-temurin:21-jdk-alpine
ARG JAR_FILE=target/*.jar

WORKDIR /app

COPY ${JAR_FILE} app.jar
COPY src/main/resources src/main/resources
ENTRYPOINT ["java","-jar","/app.jar"]