# Build stage: compila la aplicación con Maven y Java 21
FROM public.ecr.aws/docker/library/maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copiamos primero el pom para aprovechar caché de dependencias
COPY pom.xml .

# Copiamos el código fuente
COPY src ./src

# Compilamos sin ejecutar tests para acelerar el build Docker
RUN mvn -B clean package -DskipTests && \
    mkdir -p /opt/app && \
    cp $(find target -maxdepth 1 -type f -name "*.jar" ! -name "*.original" | head -n 1) /opt/app/app.jar


# Runtime stage: imagen ligera solo para ejecutar el JAR
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine

# Fuentes básicas para evitar problemas en generación de informes/PDF
RUN apk add --no-cache fontconfig ttf-dejavu

WORKDIR /app

COPY --from=build /opt/app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]