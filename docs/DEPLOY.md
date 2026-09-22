# Despliegue de CulitosTracker (estado actual)

Producción a coste cero. URL pública: **https://culitostracker.vercel.app**

| Pieza | Servicio | Detalle |
|---|---|---|
| Frontend | Vercel (Hobby) | Proyecto `culitostracker`, deploy automático al hacer push a `main` |
| Backend | Northflank (Developer Sandbox) | Servicio `backend`, US-Central, `https://p01--backend--7qf5k72l5sbm.code.run` |
| PostgreSQL | Northflank addon | `postgres` (v17, 512 MB RAM, 6 GB NVMe, TLS, solo red privada) |
| Respaldo BD | Aiven free (`pg-16f8e05d`) | Sin uso actualmente; su plan free se apaga con inactividad |

Nota histórica: el plan original usaba Koyeb, pero fue adquirido por Mistral y cerró el autoservicio (sept. 2026). Northflank lo sustituye con ventaja: sin dormir, BD incluida.

## Northflank (backend + BD)

- Proyecto `culitostracker` en `Jose15z's Team`, región US-Central (única gratuita junto a Europe-West).
- El plan Sandbox exige una tarjeta **solo para verificación**: su FAQ dice textualmente "No — when you enter a card we only verify the card". Cero facturas mientras se usen los recursos gratuitos (2 servicios, 1 addon).
- Servicio `backend`: build tipo Dockerfile con contexto `/backend` y Dockerfile `/backend/Dockerfile`, rama `main`, puerto público 8080 (HTTP). El repo se añadió como *repo público por URL*, así que **no hay auto-deploy al hacer push**: tras cambiar el backend hay que pulsar *Rebuild* (o completar el enlace GitHub↔Northflank en Integrations para activar CI).
- Credenciales de BD: el secret group `db-credentials` está vinculado al addon y las inyecta con alias `DB_URL` (JDBC), `DB_USER`, `DB_PASSWORD`, `DB_NAME`. Nadie las copia a mano; se revocan al desvincular.
- Variables del servicio (256 MB de RAM obligan a ajustar la JVM; con esto arranca en ~100 s):

  ```
  JAVA_TOOL_OPTIONS=-XX:MaxRAM=256m -Xmx80m -XX:MaxMetaspaceSize=112m -XX:ReservedCodeCacheSize=24m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxDirectMemorySize=16m
  SERVER_TOMCAT_THREADS_MAX=8
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=2
  CORS_ALLOWED_ORIGINS=https://culitostracker.vercel.app,https://culitostracker-jose15zs-projects.vercel.app
  JWT_SECRET=(secreto, generado con openssl rand -base64 48)
  ```

  Con presupuestos mayores (p. ej. `-Xmx112m -XX:MaxMetaspaceSize=96m`) el cgroup mata el proceso por OOM al final del arranque: si el servicio se queda en crashloop sin excepción en logs, es esto.
- Tras cambiar variables: **Update & restart** (verifica en la vista View que los valores nuevos quedaron guardados; el editor Env a veces no aplica).

## Vercel (frontend)

- Proyecto `culitostracker` enlazado a `Jose15z/CT` con *Root Directory* `frontend` (framework Vite autodetectado; `frontend/vercel.json` aporta rewrite de SPA y caché de assets).
- Variable `VITE_API_URL=https://p01--backend--7qf5k72l5sbm.code.run` (Production + Preview). Se hornea en el build: si cambia, redeploy.
- Cada push a `main` despliega producción automáticamente.

## Comprobación rápida de salud

```bash
curl -X POST https://p01--backend--7qf5k72l5sbm.code.run/api/auth/login \
  -H 'Content-Type: application/json' -d '{"usernameOrEmail":"x","password":"y"}'
# esperado: HTTP 422 {"code":"auth.invalidCredentials"} → backend vivo y con BD
```

En Northflank → servicio `backend` → Observe → Logs, el arranque sano termina con las migraciones de Flyway aplicadas y Tomcat en el puerto 8080.

## Límites conocidos del tier gratuito

- 0.1 vCPU: el arranque tarda ~2 min tras cada restart; en marcha va sobrado para uso personal.
- Sin auto-deploy del backend (ver arriba).
- Los umbrales de facturación están en $0.00 usados; el ranking de gasto se ve en Billing → Usage.
