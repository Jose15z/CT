# CulitosTracker — Arquitectura y decisiones de diseño

Documento de trabajo. Cubre la estructura del repositorio, el modelo de dominio, el esquema de base de datos, las reglas de negocio, el modelo de privacidad, la API, los servicios de consejo y leaderboard, y el plan de implementación.

## 1. Estructura del repositorio

```
CT/
├── backend/            # Spring Boot (Java 21, Maven)
├── frontend/           # React + TypeScript + Vite
├── docs/               # Este documento y decisiones futuras
├── docker-compose.yml  # Postgres + backend + frontend
├── .env.example
└── README.md
```

Monorepo con dos aplicaciones independientes. El contrato entre ambas es la API REST; el frontend no importa nada del backend.

### Backend: paquetes

```
com.culitostracker
├── domain/            # Entidades JPA, enums y servicios de dominio puros
│   ├── model/         #   (sin dependencias de Spring Web ni de la capa API)
│   └── service/       #   CyclePredictionService, RelationshipAdviceEngine,
│                      #   RelationshipDurationCalculator, RelationshipRules
├── application/       # Servicios de aplicación: transacciones, orquestación
│                      # y AUTORIZACIÓN (ownership / acceso compartido)
├── repository/        # Interfaces Spring Data JPA
├── api/               # Controladores REST
│   └── dto/           # Requests y responses (nunca se exponen entidades)
└── infrastructure/    # Seguridad (JWT), configuración, manejo de errores,
                       # rate limiting, seed de demo
```

Por qué así: los servicios de dominio (`domain/service`) son clases puras sin anotaciones de Spring salvo `@Service` opcional, testeables sin base de datos. Los servicios de aplicación hacen de frontera transaccional y son el único lugar donde se decide quién puede ver qué. Los controladores solo traducen HTTP ⇄ DTO.

### Frontend: estructura

```
frontend/src/
├── app/               # Router, guards, shell de navegación
├── components/ui/     # Sistema de diseño: Button, Field, Sheet, Toast...
├── features/          # Una carpeta por funcionalidad
│   ├── auth/  dashboard/  partners/  calendar/  checkin/
│   ├── stats/ leaderboard/ history/  settings/
├── i18n/              # es.json, en.json
├── lib/               # Cliente HTTP, fechas, helpers de ciclo
└── styles/            # Tokens del design system (CSS variables + Tailwind)
```

## 2. Modelo de dominio

### Entidades

- **User** — cuenta. Incluye `relationshipSituation` (estado sentimental general).
- **Partner** — persona registrada por un usuario (`ownerUserId`). Puede estar vinculada a otra cuenta (`linkedUserId`) para el modelo de consentimiento. Soft delete (`deletedAt`) para conservar historial y para la métrica del leaderboard.
- **Relationship** — la relación entre el owner y ese partner: tipo, estado y fechas (inicio, compromiso, matrimonio, fin). 1:1 con Partner en el MVP.
- **RelationshipMilestone** — hitos del timeline (primera cita, viaje, mudanza...).
- **CycleProfile** — configuración del ciclo de una pareja (1:1 con Partner).
- **PeriodRecord** — cada menstruación registrada (inicio, fin opcional, notas).
- **RelationshipCheckIn** — cómo se siente una persona ese día dentro de la relación. Lo escribe un usuario concreto (`authorUserId`); nunca se inventa.
- **PartnerObservation** — cómo *percibe* el usuario a su pareja. Siempre se presenta como percepción, nunca como hecho.
- **PartnerAccess** — permisos de una cuenta vinculada: qué puede ver el otro usuario (por scope) y con posibilidad de revocarlo.
- **LeaderboardProfile** — participación opt-in en el ranking.
- **DailyTip** — catálogo de consejos: clave i18n + condiciones (fase, etapa de la relación, categoría). El texto vive en `es.json`/`en.json`.
- **RefreshToken** — refresh tokens opacos, hasheados, revocables.

### Decisiones que se apartan del enunciado (y por qué)

1. **`Partner` vs `Relationship` separados.** El enunciado pone los campos de relación dentro de Partner, pero también pide una tabla `relationships` y `RelationshipCheckIn.relationshipId`. Separarlos evita la ambigüedad: Partner es la *persona*, Relationship es el *vínculo*. La API expone un DTO combinado para que el frontend no note la diferencia.
2. **Leaderboard: una sola fuente de verdad.** El enunciado duplica `leaderboardOptIn`/`leaderboardAlias` en User y en LeaderboardProfile. Se consolida todo en `leaderboard_profiles` (enabled, publicAlias, showAvatar). Dos flags para lo mismo terminan desincronizados.
3. **Soft delete en Partner.** Sirve para tres cosas: historial ("Ver historial"), métrica estable del leaderboard (borrar y recrear no infla el conteo) y auditoría. `DELETE /api/partners/{id}` marca `deletedAt`.
4. **Avatares: emoji para parejas, foto opcional para el propio perfil.** Las parejas usan `avatarEmoji` (subir fotos de terceros abre problemas de consentimiento). El usuario sí puede subir su foto de perfil: se valida por bytes mágicos (JPG/PNG, ≤2 MB), se recorta a cuadrado y se reduce a 256 px JPEG (~20 KB, reencodear elimina el EXIF, GPS incluido), y se guarda en Postgres (`user_avatars`). Con ese tamaño cabe en cualquier tier gratuito de BD y no ata el despliegue a un proveedor de storage; `AvatarService` es el único punto a cambiar si algún día se migra a Cloudinary/R2. La foto solo se sirve a su dueño (`/api/users/me/avatar`).
5. **404 en vez de 403 para recursos ajenos.** Si un usuario pide `/api/partners/{id}` de otro, la respuesta es 404. Un 403 confirma que el ID existe y facilita enumeración.
6. **Check-in por día.** Un check-in por autor, relación y fecha (constraint único). Repetirlo el mismo día lo actualiza. Simplifica "¿cómo te sientes hoy?" y las consultas del dashboard.

## 3. Esquema de base de datos

Migraciones Flyway en `backend/src/main/resources/db/migration`.

- `V1__schema.sql` — todas las tablas, FKs, constraints e índices.
- `V2__daily_tips.sql` — catálogo de tips (claves i18n).

Tablas: `users`, `refresh_tokens`, `partners`, `relationships`, `relationship_milestones`, `cycle_profiles`, `period_records`, `relationship_check_ins`, `partner_observations`, `partner_access`, `daily_tips`, `leaderboard_profiles`.

Puntos relevantes:

- UUID como PK (generadas por la app, `gen_random_uuid()` como default).
- `partners.normalized_name` + índice único parcial `(owner_user_id, normalized_name) WHERE deleted_at IS NULL`: impide duplicar la misma pareja activa y alimenta el conteo único del leaderboard.
- `relationship_check_ins`: único `(relationship_id, author_user_id, check_in_date)`.
- `period_records`: único `(cycle_profile_id, start_date)`; checks de rango en longitudes de ciclo (15–60) y periodo (1–12).
- `timestamptz` para timestamps, `date` para fechas de calendario (los ciclos y aniversarios son fechas civiles, no instantes).

## 4. Reglas de negocio (dominio, no frontend)

Implementadas en `RelationshipRules` y aplicadas por `PartnerService` al crear, editar o reactivar:

1. Tipos *románticos*: DATING, SERIOUS_RELATIONSHIP, MONOGAMOUS, ENGAGED, MARRIED, POLYAMOROUS. No románticos: CASUAL, FRIENDS_WITH_BENEFITS, OTHER.
2. Si el usuario tiene una relación **MONOGAMOUS activa**, no puede crear ni reactivar otra relación romántica activa (y viceversa: no puede marcar una relación como MONOGAMOUS si ya hay otra romántica activa).
3. Máximo **un matrimonio activo** (type MARRIED, status ACTIVE) salvo que el usuario sea POLYAMOROUS.
4. Coherencia de fechas: `datingStartDate ≤ relationshipStartDate ≤ engagementDate ≤ marriageDate` cuando existan; `relationshipEndDate` requiere status ENDED y no puede ser anterior al inicio.
5. Finalizar una relación fija status ENDED y `relationshipEndDate`; reactivar la limpia y vuelve a validar las reglas 2–3.
6. La situación sentimental del usuario (SINGLE, IN_RELATIONSHIP, MARRIED, POLYAMOROUS, OTHER) es autodeclarada y ajusta la UI (p. ej. leaderboard pensado para SINGLE), pero las restricciones duras se derivan de los tipos de relación configurados, que es lo que el usuario declara explícitamente por pareja.

## 5. Privacidad y permisos

Modelo en dos niveles:

**Nivel 1 — Ownership (MVP completo).** Todo recurso cuelga de un Partner, y todo Partner tiene owner. Cada servicio de aplicación resuelve el recurso *filtrando por el usuario autenticado en la query* (`findByIdAndOwnerUserId...`), nunca comprobando después. Si no hay fila: 404. Esto elimina IDOR por construcción.

**Nivel 2 — Acceso compartido por consentimiento.** Una pareja puede ser una cuenta real (`linkedUserId`). En ese caso `partner_access` guarda grants por scope:

- `CYCLE` — ver perfil de ciclo, periodos y predicciones.
- `CHECK_INS` — ver los check-ins que la persona vinculada escribió.

Quien concede es siempre el dueño del dato (`grantedByUserId`) y puede revocar (status REVOKED) en cualquier momento. La comprobación vive en `PartnerAccessService.canView(userId, partnerId, scope)` y se aplica en el backend; el frontend solo decide qué pintar.

Reglas fijas independientes de grants:

- Los check-ins solo los ve su autor, salvo grant explícito CHECK_INS.
- Las observaciones (`PartnerObservation`) solo las ve quien las escribió. Son percepciones del observador; mostrárselas a la persona observada crea más problemas de los que resuelve, así que en el MVP ni siquiera hay opción de compartirlas.
- El leaderboard solo expone: alias público, emoji de avatar (si `showAvatar`) y score agregado. Nada de nombres de parejas, fechas ni notas.
- El registro manual de una pareja muestra un aviso de consentimiento: el usuario declara que tiene permiso de esa persona para guardar sus datos.

## 6. API REST

Prefijo `/api`. Autenticación por `Authorization: Bearer <access token>`. Errores en formato Problem Details (RFC 7807) sin stack traces.

```
POST   /api/auth/register           # crea cuenta, devuelve tokens
POST   /api/auth/login
POST   /api/auth/refresh            # rota el refresh token
POST   /api/auth/logout             # revoca el refresh token

GET    /api/users/me
PATCH  /api/users/me                # displayName, preferredLanguage, situation...
PATCH  /api/users/me/password

GET    /api/partners                # ?status=ACTIVE|ENDED|ALL (excluye borradas)
POST   /api/partners
GET    /api/partners/{id}
PATCH  /api/partners/{id}
DELETE /api/partners/{id}           # soft delete
GET    /api/partners/history        # incluye finalizadas y borradas (solo owner)

GET    /api/partners/{id}/relationship
PATCH  /api/partners/{id}/relationship   # tipo, estado, fechas; end/reactivate

GET    /api/partners/{id}/milestones
POST   /api/partners/{id}/milestones
PATCH  /api/milestones/{id}
DELETE /api/milestones/{id}

GET    /api/partners/{id}/cycle
PATCH  /api/partners/{id}/cycle
GET    /api/partners/{id}/periods
POST   /api/partners/{id}/periods        # inicio hoy o fecha pasada
PATCH  /api/periods/{id}
DELETE /api/periods/{id}
GET    /api/partners/{id}/cycle/predictions?from=YYYY-MM-DD&to=YYYY-MM-DD

POST   /api/check-ins                    # self check-in sobre una relación
GET    /api/partners/{id}/check-ins      # propios + compartidos si hay grant

POST   /api/partners/{id}/observations
GET    /api/partners/{id}/observations   # solo del observador autenticado

GET    /api/partners/{id}/advice/today

GET    /api/dashboard                    # composición: parejas + pulso diario

GET    /api/leaderboard?window=GLOBAL|MONTH|YEAR
GET    /api/leaderboard/me
PATCH  /api/leaderboard/me/settings

GET    /api/stats/me
```

Cambios sobre la propuesta del enunciado: se añade `logout`, `users/me/password`, `partners/history` y el parámetro `window` del leaderboard; `PATCH /api/partners/{id}/relationship` absorbe finalizar/reactivar (un solo endpoint de estado en lugar de verbos ad hoc).

## 7. Predicción de ciclo

`CyclePredictionService` (dominio puro):

- **Longitud efectiva del ciclo**: si hay ≥ 3 inicios de periodo consecutivos, media de los últimos hasta 6 intervalos, descartando outliers fuera de 15–60 días; si no, `averageCycleLength` del perfil.
- **Próxima menstruación** = último inicio + longitud efectiva (avanzando ciclos completos si la fecha quedó en el pasado).
- **Ovulación estimada** = próxima menstruación − 14 días (aproximación estándar de fase lútea fija).
- **Ventana fértil** = ovulación − 5 … ovulación + 1.
- **Fase por día**: MENSTRUATION (días de periodo), OVULATION (ovulación ± 1), FOLLICULAR (entre fin de periodo y ventana de ovulación), LUTEAL (resto).

Toda respuesta de predicción incluye `disclaimerKeys` y la UI muestra siempre: "Estimación basada en los ciclos registrados" y "No debe usarse como método anticonceptivo ni diagnóstico". Sin datos suficientes, el endpoint responde con `insufficientData: true` y la UI explica qué falta.

## 8. RelationshipAdviceService

Motor de reglas determinista, sin ML y sin inventar emociones:

1. Construye un `AdviceContext`: etapa de la relación (NEW → VERY_LONG_TERM por duración), tipo, días hasta el aniversario, fase estimada (si tracking activo), último check-in del propio usuario, último check-in de la pareja *solo si es cuenta vinculada con grant*, última observación registrada.
2. Una lista ordenada de `AdviceRule` evalúa el contexto y emite candidatos `(categoría, clave i18n, parámetros, prioridad, fuente)`. Fuente ∈ SELF_REPORT | OBSERVATION | CYCLE | RELATIONSHIP, para que la UI diga "Laura indicó que..." vs "Registraste que notas a Laura...".
3. Se devuelven los 1–3 candidatos de mayor prioridad; los empates se resuelven con un random sembrado por `(partnerId, fecha)` para que el consejo del día sea estable durante el día.
4. Los textos son claves i18n (`advice.anniversary.upcoming`, ...) resueltas en el frontend con parámetros (`{name}`, `{days}`), así ES/EN salen gratis.

El catálogo genérico (`daily_tips`) aporta los consejos por etapa y por fase cuando ninguna regla contextual dispara. Ninguna clave de fase afirma estados emocionales ("estará irritable"); son sugerencias neutras de cuidado y comunicación.

## 9. Leaderboard

- Opt-in estricto: sin `leaderboard_profiles.enabled = true` **y** alias definido, el usuario no aparece. El endpoint público solo lee perfiles habilitados.
- **Métrica**: `uniquePartnerCount` = número de `normalized_name` distintos entre las parejas del usuario, *incluyendo* soft-deleted. Borrar y recrear la misma persona no suma; registrar la misma pareja dos veces no suma.
- Ventanas GLOBAL / MONTH / YEAR: se cuenta el primer registro de cada nombre normalizado (min(created_at)) dentro de la ventana.
- Anti-cheat MVP: nombre normalizado único, soft delete (no se puede "resetear" borrando), y rate limit de creación de parejas (10/día por usuario) aplicado en el servicio.
- La arquitectura deja espacio para más métricas (activePartnerCount, longestRelationship...) porque el cálculo vive en `LeaderboardService` sobre queries agregadas, no en contadores mutables.

## 10. Autenticación

- Access token: JWT firmado HS256 (jjwt), 15 min, claims mínimos (sub = userId).
- Refresh token: opaco (256 bits aleatorios), guardado **hasheado** (SHA-256) en `refresh_tokens`, 30 días, rotación en cada refresh, revocación en logout. Permite invalidar sesiones desde el servidor, cosa que un refresh-JWT stateless no permite.
- Contraseñas con BCrypt (fuerza 12). Validación de password ≥ 8 caracteres.
- El frontend guarda los tokens en localStorage en el MVP; el README documenta la migración recomendada a cookies httpOnly para producción.

## 11. Plan de implementación por fases

1. **F1 Infraestructura** — esqueleto backend, Flyway V1/V2, seguridad JWT, manejo de errores, registro/login/refresh/logout, perfil de usuario. ✔ tests de auth.
2. **F2 Parejas y relaciones** — CRUD partners, relationship, reglas de dominio, milestones, cálculo de duración y aniversarios. ✔ tests de reglas (monogamia, matrimonio, fechas) y duración.
3. **F3 Ciclo** — cycle profiles, periods, `CyclePredictionService`, endpoint de predicciones. ✔ tests del servicio de predicción.
4. **F4 Señales** — check-ins, observaciones, `RelationshipAdviceService`, dashboard, stats. ✔ tests del motor de consejos y de autorización de check-ins.
5. **F5 Leaderboard** — perfiles, ranking por ventanas, anti-cheat. ✔ tests de privacidad del ranking.
6. **F6 Frontend** — design system, auth, dashboard, partners, calendario, check-in, stats, leaderboard, settings, i18n ES/EN, dark mode.
7. **F7 Empaquetado** — seed demo (perfil `demo`), Docker Compose, README, auditoría visual final.

Cada fase termina con `mvn test` (backend) o `tsc && vite build` (frontend) en verde.
