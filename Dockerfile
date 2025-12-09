# Use official Eclipse Temurin OpenJDK 17 image (stable LTS)
FROM eclipse-temurin:21-jdk

# Set working directory inside container
WORKDIR /app

# Copy all files from backend folder into container
COPY . .

# Give execute permission to Maven wrapper
RUN chmod +x mvnw

# Build the project (skip tests)
RUN ./mvnw clean package -DskipTests

# Expose port 8080 (Spring Boot default)
EXPOSE 8080

# Run the jar (wildcard picks the built jar automatically)
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
