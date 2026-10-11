FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY campusconnect-backend/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY campusconnect-backend/src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
COPY frontend /app/static/frontend
COPY css /app/static/css
COPY js /app/static/js
COPY images /app/static/images
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
CMD ["java", "-Xmx400m", "-jar", "app.jar"]
