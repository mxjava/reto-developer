# Reto Developer – Plataformas Especiales 2026

Solución modular para procesamiento de transacciones construida con Java 21, Spring Boot 4.1.1, Spring Cloud OpenFeign, Spring Security OAuth2 Resource Server, Spring Data JPA/H2 y React/Vite.

## Arquitectura

```text
React / Vite
    |
    | AES-256-GCM + Bearer JWT
    v
transaction-gateway-api :8081
    |
    | OpenFeign + propagación Bearer
    v
transaction-service-api :8082
    |
    | Spring Data JPA
    v
H2
```

### Módulos

```text
security-common
└── com.challenge.transaction.security

transaction-gateway-api
└── com.challenge.transaction.gateway
    ├── api
    ├── application
    └── infrastructure

transaction-service-api
└── com.challenge.transaction.service
    ├── api
    ├── application
    ├── domain
    └── infrastructure

transaction-front
└── React / Vite
```

## Funcionalidad

- Login con usuario/password y BCrypt.
- Emisión y validación de JWT.
- Autorización por scopes.
- Registro de transacciones.
- Validación de entrada con Jakarta Bean Validation.
- Cifrado AES-256-GCM desde el frontend y descifrado en el Gateway.
- Integración Gateway → Service mediante Spring Cloud OpenFeign.
- Persistencia H2 con Spring Data `JpaRepository`.
- Referencia numérica aleatoria de seis dígitos.
- Estatus inicial `Aprobada`.
- Cancelación `Aprobada` → `Cancelada` mediante `PATCH` y `@Query`.
- Consulta paginada y ordenada.
- Consola H2 y acceso TCP disponibles únicamente para ejecución local/demo.

## Seguridad

El proyecto utiliza componentes estándar del ecosistema Java/Spring:

- Spring Security OAuth2 Resource Server.
- Nimbus JOSE/JWT.
- BCrypt con coste 12.
- Jakarta Bean Validation.
- Spring `ProblemDetail` para respuestas de error.
- Spring Cloud OpenFeign.
- JCA/JCE para AES-256-GCM.
- Variables de entorno para secretos y credenciales.
- APIs stateless.
- CORS por allow-list.
- H2 Console deshabilitada por defecto y habilitada explícitamente para demo local.
- OpenFeign sin retry automático de operaciones POST/PATCH.

> El campo `secreto` se conserva descifrado únicamente para cumplir el requerimiento funcional del reto y debe contener datos de demostración no sensibles. Este proyecto no constituye una certificación PCI DSS.

## Calidad de código

El build incorpora controles de formato y análisis estático:

- Java 21.
- Spotless.
- google-java-format, estilo AOSP con indentación de 4 espacios.
- Eliminación/verificación de imports no utilizados.
- PMD `errorprone` y `bestpractices`.
- Maven Enforcer.
- OWASP Dependency-Check mediante perfil Maven `security`.
- Integración preparada para SonarQube/SonarCloud.

Validación local:

```powershell
.\scripts\Verify-Quality.ps1
```

## Requisitos

- Java 21
- Maven 3.9+
- Node.js 22.12+
- npm 10+

## Ejecución completa

Desde la raíz del proyecto:

```powershell
.\scripts\Start-Demo.ps1
```

El script:

1. limpia duplicados generados por sincronización;
2. aplica el formato Java configurado;
3. compila el backend;
4. compila el frontend;
5. genera secretos efímeros para la sesión;
6. solicita el password temporal del usuario `admin`;
7. levanta Service, Gateway, H2 TCP y Frontend.

Servicios locales:

```text
Frontend   http://localhost:5173
Gateway    http://localhost:8081
Service    http://localhost:8082
H2 Console http://localhost:8082/h2-console
H2 TCP     localhost:9092
```

## Variables de entorno

No se deben versionar valores reales.

```text
SECURITY_JWT_SECRET_BASE64
APP_AES_KEY_BASE64
H2_DB_USERNAME
H2_DB_PASSWORD
APP_BOOTSTRAP_USERNAME
APP_BOOTSTRAP_PASSWORD
APP_CORS_ALLOWED_ORIGINS
TRANSACTION_SERVICE_URL
SECURITY_JWT_ISSUER
SECURITY_JWT_AUDIENCE
SECURITY_JWT_TTL_MINUTES
```

Frontend:

```text
VITE_API_URL
VITE_AES_KEY_BASE64
```

Se incluye `.env.example` únicamente como referencia de configuración.

## Build manual

Backend:

```bash
mvn package
```

Calidad:

```bash
mvn -DskipTests spotless:check
mvn -DskipTests -Pquality verify
```

Seguridad de dependencias:

```bash
mvn -Psecurity verify
```

Frontend:

```bash
cd transaction-front
npm install --no-fund
npm run build
```

## Estructura del repositorio

```text
reto-developer/
├── pom.xml
├── README.md
├── .editorconfig
├── .env.example
├── .gitignore
├── scripts/
├── security-common/
├── transaction-service-api/
├── transaction-gateway-api/
└── transaction-front/
```

Los artefactos generados (`target/`, `dist/`, `node_modules/`), archivos de entorno reales, documentación interna y el PDF original del reto no se versionan.
