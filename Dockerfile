FROM maven:3.8.3-openjdk-17-slim AS build
COPY src /src
COPY pom.xml /pom.xml
COPY .mvn /.mvn
RUN mvn clean package -DskipTests

FROM openjdk:17.0.2-oracle
MAINTAINER io.github.srinss01
WORKDIR /bot
COPY sites /bot/sites
COPY --from=build target/*.jar /bot/smsspambot-0.0.1.jar
ENTRYPOINT ["java","-jar","smsspambot-0.0.1.jar"]