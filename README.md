# AlmuerzoYa — API REST (Spring Boot 4 + JWT)

API para registrar pedidos de almuerzo. Solo el personal autenticado puede gestionarlos
y cada usuario ve únicamente sus propios pedidos. Base de datos: MySQL local por defecto,
con soporte para PostgreSQL en la nube (Render).

## Requisitos

- JDK 21 (en esta PC: `C:\Program Files\Android\Android Studio\jbr`). El Gradle Wrapper
  incluido descarga Gradle automáticamente.
- MySQL 8 en ejecución (o PostgreSQL si se configura `DB_URL` de PostgreSQL).
- Postman (opcional) para la colección incluida.

## Endpoints

Todas las rutas llevan el prefijo `/api` (`server.servlet.context-path`).

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/register` | Público | Crea usuario (BCrypt) y devuelve token |
| POST | `/api/auth/login` | Público | Valida credenciales y devuelve token JWT |
| GET | `/api/almuerzos` | Bearer | Pedidos del usuario, con `total` |
| GET | `/api/almuerzos/{id}` | Bearer | Un pedido del usuario |
| POST | `/api/almuerzos` | Bearer | Registra pedido (la API asigna `fecha_pedido`) |
| PUT | `/api/almuerzos/{id}` | Bearer | Edita pedido o marca entregado |
| DELETE | `/api/almuerzos/{id}` | Bearer | Anula pedido |
| GET | `/api/almuerzos/resumen` | Bearer | **Complementaria:** cantidad y monto del día |

Respuesta de auth:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEn": 1764188400000,
  "nombre": "Ana Sandoval",
  "email": "ana@correo.com",
  "rol": "USER"
}
```

Respuesta de pedido:

```json
{
  "id": 1,
  "cliente": "Carlos Pérez",
  "plato": "Arroz con pato",
  "cantidad": 2,
  "precio": 25.50,
  "total": 51.00,
  "fechaPedido": "2026-11-26",
  "entregado": false
}
```

## Configuración

Copia `.env.example` y ajusta las variables. Las más importantes:

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `DB_URL` | JDBC de MySQL/PostgreSQL | MySQL en `localhost:3306/almuerzoya` |
| `DB_USER`, `DB_PASSWORD` | Credenciales | `root` / vacía |
| `DB_DRIVER` | Driver JDBC | `com.mysql.cj.jdbc.Driver` |
| `DB_DIALECT` | Dialecto Hibernate | `org.hibernate.dialect.MySQLDialect` |
| `JWT_SECRET` | Clave de firma (≥32 caracteres) | valor solo para desarrollo |
| `JWT_EXPIRATION_MS` | Vigencia del token | `3600000` (1 hora) |
| `PORT` | Puerto HTTP | `8080` (Render lo asigna) |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos | `*` |

La URL por defecto usa `createDatabaseIfNotExist=true`, y `ddl-auto=update` crea/actualiza
las tablas `usuario` y `almuerzo`. En producción se recomienda `DDL_AUTO=validate` y
aplicar migraciones.

## Ejecutar en local (Windows / PowerShell)

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:DB_USER='root'
$env:DB_PASSWORD='tu_password_de_mysql'
$env:JWT_SECRET='clave-super-secreta-de-al-menos-32-caracteres'
.\gradlew.bat bootRun
```

La API queda en `http://localhost:8080/api`.

> En esta PC el servicio `MySQL80` escucha en el puerto **3308** (no en 3306).
> Si ese es tu caso, define antes:
> `$env:DB_URL='jdbc:mysql://localhost:3308/almuerzoya?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'`

Verificación rápida:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/actuator/health'
```

Pruebas (no necesitan MySQL): 3 pruebas unitarias de JWT/BCrypt y una prueba de
integración con H2 en memoria que verifica 401, login, CRUD, resumen y que un segundo
usuario no ve los pedidos del primero.

```powershell
.\gradlew.bat test
```

## Postman y JWT

1. Importa `postman/AlmuerzoYa.postman_collection.json`.
2. Revisa las variables de la colección (`baseUrl`, `email`, `password`, `token`, `almuerzoId`).
3. Ejecuta **Registrar usuario** o **Iniciar sesión**: el script guarda el token en la variable `token`.
4. Ejecuta **Registrar pedido** y luego el resto de endpoints; **401 sin token** verifica la seguridad.
5. Copia el token y pégalo en [jwt.io](https://jwt.io): `sub` = email, claim `rol` y fecha `exp`.

Para probar con otro usuario, cambia la variable `email`, regístralo y verifica que no ve
los pedidos del primero.

## Publicación en Render

El `Dockerfile` compila con el Gradle Wrapper y corre el JAR con JRE 21.

### 1. Crear la base de datos (Render Postgres)

Render ofrece PostgreSQL administrado (no MySQL). En el Dashboard:

1. **+ New → Postgres**. Nombre: `almuerzoya-db`.
2. Elige la **misma Region** que usarás para el Web Service (ej. Oregon).
3. Versión PostgreSQL 17 o 18. Plan **Free** (1 GB; ojo: expira a los 30 días).
4. **Create database**. Render crea automáticamente la BD y el usuario.
5. En la página de la BD, menú **Connect → Internal Connection String**. Copia:
   `postgresql://USUARIO:PASSWORD@HOST:5432/NOMBRE_BD`.

No necesitas crear las tablas: con `DDL_AUTO=update`, Hibernate crea `usuario` y
`almuerzo` en el primer arranque. Puedes verificarlo con el *PSQL Command* que da
Render y ejecutando `\dt`.

### 2. Crear el Web Service

**+ New → Web Service**, conecta el repositorio, Runtime = **Docker**, misma Region
que la BD, plan Free. En **Environment** agrega:

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://HOST:5432/NOMBRE_BD?sslmode=require` |
| `DB_USER` | usuario copiado de *Connect* |
| `DB_PASSWORD` | contraseña copiada de *Connect* |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `DB_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` |
| `DDL_AUTO` | `update` (luego cambia a `validate`) |
| `JWT_SECRET` | clave aleatoria de ≥ 32 caracteres |
| `CORS_ALLOWED_ORIGINS` | `*` |

> En `DB_URL` NO pongas `createDatabaseIfNotExist=true`: esa opción es solo de MySQL.

Genera el secreto con:

```powershell
python -c "import secrets; print(secrets.token_urlsafe(48))"
```

3. Health Check Path: `/api/actuator/health`.
4. `PORT` lo asigna Render automáticamente (la app ya usa `server.port=${PORT:8080}`).

### 3. Probar

```powershell
Invoke-RestMethod 'https://TU-SERVICIO.onrender.com/api/actuator/health'
```

Luego en Postman cambia la variable `baseUrl` a `https://TU-SERVICIO.onrender.com/api`
y ejecuta la colección. En la app Flutter:

```powershell
flutter build apk --release --dart-define=API_BASE_URL=https://TU-SERVICIO.onrender.com/api
```

> El plan Free duerme el Web Service tras 15 min sin tráfico: la primera petición
> tarda alrededor de 1 min. Haz una petición antes de la presentación. La BD Free
> expira a los 30 días; súbela a un plan pago si la necesitas más tiempo.

Si prefieres MySQL, Render no lo ofrece administrado: usa un proveedor externo
(Aiven, Railway, etc.) y configura `DB_URL=jdbc:mysql://...`,
`DB_DRIVER=com.mysql.cj.jdbc.Driver`, `DB_DIALECT=org.hibernate.dialect.MySQLDialect`.

Prueba local con Docker:

```powershell
docker build -t almuerzoservice .
docker run -p 8080:8080 -e DB_URL='jdbc:mysql://host.docker.internal:3306/almuerzoya' `
  -e DB_USER=root -e DB_PASSWORD=tu_clave `
  -e JWT_SECRET='clave-super-secreta-de-al-menos-32-caracteres' almuerzoservice
```

## Estructura de capas

```
src/main/java/com/sandovalpurisaca/almuerzoservice/
├── config/       SecurityConfig (stateless, CORS, 401 JSON)
├── controller/   AuthController, AlmuerzoController
├── dto/          Registro/Login/AuthResponse, AlmuerzoRequest/Response, ResumenDia
├── entity/       Usuario, Almuerzo
├── exception/    Validación, 404, 409, 401 y manejo global
├── repository/   UsuarioRepository, AlmuerzoRepository (filtrado por usuario)
├── security/     JwtService, JwtAuthenticationFilter, UserDetailsService
└── service/      AuthService, AlmuerzoService
```

## Seguridad implementada

- Contraseñas BCrypt; nunca se devuelven en las respuestas.
- JWT HS256 firmado con `JWT_SECRET`, `sub` = email, claim `rol`, `exp` = fecha de vencimiento.
- API `stateless`; sin token, token alterado o vencido → **401** con cuerpo JSON.
- El usuario se obtiene del token (`@AuthenticationPrincipal`), nunca de la URL ni del cuerpo.
- Las consultas usan `findByIdAndUsuarioId`, por lo que nadie ve o modifica pedidos ajenos.
- Clave JWT y credenciales de BD se leen de variables de entorno.

## Subir a Git

```powershell
git init
git add .
git commit -m "API AlmuerzoYa con JWT y colección de Postman"
git remote add origin https://github.com/TU_USUARIO/almuerzoservice.git
git push -u origin main
```
