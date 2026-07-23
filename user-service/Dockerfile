# NOTE: build from the REPO ROOT so the shared starter is in context:
#   docker build -f user-service/Dockerfile -t premtsd18/user-service .
FROM maven:3.9.4-eclipse-temurin-21-alpine

WORKDIR /app

# shared platform starter first (cached unless it changes)
COPY linkedin-platform-starter ./linkedin-platform-starter
RUN mvn -q -f linkedin-platform-starter/pom.xml install -DskipTests

COPY user-service/pom.xml ./user-service/pom.xml
RUN mvn -q -f user-service/pom.xml dependency:go-offline

COPY user-service/src ./user-service/src

WORKDIR /app/user-service
CMD ["mvn", "spring-boot:run"]
