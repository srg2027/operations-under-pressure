# Multi-stage build for Spring Boot Backend on Railway
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Cache Maven dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Compile and package production JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/operations-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]
