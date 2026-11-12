
# Stage 1: Build application
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven configuration
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./

RUN chmod +x ./mvnw && ./mvnw dependency:resolve -B

# Copy application source
COPY backend/src/ src/

# Build executable JAR
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Production runtime
FROM eclipse-temurin:17-jre-alpine AS runtime

RUN apk add --no-cache curl tzdata

RUN addgroup -S mealshare && adduser -S mealshare -G mealshare

WORKDIR /app

COPY --from=builder /build/target/mealshare-backend-*.jar app.jar

RUN chown -R mealshare:mealshare /app

USER mealshare

ENV PORT=8081 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=50.0 -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -Xss512k -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT}/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
