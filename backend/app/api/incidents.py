import uuid

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..core.security import decode_access_token
from ..database import get_db
from ..models import Incident, Location, User
from ..schemas import CreateSosRequest, IncidentOut, LocationUpdateRequest
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

router = APIRouter(prefix="/api/v1", tags=["incidents"])
bearer = HTTPBearer(auto_error=False)


def current_user(
    creds: HTTPAuthorizationCredentials | None = Depends(bearer),
    db: Session = Depends(get_db),
) -> User:
    if not creds:
        raise HTTPException(status_code=401, detail="Missing bearer token")
    try:
        data = decode_access_token(creds.credentials)
    except ValueError:
        raise HTTPException(status_code=401, detail="Invalid token")
    user = db.get(User, data.get("sub"))
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return user


@router.post("/sos", response_model=IncidentOut)
def create_sos(
    payload: CreateSosRequest,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
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
    db.flush()

    # Set PostGIS point if coordinates provided
    if payload.latitude is not None and payload.longitude is not None:
        incident.location = f"POINT({payload.longitude} {payload.latitude})"

    db.commit()
    db.refresh(incident)
    return IncidentOut.model_validate(incident)


@router.get("/incidents/my", response_model=list[IncidentOut])
def my_incidents(
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    rows = db.scalars(
        select(Incident)
        .where(Incident.user_id == user.id)
        .order_by(Incident.created_at.desc())
    ).all()
    return [IncidentOut.model_validate(r) for r in rows]


@router.get("/incidents/{incident_id}", response_model=IncidentOut)
def get_incident(
    incident_id: uuid.UUID,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    incident = db.get(Incident, incident_id)
    if not incident or incident.user_id != user.id:
        raise HTTPException(status_code=404, detail="Incident not found")
    return IncidentOut.model_validate(incident)


@router.post("/sos/cancel")
def cancel_sos(
    incident_id: uuid.UUID,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    incident = db.get(Incident, incident_id)
    if not incident or incident.user_id != user.id:
        raise HTTPException(status_code=404, detail="Incident not found")
    incident.status = "CANCELLED"
    db.commit()
    return {"status": "cancelled"}


@router.post("/location")
def post_location(
    payload: LocationUpdateRequest,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    loc = Location(
        user_id=user.id,
        incident_id=payload.incident_id,
        latitude=payload.latitude,
        longitude=payload.longitude,
        accuracy_meters=payload.accuracy_meters,
        point=f"POINT({payload.longitude} {payload.latitude})",
    )
    db.add(loc)
    db.commit()
    return {"status": "ok"}
