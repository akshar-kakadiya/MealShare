# ==============================================================================
# Multi-Stage Dockerfile for MealShare Spring Boot Backend
# ==============================================================================

# --- Stage 1: Build Application ---
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and POM for dependency layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy application source and build JAR (skip unit tests during image build)
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# --- Stage 2: Production Runtime ---
FROM eclipse-temurin:17-jre-alpine AS runtime

# Install curl/wget for container health check
RUN apk add --no-cache curl tzdata

# Create dedicated non-root application user
RUN addgroup -S mealshare && adduser -S mealshare -G mealshare

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder /build/target/mealshare-backend-*.jar app.jar

# Ensure ownership
RUN chown -R mealshare:mealshare /app

USER mealshare

# Default environment configurations
ENV PORT=8081 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

EXPOSE 8081

# Liveness probe
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${PORT}/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
