# ─── Stage 1: build ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copia o Maven wrapper e o pom.xml primeiro
# (Docker cacheia essa camada se o pom.xml não mudar)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Baixa dependências (camada cacheada separada do código)
RUN ./mvnw dependency:resolve -q

# Copia o código e compila
COPY src src
RUN ./mvnw package -DskipTests -q

# ─── Stage 2: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cria usuário não-root (boa prática de segurança)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
