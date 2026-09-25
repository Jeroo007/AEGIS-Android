from fastapi import APIRouter, Depends, Query
from sqlalchemy import text
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import SafeZone
from ..schemas import SafeZoneOut

router = APIRouter(prefix="/api/v1/safe-zones", tags=["safe-zones"])


@router.get("/nearest", response_model=list[SafeZoneOut])
def nearest(
    lat: float = Query(..., ge=-90, le=90),
    lon: float = Query(..., ge=-180, le=180),
    radius_meters: float = Query(15_000, gt=0, le=100_000),
    limit: int = Query(30, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """
    Nearest safe zones using PostGIS ST_DistanceSphere (great-circle meters).
    """
    sql = text(
        """
        SELECT
            id, name, type, latitude, longitude, radius_meters,
            phone_number, address, open_24h, verified,
            ST_DistanceSphere(
                ST_MakePoint(longitude, latitude),
                ST_MakePoint(:lon, :lat)
            ) AS distance_meters
        FROM safe_zones
        WHERE ST_DistanceSphere(
            ST_MakePoint(longitude, latitude),
            ST_MakePoint(:lon, :lat)
        ) <= :radius
        ORDER BY distance_meters ASC
        LIMIT :lim
    """
    )
    rows = (
        db.execute(
            sql,
            {"lat": lat, "lon": lon, "radius": radius_meters, "lim": limit},
        )
        .mappings()
        .all()
    )

    out: list[SafeZoneOut] = []
    for r in rows:
        distance = float(r["distance_meters"])
        out.append(
            SafeZoneOut(
                id=r["id"],
                name=r["name"],
                type=r["type"],
                latitude=r["latitude"],
                longitude=r["longitude"],
                radius_meters=r["radius_meters"],
                phone_number=r["phone_number"],
                address=r["address"],
                open_24h=r["open_24h"],
                verified=r["verified"],
                distance_meters=distance,
                eta_minutes=max(1, round(distance / 80.0)),
            )
        )
    return out


@router.get("", response_model=list[SafeZoneOut])
def list_all(db: Session = Depends(get_db)):
    rows = db.query(SafeZone).limit(500).all()
    return [SafeZoneOut.model_validate(r) for r in rows]
