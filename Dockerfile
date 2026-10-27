FROM eclipse-temurin:26-jdk-noble AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
# O checkout Windows pode usar CRLF; normalize apenas o script dentro da imagem.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -ntp -DskipTests clean package

FROM eclipse-temurin:26-jre-noble AS runtime
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build --chown=app:app /app/target/clinica-api-0.0.1-SNAPSHOT.jar /app/app.jar
USER app

# Documenta o default local. Render fornece PORT em runtime.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
