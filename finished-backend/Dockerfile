FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre

WORKDIR /app
COPY target/finished-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar /app/app.jar"]
