# NOTE: build from the REPO ROOT so the shared starter is in context:
#   docker build -f post-service/Dockerfile -t premtsd18/post-service .
FROM maven:3.9.4-eclipse-temurin-21-alpine

WORKDIR /app

# shared platform starter first (cached unless it changes)
COPY linkedin-platform-starter ./linkedin-platform-starter
RUN mvn -q -f linkedin-platform-starter/pom.xml install -DskipTests

COPY post-service/pom.xml ./post-service/pom.xml
RUN mvn -q -f post-service/pom.xml dependency:go-offline

COPY post-service/src ./post-service/src

WORKDIR /app/post-service
CMD ["mvn", "spring-boot:run"]
