# Convenciones de código — UsochimochaBackend

- **Tamaño de archivo**: 500-600 líneas es la señal de alerta para dividir una clase, no una ley. El criterio real es una responsabilidad por clase. Un método > 40-50 líneas es candidato a extraer.
- **Anti-duplicación**: si dos servicios/controladores comparten más del 50% del código, se extrae una clase base o utilidad genérica antes de copiar y pegar.
- **Prohibido hardcodear URLs, credenciales o secretos**: siempre variables de entorno (`${DB_PASSWORD}`, `${JWT_SECRET}`, etc.), nunca valores por defecto con datos reales en `application-*.properties`.
- **Soft-delete siempre**: las entidades tienen un campo `status: Boolean`; nunca se borra un registro en duro.
- **Auditoría**: toda operación de escritura debe llamar a `SaveActionUseCase.save(mensaje)`.
- **Tests**: se actualizan en el mismo PR que cambia el comportamiento. No se mergea con la suite en rojo (`mvn test`).
- **Versionado de rutas**: los controladores nuevos usan `/api/v1/...`.
