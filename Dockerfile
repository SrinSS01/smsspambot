FROM openjdk:17.0.2-oracle
MAINTAINER io.github.srinss01
COPY sites /sites
COPY config /config
COPY target/*.jar smsspambot-0.0.1.jar
ENTRYPOINT ["java","-jar","smsspambot-0.0.1.jar"]