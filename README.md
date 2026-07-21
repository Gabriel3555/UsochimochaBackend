# UsochimochaBackend

Backend Spring Boot del sistema de gestión de flota y maquinaria de Distrito de Riego Usochicamocha.

## Ambientes

| Perfil | Archivo | Uso |
|---|---|---|
| `dev` | `application-dev.properties` | Desarrollo local. No versionado (ver `.gitignore`). |
| `test` | `application-test.properties` | Pruebas automatizadas (`mvn test`), usa H2 en memoria. |
| `prod` | `application-prod.properties` | Producción, credenciales por variables de entorno (`DB_PASSWORD`, `JWT_SECRET`). |

`application-dev.properties` es un archivo local de cada desarrollador (no se sube al repo). La contraseña de BD y el secreto JWT que contenga son **solo para tu entorno local** — nunca deben coincidir con ninguna credencial real de test o producción, ni compartirse entre desarrolladores.

## Comandos

```bash
mvn clean install                                          # Build
mvn test                                                    # Unit tests
mvn test -Dgroups=e2e                                       # E2E tests
mvn clean spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"  # Run local
```
