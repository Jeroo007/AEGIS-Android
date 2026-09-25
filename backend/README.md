# AEGIS Backend

FastAPI + PostgreSQL (PostGIS) + Redis.

## Run

```bash
cd backend
cp .env.example .env       # edit JWT secrets
docker compose up -d
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Open http://localhost:8000/docs for the interactive API explorer.

## Test the flow

```bash
# Register
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"a@b.com","password":"hunter2hunter2","full_name":"Test User"}'

# Save the access_token, then:
curl -X POST http://localhost:8000/api/v1/sos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"MANUAL_SOS","latitude":11.0168,"longitude":76.9558,"accuracy_meters":15}'
```

## Connect from Android

In `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_BASE_URL",
    "\"http://10.0.2.2:8000/\"")  // emulator → host
```

Rebuild the app. Register/login through the UI. The app will now hit your local backend.

## Next steps

- Add Alembic migrations (`alembic init alembic`)
- Add the WebSocket channels `/ws/user`, `/ws/dispatcher`, `/ws/patrol`
- Add the dispatcher dashboard (Next.js or Vite + React)
