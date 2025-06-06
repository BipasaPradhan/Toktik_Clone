# Stage 1: Build with Maven & JDK
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set the working directory in the container to /app
WORKDIR /app

# Copy the pom.xml and the source code to the container
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

# Build the application using Maven (skip tests)
RUN mvn clean package -DskipTests

# Stage 2: Run app with OpenJDK
FROM eclipse-temurin:21-jre

# Copy jar file 
COPY --from=builder /app/target/*.jar app.jar

# Wait for MariaDB to be ready before starting the app
ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

EXPOSE 8080
ENTRYPOINT ["./wait-for-it.sh", "maria-db:3306", "--", "java", "-jar", "/app.jar"]