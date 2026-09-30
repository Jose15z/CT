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
- Servicio `backend`: build tipo Dockerfile con contexto `/backend` y Dockerfile `/backend/Dockerfile`, rama `main`, puerto público 8080 (HTTP). Aunque el repo entró como *repo público por URL*, el CI de Northflank **sí detecta los pushes a `main` y redeploya solo** (sondea el repo; tarda unos minutos en verlos). El botón *Rebuild* queda para forzarlo.
- Credenciales de BD: el secret group `db-credentials` está vinculado al addon y las inyecta con alias `DB_URL` (JDBC), `DB_USER`, `DB_PASSWORD`, `DB_NAME`. Nadie las copia a mano; se revocan al desvincular.
- Plan de cómputo: **nf-compute-20** (0,2 vCPU compartida / 512 MB), el mayor que el proyecto gratuito permite elegir (Resources → Compute plan; los planes desde nf-compute-50 aparecen como "Not available due to free project limits"). Con nf-compute-10 (256 MB) la JVM ya no cabe desde que existen Web Push, Stripe y el correo: el proceso se quedaba clavado en el límite del cgroup (`memory.current` = `memory.max`, 1,4 M de eventos `max`, 92 % de los periodos de CPU estrangulados y el 86 % del tiempo de CPU en el kernel reclamando páginas), así que un login tardaba más de 40 s.
- Variables del servicio (la JVM se dimensiona a mano para los 512 MB; arranca en ~60 s con 0,2 vCPU):

  ```
  JAVA_TOOL_OPTIONS=-XX:MaxRAM=512m -Xmx160m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxDirectMemorySize=16m -XX:-UsePerfData -XX:CICompilerCount=1
  MALLOC_ARENA_MAX=2
  SERVER_TOMCAT_THREADS_MAX=8
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=2
  CORS_ALLOWED_ORIGINS=https://culitostracker.vercel.app,https://culitostracker-jose15zs-projects.vercel.app
  JWT_SECRET=(secreto, generado con openssl rand -base64 48)
  APP_SECURITY_BCRYPT_STRENGTH=10
  ```

  Con 0.1 vCPU, BCrypt(12) tardaba ~4 s por login; el coste 10 (mínimo OWASP) lo deja en ~1,3 s. Los hashes antiguos se migran solos en el siguiente login correcto, y el warmup de arranque precompila BCrypt y JDBC para que la primera petición real no pague el JIT frío.

- **Notificaciones push (recordatorios)**: necesitan un par VAPID en el servicio: `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` y `VAPID_SUBJECT` (`mailto:tu@email`). Se generan una vez con `npx web-push generate-vapid-keys`; si cambias las claves, todas las suscripciones existentes dejan de valer y los usuarios tienen que volver a activar los recordatorios. Sin las claves la app funciona igual, solo oculta la sección de recordatorios.
- **Suscripciones (Stripe)**: opcionales. Crea en Stripe un producto con precio recurrente y define `STRIPE_SECRET_KEY`, `STRIPE_PRICE_ID` y `STRIPE_WEBHOOK_SECRET` (el endpoint del webhook es `https://<backend>/api/billing/webhook` con los eventos `checkout.session.completed`, `customer.subscription.updated` y `customer.subscription.deleted`). Empieza con las claves de test (`sk_test_…`). Sin estas variables no hay pagos y todas las cuentas son Pro.
- **Recuperar contraseña sin SMTP**: no hay proveedor de email configurado, así que cuando alguien usa "¿Olvidaste tu contraseña?" el enlace de restablecimiento aparece en los logs del backend (Observe → Logs, busca `password reset link`). Copia el enlace y entrégaselo al usuario (caduca en 30 min y es de un solo uso). Para enviar emails de verdad, define en el servicio `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` y `MAIL_FROM` con cualquier SMTP (p. ej. Brevo tiene 300 emails/día gratis) y reinicia; no hace falta tocar código.

  Medido con `-XX:NativeMemoryTracking=summary` y el mismo jar: el proceso compromete ~220 MB (heap 82 MB con `-Xmx80m`, metaspace 71 MB para ~19 500 clases, tabla de símbolos 18 MB, code cache 16 MB, CDS del JDK 14 MB). Por eso 256 MB no dejaban sitio ni para la caché de páginas del jar (91 MB). Con `-Xmx160m` el tope teórico ronda los 330 MB y quedan ~180 MB de margen en 512 MB.

  `MALLOC_ARENA_MAX=2` recorta las arenas de glibc (decenas de MB de RSS nativo en la JVM) y `-XX:-UsePerfData -XX:CICompilerCount=1` ahorran algo más. Síntoma de quedarse sin margen: el log de arranque muestra `Warmup completed in` con cientos de miles de ms y las peticiones agotan el tiempo (el cgroup estrangula el proceso antes de matarlo); se confirma desde Observe → Shell con `cat /sys/fs/cgroup/memory.current /sys/fs/cgroup/memory.max /sys/fs/cgroup/cpu.stat`. El cliente de Web Push (BouncyCastle) se crea perezosamente en el primer envío, Hibernate corre con `bytecode.provider=none` (las entidades no tienen asociaciones perezosas) y el pool/threads vienen acotados en `application.yml` por la misma razón.
- Tras cambiar variables: **Update & restart** (verifica en la vista View que los valores nuevos quedaron guardados; el editor Env a veces no aplica).

## Vercel (frontend)

- Proyecto `culitostracker` enlazado a `Jose15z/CT` con *Root Directory* `frontend` (framework Vite autodetectado; `frontend/vercel.json` aporta rewrite de SPA y caché de assets).
- Variable `VITE_API_URL=https://p01--backend--7qf5k72l5sbm.code.run` (Production + Preview). Se hornea en el build: si cambia, redeploy.
- Cada push a `main` despliega producción automáticamente.

## Buscadores (Google)

El frontend ya lleva lo que un buscador necesita para indexar una SPA: `index.html` con título, descripción, canonical, Open Graph/Twitter (imagen `public/og.png`, 1200×630) y datos estructurados `WebApplication`; `public/robots.txt` (permite todo salvo `/invite/` y `/reset-password`) y `public/sitemap.xml` con `/`, `/register`, `/privacy` y `/login`. Cada ruta ajusta título, descripción, `robots` y canonical desde `src/lib/seo.ts` (`usePageMeta`): solo la portada, el registro y la política de privacidad son indexables; login, recuperación, invitaciones, 404 y todo lo que está detrás del login llevan `noindex`. `/privacy` es pública (sin sesión se muestra con la cabecera de la portada). `vercel.json` excluye `.xml` del rewrite de la SPA para que el sitemap se sirva tal cual.

Search Console (hecho el 2026-09-29): la propiedad de tipo **Prefijo de URL** `https://culitostracker.vercel.app/` está registrada en la cuenta de Google `rodriguezospinajosemanuel@gmail.com` (el tipo "Dominio" exige DNS y `vercel.app` no es nuestro). La verificación de propiedad se apoya en dos cosas que no hay que borrar: el archivo `frontend/public/googlea12d97953ef0320e.html` y la etiqueta `<meta name="google-site-verification">` del `<head>` de `frontend/index.html` (`vercel.json` excluye `.html` del rewrite y el service worker no lo precachea). El sitemap está enviado y se pidió la indexación de `/` y `/register`. Para dar acceso a otra cuenta: Ajustes → Usuarios y permisos.

La primera aparición suele tardar entre unos días y dos semanas; buscar `site:culitostracker.vercel.app` muestra qué hay indexado, y en Search Console → Páginas se ve el motivo si algo no se indexa.

Bing Webmaster Tools (https://www.bing.com/webmasters) permite importar la propiedad desde Search Console con un clic y cubre también DuckDuckGo.

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
