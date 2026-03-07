from fastapi import APIRouter, Depends
from app.api.deps import internal_auth
from app.api.routes.internal import detect, dialog, knowledge, health_internal


router = APIRouter(prefix="/internal", dependencies=[Depends(internal_auth)])
router.include_router(detect.router)
router.include_router(dialog.router)
router.include_router(knowledge.router)
router.include_router(health_internal.router)
