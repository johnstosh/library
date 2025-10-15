# Use a slim Java base image matching the project's Java 17 configuration
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy the Spring Boot JAR
COPY build/libs/*.jar /app/application.jar

# Expose the port (Cloud Run uses PORT env variable, default 8080)
EXPOSE ${PORT:-8080}

# Run the JAR
CMD ["java", "-jar", "/app/application.jar"]
