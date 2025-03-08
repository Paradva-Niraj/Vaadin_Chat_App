# Build stage
FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /workspace/app

# Copy Maven wrapper and POM first
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source files
COPY src src

# Build with production profile
RUN ./mvnw clean package -Pproduction -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy built JAR
COPY --from=builder /workspace/app/target/*.jar app.jar

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]