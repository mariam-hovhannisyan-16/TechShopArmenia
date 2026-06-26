FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY techshop-rest-api/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]