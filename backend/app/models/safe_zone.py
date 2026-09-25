import uuid

from geoalchemy2 import Geography
from sqlalchemy import Boolean, Float, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from ..database import Base


class SafeZone(Base):
    __tablename__ = "safe_zones"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)

    # POLICE_STATION | HOSPITAL | SECURITY_OFFICE | CAMPUS_SECURITY
    # | PATROL_BASE | VERIFIED_PUBLIC | TRANSIT | PARKING | OTHER
    type: Mapped[str] = mapped_column(String(32), nullable=False, index=True)

    latitude: Mapped[float] = mapped_column(Float, nullable=False)
    longitude: Mapped[float] = mapped_column(Float, nullable=False)
    point: Mapped[Geography] = mapped_column(
        Geography(geometry_type="POINT", srid=4326), nullable=False
    )

    radius_meters: Mapped[float] = mapped_column(Float, default=100.0, nullable=False)
    phone_number: Mapped[str | None] = mapped_column(String(32), nullable=True)
    address: Mapped[str | None] = mapped_column(String(500), nullable=True)
    open_24h: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    verified: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
