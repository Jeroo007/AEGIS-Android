import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field


# ---- Auth ----


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    full_name: str = Field(min_length=1, max_length=120)
    phone_number: str | None = None


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class UserOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: uuid.UUID
    email: str
    full_name: str
    phone_number: str | None
    role: str
    is_verified: bool
    created_at: datetime


class AuthResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "Bearer"
    expires_in: int
    user: UserOut


class RefreshRequest(BaseModel):
    refresh_token: str


# ---- Incidents ----


class CreateSosRequest(BaseModel):
    latitude: float | None = None
    longitude: float | None = None
    accuracy_meters: float | None = None
    type: str = "MANUAL_SOS"
    confidence: float | None = None
    notes: str | None = None
    is_demo: bool = False


class IncidentOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: uuid.UUID
    user_id: uuid.UUID
    type: str
    status: str
    severity: str
    latitude: float | None
    longitude: float | None
    accuracy_meters: float | None
    confidence: float | None
    notes: str | None
    created_at: datetime
    updated_at: datetime
    resolved_at: datetime | None
    assigned_patrol_id: uuid.UUID | None
    is_demo: bool


# ---- Location ----


class LocationUpdateRequest(BaseModel):
    latitude: float
    longitude: float
    accuracy_meters: float
    incident_id: uuid.UUID | None = None
    timestamp: int | None = None


# ---- Safe zones ----


class SafeZoneOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: uuid.UUID
    name: str
    type: str
    latitude: float
    longitude: float
    radius_meters: float
    phone_number: str | None
    address: str | None
    open_24h: bool
    verified: bool
    distance_meters: float | None = None
    eta_minutes: int | None = None
