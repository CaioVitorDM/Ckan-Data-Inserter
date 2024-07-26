FROM openjdk:17-jre-slim

WORKDIR /app

COPY target/Ckan-Data-Inserter-0.0.1-SNAPSHOT.jar /app/Ckan-Data-Inserter-0.0.1-SNAPSHOT.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "Ckan-Data-Inserter-0.0.1-SNAPSHOT.jar"]
