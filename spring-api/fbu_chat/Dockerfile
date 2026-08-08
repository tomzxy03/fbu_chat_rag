# ─── Stage 1: Build ──────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Copy dependency manifest trước để tận dụng Docker layer cache
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Pre-download dependencies (cached nếu pom.xml không đổi)
RUN ./mvnw dependency:go-offline -q

# Copy source và build JAR (bỏ qua tests)
COPY src/ src/
RUN ./mvnw clean package -DskipTests -q

# ─── Stage 2: Run ────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS run
WORKDIR /app

# Copy JAR từ stage build
COPY --from=build /build/target/fbu_chat-0.0.1-SNAPSHOT.jar app.jar

# CocCoc native libs được mount từ host qua volume (xem docker-compose.yml)
# LD_LIBRARY_PATH và java.library.path set tại runtime

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.library.path=/app/libs", "-jar", "app.jar"]
