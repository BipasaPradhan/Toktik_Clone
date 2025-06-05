# Stage 1: Build with Maven & JDK - https://medium.com/@vitthalchandankar2/creating-a-dockerfile-for-a-spring-boot-application-a-step-by-step-guide-237e84bb37b6
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
# Wait for MariaDB to be ready before starting the app: https://medium.com/@pavel.loginov.dev/wait-for-services-to-start-in-docker-compose-wait-for-it-vs-healthcheck-e0248f54962b
ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

EXPOSE 8081
ENTRYPOINT ["./wait-for-it.sh", "maria-db:3306", "--", "java", "-jar", "/app.jar"]