FROM maven:3.9.4-eclipse-temurin-21-alpine

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline

COPY src ./src

CMD ["mvn", "spring-boot:run"]
