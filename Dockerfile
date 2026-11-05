# Etapa 1: compila o jar
FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# Etapa 2: imagem final, só com o JRE e o jar
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Roda sem privilégios de root
RUN useradd --system --uid 1001 app
USER app

COPY --from=builder /app/target/*-exec.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
