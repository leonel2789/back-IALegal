# IA Legal Backend

Backend Spring Boot para la aplicación IA Legal con manejo de sesiones de chat y integración con Keycloak.

## Características

- ✅ **Session Management**: API REST completa para manejo de sesiones de chat
- ✅ **Keycloak Integration**: Autenticación y autorización con OAuth2/JWT
- ✅ **Multi-Agent Support**: Soporte para diferentes tipos de agentes de IA
- ✅ **PostgreSQL**: Base de datos para producción con fallback a H2 para desarrollo
- ✅ **CORS Configuration**: Configurado para funcionar con Expo/React Native
- ✅ **JPA Auditing**: Timestamps automáticos para entidades
- ✅ **Validation**: Validación de entrada con Bean Validation
- ✅ **Coolify Ready**: Configurado para deployment en Coolify

## Deployment

**Producción**: `https://ialegalbackend.nilosolutions.com`

El backend está configurado para ser desplegado en Coolify junto con el frontend.

## Tecnologías

- **Spring Boot 3.2.0**
- **Java 17**
- **Spring Security** con OAuth2 Resource Server
- **Spring Data JPA**
- **PostgreSQL** / H2 Database
- **Lombok** para reducir boilerplate
- **Maven** para gestión de dependencias

## API Endpoints

### Sesiones

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/sessions` | Crear nueva sesión |
| GET | `/api/sessions` | Obtener sesiones del usuario |
| GET | `/api/sessions/agent/{agentType}` | Sesiones por tipo de agente |
| GET | `/api/sessions/{sessionId}` | Obtener sesión específica |
| PUT | `/api/sessions/{sessionId}/name` | Actualizar nombre de sesión |
| DELETE | `/api/sessions/{sessionId}` | Eliminar sesión |
| GET | `/api/sessions/search?query={term}` | Buscar sesiones |

### Mensajes

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/sessions/{sessionId}/messages` | Obtener mensajes de sesión |
| POST | `/api/sessions/{sessionId}/messages` | Agregar mensaje a sesión |

### Health Check

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/sessions/health` | Estado del servicio |

## Tipos de Agentes Soportados

- `ia-contratos` - IA especializada en contratos legales
- `ia-laboral` - IA especializada en derecho laboral
- `ia-defensa-consumidor` - IA especializada en defensa del consumidor
- `ia-general` - IA general para consultas legales

## Configuración Frontend

Para conectar el frontend con este backend:

```bash
# Variables de entorno del frontend
EXPO_PUBLIC_USE_SPRING_BOOT=true
EXPO_PUBLIC_SPRING_BOOT_URL=https://ialegalbackend.nilosolutions.com
```

## Configuración pgvector Databases

Este backend se conecta a las bases de datos **pgvector existentes** de N8N:

### Variables de Entorno para Coolify

```bash
# Databases pgvector por agente
CONTRATOS_DB_URL=jdbc:postgresql://your-db-host:5432/n8n_contratos
CONTRATOS_DB_USERNAME=n8n_user
CONTRATOS_DB_PASSWORD=your_password

LABORAL_DB_URL=jdbc:postgresql://your-db-host:5432/n8n_laboral
LABORAL_DB_USERNAME=n8n_user
LABORAL_DB_PASSWORD=your_password

DEFENSA_DB_URL=jdbc:postgresql://your-db-host:5432/n8n_defensa
DEFENSA_DB_USERNAME=n8n_user
DEFENSA_DB_PASSWORD=your_password

GENERAL_DB_URL=jdbc:postgresql://your-db-host:5432/n8n_general
GENERAL_DB_USERNAME=n8n_user
GENERAL_DB_PASSWORD=your_password

# Keycloak
KEYCLOAK_ISSUER_URI=https://keycloak.nilosolutions.com/realms/ia-legal
KEYCLOAK_JWK_SET_URI=https://keycloak.nilosolutions.com/realms/ia-legal/protocol/openid-connect/certs
```

## Instalación y Ejecución

### Prerrequisitos

- Java 17+
- Maven 3.6+
- PostgreSQL (para producción)
- Keycloak server ejecutándose

### Desarrollo

```bash
# Clonar y compilar
git clone <repository-url>
cd back-IALegal
mvn clean compile

# Ejecutar en desarrollo
mvn spring-boot:run
```

### Producción (Coolify)

```bash
# Ejecutar con perfil de producción
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Security

- **JWT Validation**: Tokens validados contra Keycloak
- **CORS**: Configuración para dominios de producción y desarrollo
- **Authorization**: Acceso basado en ownership de sesiones
- **Input Validation**: Validación robusta de entrada