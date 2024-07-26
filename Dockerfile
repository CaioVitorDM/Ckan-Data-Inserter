FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/Ckan-Data-Inserter-0.0.1-SNAPSHOT.jar /app/Ckan-Data-Inserter-0.0.1-SNAPSHOT.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "Ckan-Data-Inserter-0.0.1-SNAPSHOT.jar"]
