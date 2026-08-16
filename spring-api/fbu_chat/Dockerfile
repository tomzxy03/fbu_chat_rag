# ─── Stage 1: Build ──────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Copy coccoc-tokenizer.jar TRƯỚC khi copy pom.xml.
# pom.xml khai báo systemPath=${project.basedir}/libs/coccoc-tokenizer.jar
# → Maven cần file này tồn tại khi parse pom, kể cả lúc resolve dependencies.
COPY libs/coccoc-tokenizer.jar libs/coccoc-tokenizer.jar

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Warm Maven cache: resolve tất cả dependency từ remote repos.
# dependency:resolve xử lý system scope đúng cách (skip download, dùng local path).
# Layer này được cache bởi Docker nếu pom.xml không thay đổi.
RUN ./mvnw dependency:resolve dependency:resolve-plugins -q

# Copy source và build JAR (bỏ qua tests)
COPY src/ src/
RUN ./mvnw clean package -DskipTests -q

# ─── Stage 2: Run ────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS run
WORKDIR /app

COPY --from=build /build/target/fbu_chat-0.0.1-SNAPSHOT.jar app.jar

# CocCoc native libs được mount từ host qua volume (xem docker-compose.yml)
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.library.path=/app/libs", "-jar", "app.jar"]
