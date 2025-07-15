#FROM maven:3.9.4-eclipse-temurin-21-alpine
#
#WORKDIR /app
#
#COPY .mvn/ .mvn
#COPY mvnw pom.xml ./
#
#RUN chmod +x mvnw
#RUN ./mvnw dependency:go-offline
#
#COPY src ./src
#
#CMD ["./mvnw", "spring-boot:run"]


FROM maven:3.9.4-eclipse-temurin-21-alpine

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline

COPY src ./src

CMD ["mvn", "spring-boot:run"]
