# 🍑 CulitosTracker

Aplicación web para llevar, con consentimiento, el seguimiento del ciclo menstrual de tu pareja (o parejas), las fechas importantes de la relación, check-ins emocionales de ambos y sugerencias diarias según el contexto. Incluye un leaderboard opcional y anónimo para solteros.

Las predicciones de ciclo son estimaciones calculadas a partir de los datos registrados. No son información médica y no sirven como método anticonceptivo.

## Stack

| Capa | Tecnología |
|---|---|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS 4, React Router 7, TanStack Query 5, i18next |
| Backend | Java 21, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, Bean Validation |
| Base de datos | PostgreSQL 16, migraciones con Flyway |
| Local | Docker Compose |

La arquitectura completa, el modelo de dominio y las decisiones de diseño están en [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Requisitos

- Docker + Docker Compose (suficiente para levantar todo), o bien
- JDK 21+, Maven 3.9+ y Node 20+ para desarrollo fuera de Docker.

## Arranque rápido (todo en Docker)

```bash
cp .env.example .env   # revisa JWT_SECRET y la contraseña de Postgres
docker compose up --build
```

- Frontend: http://localhost:5173
- API: http://localhost:8080
- Postgres: localhost:5432

## Desarrollo (Postgres en Docker, apps en local)

Es el modo más cómodo para iterar: hot reload en ambos lados.

```bash
docker compose up -d db
```

Backend (el perfil `demo` carga datos de prueba; no usarlo en producción):

```bash
cd backend
SPRING_PROFILES_ACTIVE=demo DB_PASSWORD=change-me-locally mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Usuario demo: `demo@culitostracker.local` / `demo1234` (también `laura@culitostracker.local` con la misma contraseña, para ver el lado de la cuenta vinculada).

## Variables de entorno

Todas están documentadas en [.env.example](.env.example). Las importantes:

- `JWT_SECRET`: firma de los access tokens. Genera uno real con `openssl rand -base64 48`. El valor por defecto solo vale para desarrollo.
- `DB_URL` / `DB_USER` / `DB_PASSWORD`: conexión de la API a Postgres.
- `CORS_ALLOWED_ORIGINS`: orígenes permitidos, separados por comas.
- `SPRING_PROFILES_ACTIVE`: añade `demo` para el seed de desarrollo.
- `VITE_API_URL`: URL del backend que usa el frontend (se hornea en el build).

Ningún secreto va al repositorio; `.env` está en `.gitignore`.

## Tests

Backend (unitarios + integración con Testcontainers, necesita Docker corriendo):

```bash
cd backend
mvn test
```

Frontend (Vitest):

```bash
cd frontend
npm test
```

Los tests de integración levantan un Postgres real y cubren, entre otros: IDOR (un usuario no puede leer parejas ajenas ni adivinando IDs), privacidad de check-ins con y sin grant de consentimiento, opt-in del leaderboard y la regla de monogamia aplicada por la API. Un test del frontend verifica que todas las claves i18n que emite el backend (consejos, tips, errores) existen en `es.json` y `en.json`.

## Estructura del proyecto

```
backend/src/main/java/com/culitostracker/
├── domain/model      # Entidades y enums
├── domain/service    # Lógica pura: predicción de ciclo, motor de consejos,
│                     # reglas de relación, duración/aniversarios
├── application/      # Servicios de aplicación + autorización
├── repository/       # Spring Data JPA
├── api/ + api/dto    # Controladores REST y DTOs
└── infrastructure/   # Seguridad JWT, errores, configuración, seed demo

frontend/src/
├── app/              # Shell de navegación, guards, rutas
├── components/ui/    # Sistema de diseño (Button, Field, Sheet, Toast…)
├── features/         # Una carpeta por funcionalidad
├── i18n/             # es.json / en.json
├── lib/              # Cliente API, auth, fechas, tipos
└── styles/           # Tokens del design system (light + dark)
```

## Modelo de privacidad

- **Consentimiento primero.** Registrar manualmente a una persona exige confirmar que tienes su consentimiento. Si esa persona tiene cuenta vinculada, cada dato compartido (ciclo, check-ins) requiere un grant explícito por scope, revocable en cualquier momento.
- **Autorización en el backend.** Cada query filtra por el usuario autenticado; un recurso ajeno responde 404 (no 403, para no confirmar que el ID existe). Ocultar cosas en la interfaz nunca es la única barrera.
- **Check-ins**: solo los ve su autor salvo grant explícito, y las notas de texto libre no se comparten nunca.
- **Observaciones** ("cómo notas a tu pareja"): son percepciones privadas del observador. La interfaz siempre dice "registraste que la notas..." y nunca "ella está...".
- **Leaderboard**: estrictamente opt-in. Solo publica alias, emoji opcional y un número agregado; jamás nombres de parejas, fechas ni notas.

## Cómo funcionan las predicciones

1. La longitud efectiva del ciclo es la media de los últimos intervalos registrados (hasta 6, descartando valores fuera de 15–60 días). Con menos de 3 registros se usa la media configurada en el perfil (28 por defecto, ajustable: no todos los ciclos duran 28 días).
2. Próxima menstruación = último inicio + longitud efectiva, avanzando ciclos completos si quedó en el pasado.
3. Ovulación estimada = próxima menstruación − 14 días (aproximación de fase lútea fija). Ventana fértil = ovulación −5 … +1.
4. Cada día se clasifica en una fase (menstruación, folicular, ovulación, lútea) para el calendario y los consejos.

Toda respuesta de predicción lleva sus avisos: es una estimación y no sirve como anticonceptivo ni diagnóstico.

## Consejos diarios

`RelationshipAdviceService` construye un contexto con señales registradas (nunca inventadas): duración y tipo de la relación, aniversarios próximos, fase estimada, tu último check-in, el de tu pareja si lo compartió, y tu última observación. Un motor de reglas determinista produce 1–3 sugerencias como claves i18n con parámetros, etiquetadas por origen para que la interfaz distinga "Laura indicó que..." de "Registraste que notas a Laura...". Los textos genéricos salen de un catálogo por etapa de la relación y por fase del ciclo.
