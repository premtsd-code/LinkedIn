# NOTE: build from the REPO ROOT so the shared starter is in context:
#   docker build -f notification-service/Dockerfile -t premtsd18/notification-service .
FROM maven:3.9.4-eclipse-temurin-21-alpine

WORKDIR /app

# shared platform starter first (cached unless it changes)
COPY linkedin-platform-starter ./linkedin-platform-starter
RUN mvn -q -f linkedin-platform-starter/pom.xml install -DskipTests

COPY notification-service/pom.xml ./notification-service/pom.xml
RUN mvn -q -f notification-service/pom.xml dependency:go-offline

COPY notification-service/src ./notification-service/src

WORKDIR /app/notification-service
CMD ["mvn", "spring-boot:run"]
