"""
AEGIS Backend — SQLite edition with auto-seeded demo user.
Run:  python main.py
Open: http://localhost:8000
Login: demo@aegis.local / demo1234
"""

from __future__ import annotations

import math
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Annotated, Optional

import uvicorn
from fastapi import Depends, FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, HTMLResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from fastapi.staticfiles import StaticFiles
from jose import JWTError, jwt
from passlib.context import CryptContext
from pydantic import BaseModel, ConfigDict, EmailStr, Field
from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy import (
    Boolean,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    create_engine,
    func,
    select,
)
from sqlalchemy import func as sqlfunc
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column, sessionmaker


# =============================================================================
# CONFIG
# =============================================================================


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")
    DATABASE_URL: str = "sqlite:///./aegis.db"
    JWT_SECRET: str = "dev-secret-change-me-1234567890abcdef"
    JWT_REFRESH_SECRET: str = "dev-refresh-secret-0987654321fedcba"
    JWT_ALGORITHM: str = "HS256"
    JWT_ACCESS_MINUTES: int = 60
    JWT_REFRESH_DAYS: int = 30
    CORS_ORIGINS: str = "*"

    @property
    def cors_list(self) -> list[str]:
        return [o.strip() for o in self.CORS_ORIGINS.split(",") if o.strip()]


settings = Settings()


# =============================================================================
# DATABASE
# =============================================================================

engine = create_engine(
    settings.DATABASE_URL,
    connect_args=(
        {"check_same_thread": False}
        if settings.DATABASE_URL.startswith("sqlite")
        else {}
    ),
)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


class Base(DeclarativeBase):
    pass


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


DB = Annotated[Session, Depends(get_db)]


# =============================================================================
# MODELS (SQLite-compatible: IDs are strings)
# =============================================================================


def _uid() -> str:
    return str(uuid.uuid4())


class User(Base):
    __tablename__ = "users"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uid)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    full_name: Mapped[str] = mapped_column(String(120))
    phone_number: Mapped[Optional[str]] = mapped_column(String(32), nullable=True)
    role: Mapped[str] = mapped_column(String(32), default="USER")
    is_verified: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)


class EmergencyContact(Base):
    __tablename__ = "emergency_contacts"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uid)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), index=True)
    name: Mapped[str] = mapped_column(String(120))
    phone_number: Mapped[str] = mapped_column(String(32))
    relationship: Mapped[Optional[str]] = mapped_column(String(64), nullable=True)
    priority: Mapped[int] = mapped_column(Integer, default=0)
    verified: Mapped[bool] = mapped_column(Boolean, default=False)
    notify_on_sos: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)


class Incident(Base):
    __tablename__ = "incidents"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uid)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), index=True)
    type: Mapped[str] = mapped_column(String(32), index=True)
    status: Mapped[str] = mapped_column(String(32), default="CREATED", index=True)
    severity: Mapped[str] = mapped_column(String(16), default="MEDIUM")
    latitude: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    longitude: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    accuracy_meters: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    confidence: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    is_demo: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.utcnow, index=True
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.utcnow, onupdate=datetime.utcnow
    )
    resolved_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)


class Location(Base):
    __tablename__ = "locations"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), index=True)
    incident_id: Mapped[Optional[str]] = mapped_column(
        String(36), ForeignKey("incidents.id"), nullable=True
    )
    latitude: Mapped[float] = mapped_column(Float)
    longitude: Mapped[float] = mapped_column(Float)
    accuracy_meters: Mapped[float] = mapped_column(Float)
    timestamp: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.utcnow, index=True
    )


class SafeZone(Base):
    __tablename__ = "safe_zones"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uid)
    name: Mapped[str] = mapped_column(String(200))
    type: Mapped[str] = mapped_column(String(32), index=True)
    latitude: Mapped[float] = mapped_column(Float)
    longitude: Mapped[float] = mapped_column(Float)
    radius_meters: Mapped[float] = mapped_column(Float, default=100.0)
    phone_number: Mapped[Optional[str]] = mapped_column(String(32), nullable=True)
    address: Mapped[Optional[str]] = mapped_column(String(500), nullable=True)
    open_24h: Mapped[bool] = mapped_column(Boolean, default=False)
    verified: Mapped[bool] = mapped_column(Boolean, default=False)


# =============================================================================
# SCHEMAS
# =============================================================================


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    full_name: str = Field(min_length=1, max_length=120)
    phone_number: Optional[str] = None


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class RefreshRequest(BaseModel):
    refresh_token: str


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    email: str
    full_name: str
    phone_number: Optional[str]
    role: str
    is_verified: bool
    created_at: datetime


class AuthResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "Bearer"
    expires_in: int
    user: UserOut


class CreateSosRequest(BaseModel):
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    accuracy_meters: Optional[float] = None
    type: str = "MANUAL_SOS"
    confidence: Optional[float] = None
    notes: Optional[str] = None
    is_demo: bool = False


class IncidentOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    user_id: str
    type: str
    status: str
    severity: str
    latitude: Optional[float]
    longitude: Optional[float]
    accuracy_meters: Optional[float]
    confidence: Optional[float]
    notes: Optional[str]
    created_at: datetime
    updated_at: datetime
    resolved_at: Optional[datetime]
    is_demo: bool


class LocationUpdateRequest(BaseModel):
    latitude: float
    longitude: float
    accuracy_meters: float
    incident_id: Optional[str] = None


class SafeZoneOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: str
    name: str
    type: str
    latitude: float
    longitude: float
    radius_meters: float
    phone_number: Optional[str]
    address: Optional[str]
    open_24h: bool
    verified: bool
    distance_meters: Optional[float] = None
    eta_minutes: Optional[int] = None


# =============================================================================
# SECURITY
# =============================================================================

pwd = CryptContext(schemes=["bcrypt"], deprecated="auto")
bearer = HTTPBearer(auto_error=False)


def hash_password(raw: str) -> str:
    return pwd.hash(raw)


def verify_password(raw: str, hashed: str) -> bool:
    try:
        return pwd.verify(raw, hashed)
    except Exception:
        return False


def make_access(user_id: str, role: str) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "sub": user_id,
        "role": role,
        "type": "access",
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(minutes=settings.JWT_ACCESS_MINUTES)).timestamp()),
    }
    return jwt.encode(payload, settings.JWT_SECRET, algorithm=settings.JWT_ALGORITHM)


def make_refresh(user_id: str) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "sub": user_id,
        "type": "refresh",
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(days=settings.JWT_REFRESH_DAYS)).timestamp()),
    }
    return jwt.encode(
        payload, settings.JWT_REFRESH_SECRET, algorithm=settings.JWT_ALGORITHM
    )


def current_user(
    creds: Optional[HTTPAuthorizationCredentials] = Depends(bearer),
    db: Session = Depends(get_db),
) -> User:
    if not creds:
        raise HTTPException(status_code=401, detail="Missing bearer token")
    try:
        data = jwt.decode(
            creds.credentials, settings.JWT_SECRET, algorithms=[settings.JWT_ALGORITHM]
        )
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")
    user = db.get(User, data.get("sub"))
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return user


CurrentUser = Annotated[User, Depends(current_user)]


# =============================================================================
# APP
# =============================================================================

app = FastAPI(title="AEGIS API", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

STATIC_DIR = Path(__file__).parent / "static"
if STATIC_DIR.exists():
    app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")


# =============================================================================
# AUTH ENDPOINTS
# =============================================================================


def _auth_response(user: User) -> AuthResponse:
    return AuthResponse(
        access_token=make_access(str(user.id), user.role),
        refresh_token=make_refresh(str(user.id)),
        expires_in=settings.JWT_ACCESS_MINUTES * 60,
        user=UserOut.model_validate(user),
    )


@app.post("/api/v1/auth/register", response_model=AuthResponse)
def register(payload: RegisterRequest, db: DB):
    if db.scalar(select(User).where(User.email == payload.email)):
        raise HTTPException(409, "Email already registered")
    user = User(
        email=payload.email,
        password_hash=hash_password(payload.password),
        full_name=payload.full_name,
        phone_number=payload.phone_number,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return _auth_response(user)


@app.post("/api/v1/auth/login", response_model=AuthResponse)
def login(payload: LoginRequest, db: DB):
    user = db.scalar(select(User).where(User.email == payload.email))
    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(401, "Invalid credentials")
    return _auth_response(user)


@app.post("/api/v1/auth/refresh", response_model=AuthResponse)
def refresh(payload: RefreshRequest, db: DB):
    try:
        data = jwt.decode(
            payload.refresh_token,
            settings.JWT_REFRESH_SECRET,
            algorithms=[settings.JWT_ALGORITHM],
        )
    except JWTError:
        raise HTTPException(401, "Invalid refresh token")
    user = db.get(User, data.get("sub"))
    if not user:
        raise HTTPException(401, "User not found")
    return _auth_response(user)


@app.post("/api/v1/auth/logout")
def logout():
    return {"status": "ok"}


@app.get("/api/v1/users/me", response_model=UserOut)
def me(user: CurrentUser):
    return UserOut.model_validate(user)


# =============================================================================
# SOS + INCIDENTS
# =============================================================================


@app.post("/api/v1/sos", response_model=IncidentOut)
def create_sos(payload: CreateSosRequest, user: CurrentUser, db: DB):
    severity = "CRITICAL" if payload.type == "MANUAL_SOS" else "HIGH"
    incident = Incident(
        user_id=user.id,
        type=payload.type,
        status="CREATED",
        severity=severity,
        latitude=payload.latitude,
        longitude=payload.longitude,
        accuracy_meters=payload.accuracy_meters,
        confidence=payload.confidence,
        notes=payload.notes,
        is_demo=payload.is_demo,
    )
    db.add(incident)
    db.commit()
    db.refresh(incident)
    return IncidentOut.model_validate(incident)


@app.get("/api/v1/incidents/my", response_model=list[IncidentOut])
def my_incidents(user: CurrentUser, db: DB):
    rows = db.scalars(
        select(Incident)
        .where(Incident.user_id == user.id)
        .order_by(Incident.created_at.desc())
        .limit(100)
    ).all()
    return [IncidentOut.model_validate(r) for r in rows]


@app.get("/api/v1/incidents/{incident_id}", response_model=IncidentOut)
def get_incident(incident_id: str, user: CurrentUser, db: DB):
    inc = db.get(Incident, incident_id)
    if not inc or inc.user_id != user.id:
        raise HTTPException(404, "Incident not found")
    return IncidentOut.model_validate(inc)


@app.post("/api/v1/sos/cancel")
def cancel_sos(incident_id: str, user: CurrentUser, db: DB):
    inc = db.get(Incident, incident_id)
    if not inc or inc.user_id != user.id:
        raise HTTPException(404, "Incident not found")
    inc.status = "CANCELLED"
    db.commit()
    return {"status": "cancelled"}


@app.post("/api/v1/location")
def post_location(payload: LocationUpdateRequest, user: CurrentUser, db: DB):
    db.add(
        Location(
            user_id=user.id,
            incident_id=payload.incident_id,
            latitude=payload.latitude,
            longitude=payload.longitude,
            accuracy_meters=payload.accuracy_meters,
        )
    )
    db.commit()
    return {"status": "ok"}


# =============================================================================
# SAFE ZONES
# =============================================================================


def haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return r * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


@app.get("/api/v1/safe-zones/nearest", response_model=list[SafeZoneOut])
def nearest_safe_zones(
    lat: float = Query(..., ge=-90, le=90),
    lon: float = Query(..., ge=-180, le=180),
    radius_meters: float = Query(15_000, gt=0, le=100_000),
    limit: int = Query(30, ge=1, le=100),
    db: Session = Depends(get_db),
):
    zones = db.scalars(select(SafeZone)).all()
    scored = [(haversine_m(lat, lon, z.latitude, z.longitude), z) for z in zones]
    scored = [(d, z) for d, z in scored if d <= radius_meters]
    scored.sort(key=lambda x: x[0])
    out: list[SafeZoneOut] = []
    for d, z in scored[:limit]:
        item = SafeZoneOut.model_validate(z)
        item.distance_meters = round(d, 1)
        item.eta_minutes = max(1, round(d / 80))
        out.append(item)
    return out


@app.get("/api/v1/safe-zones", response_model=list[SafeZoneOut])
def list_safe_zones(db: DB):
    rows = db.scalars(select(SafeZone).order_by(SafeZone.name)).all()
    return [SafeZoneOut.model_validate(r) for r in rows]


# =============================================================================
# ADMIN / DASHBOARD
# =============================================================================


@app.get("/api/v1/admin/stats")
def admin_stats(db: DB):
    return {
        "total_users": db.scalar(select(sqlfunc.count()).select_from(User)) or 0,
        "total_incidents": db.scalar(select(sqlfunc.count()).select_from(Incident))
        or 0,
        "active_incidents": db.scalar(
            select(sqlfunc.count())
            .select_from(Incident)
            .where(
                Incident.status.in_(
                    ["CREATED", "ACKNOWLEDGED", "DISPATCHED", "PATROL_ASSIGNED"]
                )
            )
        )
        or 0,
        "resolved_incidents": db.scalar(
            select(sqlfunc.count())
            .select_from(Incident)
            .where(Incident.status == "RESOLVED")
        )
        or 0,
        "total_safe_zones": db.scalar(select(sqlfunc.count()).select_from(SafeZone))
        or 0,
    }


@app.get("/api/v1/admin/incidents")
def admin_incidents(db: DB, limit: int = 50):
    rows = db.scalars(
        select(Incident).order_by(Incident.created_at.desc()).limit(limit)
    ).all()
    users_by_id = {u.id: u for u in db.scalars(select(User)).all()}
    return [
        {
            "id": str(r.id),
            "user_name": (
                users_by_id[r.user_id].full_name
                if r.user_id in users_by_id
                else "Unknown"
            ),
            "user_email": (
                users_by_id[r.user_id].email if r.user_id in users_by_id else "—"
            ),
            "type": r.type,
            "status": r.status,
            "severity": r.severity,
            "latitude": r.latitude,
            "longitude": r.longitude,
            "accuracy_meters": r.accuracy_meters,
            "confidence": r.confidence,
            "notes": r.notes,
            "is_demo": r.is_demo,
            "created_at": r.created_at.isoformat() if r.created_at else None,
            "resolved_at": r.resolved_at.isoformat() if r.resolved_at else None,
        }
        for r in rows
    ]


@app.get("/api/v1/admin/users")
def admin_users(db: DB):
    rows = db.scalars(select(User).order_by(User.created_at.desc()).limit(200)).all()
    return [
        {
            "id": str(u.id),
            "email": u.email,
            "full_name": u.full_name,
            "phone_number": u.phone_number,
            "role": u.role,
            "is_verified": u.is_verified,
            "created_at": u.created_at.isoformat() if u.created_at else None,
        }
        for u in rows
    ]


@app.get("/api/v1/admin/safe-zones")
def admin_safe_zones(db: DB):
    rows = db.scalars(select(SafeZone).order_by(SafeZone.name)).all()
    return [
        {
            "id": str(z.id),
            "name": z.name,
            "type": z.type,
            "latitude": z.latitude,
            "longitude": z.longitude,
            "phone_number": z.phone_number,
            "address": z.address,
            "open_24h": z.open_24h,
            "verified": z.verified,
        }
        for z in rows
    ]


@app.post("/api/v1/admin/incidents/{incident_id}/resolve")
def admin_resolve_incident(incident_id: str, db: DB):
    inc = db.get(Incident, incident_id)
    if not inc:
        raise HTTPException(404, "Incident not found")
    inc.status = "RESOLVED"
    inc.resolved_at = datetime.utcnow()
    db.commit()
    return {"status": "resolved"}


# =============================================================================
# HEALTH + ROOT
# =============================================================================


@app.get("/health")
def health():
    return {"status": "ok", "service": "aegis-backend"}


@app.get("/", response_class=HTMLResponse)
def root():
    index = STATIC_DIR / "index.html"
    if index.exists():
        return FileResponse(str(index))
    return HTMLResponse(
        "<!doctype html><html><body style='font-family:sans-serif;padding:40px;"
        "background:#0B1220;color:#fff'><h1>AEGIS Backend is running</h1>"
        "<p>Dashboard not found. Create <code>static/index.html</code>.</p>"
        "<p>API docs: <a style='color:#22D3EE' href='/docs'>/docs</a></p>"
        "</body></html>"
    )


# =============================================================================
# SEED DATA
# =============================================================================

SEED_ZONES = [
    (
        "Coimbatore City Police Commissioner Office",
        "POLICE_STATION",
        11.0067,
        76.9653,
        "100",
        True,
    ),
    ("Race Course Police Station", "POLICE_STATION", 11.0068, 76.9714, "100", True),
    ("RS Puram Police Station", "POLICE_STATION", 11.0147, 76.9497, "100", True),
    ("Gandhipuram Police Station", "POLICE_STATION", 11.0183, 76.9675, "100", True),
    ("Town Hall Police Station", "POLICE_STATION", 10.9972, 76.9625, "100", True),
    ("Kattoor Police Station", "POLICE_STATION", 11.0061, 76.9514, "100", True),
    ("Ramanathapuram Police Station", "POLICE_STATION", 10.9925, 76.9812, "100", True),
    ("Singanallur Police Station", "POLICE_STATION", 10.9961, 77.0152, "100", True),
    ("Peelamedu Police Station", "POLICE_STATION", 11.0278, 77.0003, "100", True),
    ("Ukkadam Police Station", "POLICE_STATION", 10.9884, 76.9497, "100", True),
    ("Saibaba Colony Police Station", "POLICE_STATION", 11.0242, 76.9497, "100", True),
    ("Thudiyalur Police Station", "POLICE_STATION", 11.0811, 76.9425, "100", True),
    ("Saravanampatti Police Station", "POLICE_STATION", 11.0800, 77.0003, "100", True),
    ("Vadavalli Police Station", "POLICE_STATION", 11.0261, 76.8942, "100", True),
    ("Sulur Police Station", "POLICE_STATION", 11.0261, 77.1231, "100", True),
    ("Mettupalayam Police Station", "POLICE_STATION", 11.2989, 76.9353, "100", True),
    ("Pollachi Police Station", "POLICE_STATION", 10.6583, 77.0083, "100", True),
    ("Valparai Police Station", "POLICE_STATION", 10.3242, 76.9533, "100", True),
    (
        "Coimbatore Medical College Hospital",
        "HOSPITAL",
        11.0056,
        76.9755,
        "0422-2301393",
        True,
    ),
    ("KG Hospital", "HOSPITAL", 11.0139, 76.9653, "0422-2227771", True),
    ("PSG Hospitals", "HOSPITAL", 11.0178, 77.0031, "0422-4345678", True),
    (
        "Kovai Medical Center & Hospital",
        "HOSPITAL",
        11.0281,
        77.0408,
        "0422-4323800",
        True,
    ),
    ("Sri Ramakrishna Hospital", "HOSPITAL", 11.0167, 76.9814, "0422-4500000", True),
    ("Ganga Hospital", "HOSPITAL", 11.0206, 76.9666, "0422-2485000", True),
    ("Royal Care Hospital", "HOSPITAL", 11.0539, 77.0322, "0422-4208888", True),
    ("Aravind Eye Hospital", "HOSPITAL", 11.0175, 76.9900, "0422-4222222", True),
    ("Sankara Eye Hospital", "HOSPITAL", 11.0242, 77.0297, "0422-2304040", True),
    (
        "Government Hospital Coimbatore",
        "HOSPITAL",
        10.9961,
        76.9608,
        "0422-2301298",
        True,
    ),
    ("ESI Hospital", "HOSPITAL", 11.0042, 76.9889, "0422-2571010", True),
    ("Masonic Medical Centre", "HOSPITAL", 11.0203, 76.9542, "0422-2222222", True),
    (
        "Government Hospital Pollachi",
        "HOSPITAL",
        10.6592,
        77.0089,
        "04259-222222",
        True,
    ),
    (
        "Government Hospital Mettupalayam",
        "HOSPITAL",
        11.2989,
        76.9420,
        "04254-222222",
        True,
    ),
    ("Gandhipuram Bus Stand", "TRANSIT", 11.0178, 76.9672, None, True),
    ("Coimbatore Junction Railway Station", "TRANSIT", 10.9975, 76.9662, "139", True),
    (
        "Coimbatore International Airport",
        "TRANSIT",
        11.0300,
        77.0434,
        "0422-2598100",
        True,
    ),
    ("Ukkadam Bus Stand", "TRANSIT", 10.9884, 76.9514, None, True),
]


def seed_safe_zones() -> None:
    db = SessionLocal()
    try:
        if db.scalar(select(SafeZone).limit(1)):
            print("[seed] safe zones already present")
            return
        for name, ztype, lat, lon, phone, open24 in SEED_ZONES:
            db.add(
                SafeZone(
                    name=name,
                    type=ztype,
                    latitude=lat,
                    longitude=lon,
                    radius_meters=100.0,
                    phone_number=phone,
                    open_24h=open24,
                    verified=True,
                )
            )
        db.commit()
        print(f"[seed] inserted {len(SEED_ZONES)} safe zones")
    finally:
        db.close()


def seed_demo_user() -> None:
    """Auto-create the demo account so login from the app always works."""
    db = SessionLocal()
    try:
        existing = db.scalar(select(User).where(User.email == "demo@aegis.local"))
        if existing:
            print("[seed] demo user already exists")
            return
        user = User(
            email="demo@aegis.local",
            password_hash=hash_password("demo1234"),
            full_name="Demo User",
            phone_number="9566168246",
            is_verified=True,
        )
        db.add(user)
        db.commit()
        print("[seed] demo user created: demo@aegis.local / demo1234")
    finally:
        db.close()


# =============================================================================
# STARTUP
# =============================================================================


@app.on_event("startup")
def on_startup() -> None:
    print("[aegis] creating tables…")
    Base.metadata.create_all(bind=engine)
    print("[aegis] tables ready")
    seed_safe_zones()
    seed_demo_user()
    print("[aegis] ready → http://localhost:8000")


# =============================================================================
# ENTRYPOINT
# =============================================================================

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
