FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

# El perfil por defecto de esta imagen es siempre prod.
# Cualquier otro perfil requiere sobreescribir explícitamente esta variable.
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
