# ─── Stage 1: build ─────────────────────────────────────────────────────────
# Imagem oficial do Maven com JDK 21 — evita depender do Maven Wrapper.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia o pom.xml primeiro para cachear o download de dependências:
# essa camada só é refeita quando o pom muda.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# Copia o código e empacota.
COPY src src
RUN mvn -B -ntp package -DskipTests

# ─── Stage 2: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cria usuário não-root (boa prática de segurança)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
