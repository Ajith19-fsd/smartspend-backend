# ===============================
# 1. BUILD STAGE
# ===============================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -e -DskipTests clean package

# ===============================
# 2. RUN STAGE
# ===============================
FROM eclipse-temurin:21-jdk
WORKDIR /app

# copy the fat jar
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Environment for production profile
ENV SPRING_PROFILES_ACTIVE=prod

# Expose Render's dynamic PORT variable
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
