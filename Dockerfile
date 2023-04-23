FROM maven:3.8.3-openjdk-17-slim
FROM openjdk:17.0.2-oracle
MAINTAINER io.github.srinss01
WORKDIR /bot
COPY .mvn /bot/.mvn
COPY mvnw pom.xml /bot/
RUN ./mvnw dependency:resolve
COPY src /bot/src
COPY sites /bot/sites
CMD ["./mvnw", "package", "-DskipTests"]
COPY target/*.jar smsspambot-0.0.1.jar
ENTRYPOINT ["java","-jar","smsspambot-0.0.1.jar"]