
# ==========================================
# 1. Build Stage
# ==========================================
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy Maven files for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source code and jar build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ==========================================
# 2. Runtime Stage
# ==========================================
FROM eclipse-temurin:25-jre
WORKDIR /app

# Create secured user without root privilages
RUN useradd -ms /bin/bash appuser
USER appuser

# Copy produced JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# JVM memory containment for stability
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]