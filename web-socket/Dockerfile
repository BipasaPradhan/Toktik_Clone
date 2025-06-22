# Stage 1: Build with Maven & JDK - https://medium.com/@vitthalchandankar2/creating-a-dockerfile-for-a-spring-boot-application-a-step-by-step-guide-237e84bb37b6Add commentMore actions
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set the working directory in the container to /app
WORKDIR /app

# Copy the pom.xml and the source code to the container
COPY ./pom.xml .

RUN mvn dependency:go-offline

COPY ./src ./src

# Build the application using Maven (skip tests)
RUN mvn clean package

# Stage 2: Run app with OpenJDK
FROM eclipse-temurin:21-jre

# Copy jar file
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app.jar"]