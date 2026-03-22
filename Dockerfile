FROM maven:3.9.9-eclipse-temurin-26 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /app/uploads/images
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]