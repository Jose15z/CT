# Despliegue gratuito de CulitosTracker

Objetivo: producción a coste cero, sin Render.

| Pieza | Servicio | Tier gratuito |
|---|---|---|
| Frontend | Vercel | Hobby, deploy automático desde GitHub |
| Backend | Koyeb | 1 servicio web, 512 MB RAM |
| PostgreSQL | Aiven | 1 vCPU, 1 GB RAM, 5 GB de disco |

Los tiers gratuitos cambian con frecuencia: verifica las condiciones al darte de alta.

## 1. Base de datos (Aiven)

1. Cuenta en https://aiven.io (vale GitHub SSO) → *Create service* → **PostgreSQL** → plan **Free**.
2. Cuando el servicio esté `Running`, copia del panel *Connection information*: host, puerto, base de datos (`defaultdb`), usuario (`avnadmin`) y contraseña.
3. La URL JDBC queda así (Aiven exige TLS):

   ```
   jdbc:postgresql://<host>:<puerto>/defaultdb?sslmode=require
   ```

## 2. Backend (Koyeb)

1. Cuenta en https://koyeb.com (GitHub SSO) → *Create Web Service* → *GitHub* → repo `Jose15z/CT`.
2. Configuración del servicio:
   - **Builder**: Dockerfile · **Work directory**: `backend` · **Dockerfile location**: `backend/Dockerfile`
   - **Instance**: Free · **Port**: 8080 · **Health check**: TCP 8080
3. Variables de entorno (las sensibles como *Secret*):

   | Variable | Valor |
   |---|---|
   | `DB_URL` | la URL JDBC del paso 1 |
   | `DB_USER` | `avnadmin` |
   | `DB_PASSWORD` | (secret) contraseña de Aiven |
   | `JWT_SECRET` | (secret) genera uno: `openssl rand -base64 48` |
   | `CORS_ALLOWED_ORIGINS` | la URL del frontend en Vercel, p. ej. `https://ct-xxxx.vercel.app` |
   | `JAVA_TOOL_OPTIONS` | `-Xmx256m -XX:MaxMetaspaceSize=128m` |

   No definas `SPRING_PROFILES_ACTIVE`: el seed demo no debe correr en producción.
4. Deploy. Flyway crea el esquema solo en el primer arranque. La URL pública será `https://<app>.koyeb.app`.

## 3. Frontend (Vercel)

Proyecto conectado al repo con *Root Directory* `frontend` (framework Vite se autodetecta; `frontend/vercel.json` ya trae el rewrite de SPA y el cache de assets).

Variable de entorno del proyecto (Production + Preview):

| Variable | Valor |
|---|---|
| `VITE_API_URL` | `https://<app>.koyeb.app` (sin barra final) |

`VITE_API_URL` se hornea en el build: tras cambiarla hay que redeployar.

## 4. Cierre del círculo

1. Con la URL real de Vercel, ajusta `CORS_ALLOWED_ORIGINS` en Koyeb (redeploy automático).
2. Prueba: registro de usuario nuevo, crear pareja, registrar periodo, subir foto de perfil.

## Alternativa con más capacidad

Si algún día quieres más músculo gratis: una VM ARM de **Oracle Cloud Always Free** (hasta 4 OCPUs / 24 GB RAM) corre el `docker compose up` completo, base de datos incluida. A cambio, la operación (TLS con Caddy, backups, actualizaciones) es tuya.
