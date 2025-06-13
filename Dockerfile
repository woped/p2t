FROM adoptopenjdk/openjdk11:alpine-slim
ARG JAR_FILE=target/*.jar
RUN mkdir -p /logs && chmod 777 /logs
COPY ${JAR_FILE} app.jar
COPY src/main/resources src/main/resources
ENTRYPOINT ["java","-jar","/app.jar"]