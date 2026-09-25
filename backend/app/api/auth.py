from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..config import settings
from ..core.security import (
    create_access_token,
    create_refresh_token,
    decode_refresh_token,
    hash_password,
    verify_password,
)
from ..database import get_db
from ..models import RefreshToken, User
from ..schemas import (
    AuthResponse,
    LoginRequest,
    RefreshRequest,
    RegisterRequest,
    UserOut,
)

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])


@router.post("/register", response_model=AuthResponse)
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    existing = db.scalar(select(User).where(User.email == payload.email))
    if existing:
        raise HTTPException(status_code=409, detail="Email already registered")

    user = User(
        email=payload.email,
        password_hash=hash_password(payload.password),
        full_name=payload.full_name,
        phone_number=payload.phone_number,
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    return _issue_tokens(db, user)


@router.post("/login", response_model=AuthResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = db.scalar(select(User).where(User.email == payload.email))
    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return _issue_tokens(db, user)


@router.post("/refresh", response_model=AuthResponse)
def refresh(payload: RefreshRequest, db: Session = Depends(get_db)):
    try:
        data = decode_refresh_token(payload.refresh_token)
    except ValueError:
        raise HTTPException(status_code=401, detail="Invalid refresh token")

    user_id = data.get("sub")
    user = db.get(User, user_id)
    if not user:
        raise HTTPException(status_code=401, detail="User not found")

    # Optionally: verify token hash against stored refresh token (recommended in prod)

    return _issue_tokens(db, user)


@router.post("/logout")
def logout():
    # In this minimal version, client simply discards tokens.
    # Production: revoke by storing token hashes in refresh_tokens table.
    return {"status": "ok"}


def _issue_tokens(db: Session, user: User) -> AuthResponse:
    access = create_access_token(str(user.id), extra={"role": user.role})
    refresh = create_refresh_token(str(user.id))

    # Store refresh token hash for revocation (minimal version: just record expiry)
    db.add(
        RefreshToken(
            user_id=user.id,
            token_hash=refresh[-32:],  # placeholder — use full hash in prod
            expires_at=datetime.now(timezone.utc)
            + timedelta(days=settings.JWT_REFRESH_EXPIRES_DAYS),
        )
    )
    db.commit()

    return AuthResponse(
        access_token=access,
        refresh_token=refresh,
        expires_in=settings.JWT_ACCESS_EXPIRES_MINUTES * 60,
        user=UserOut.model_validate(user),
    )
