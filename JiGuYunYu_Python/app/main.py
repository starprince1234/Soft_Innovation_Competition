from contextlib import asynccontextmanager
import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from starlette.middleware.cors import CORSMiddleware

from app.logging_config import configure_logging
from app.settings import settings
from app.bootstrap import create_container, startup, shutdown
from app.middlewares.internal_auth import InternalAuthMiddleware
from app.middlewares.request_id import RequestIDMiddleware
from app.exceptions import ApplicationError

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    # ---- 日志（尽早初始化）----
    configure_logging(level=settings.LOG_LEVEL)

    # ---- 装配容器 ----
    container = create_container()

    # ---- lifespan（startup / shutdown）----
    @asynccontextmanager
    async def lifespan(app: FastAPI):
        await startup(container)
        yield
        await shutdown(container)

    app = FastAPI(
        title="JiGuYunYu - Internal API",
        lifespan=lifespan,
    )

    # 容器挂载到 app.state，供 deps 获取
    app.state.container = container

    # ---- 中间件（注册顺序：先注册的先处理请求）----
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_methods=["*"],
        allow_headers=["*"],
    )
    app.add_middleware(InternalAuthMiddleware)
    app.add_middleware(RequestIDMiddleware)  # 最先执行：生成 request_id

    # ---- 路由 ----
    from app.api.routes import health
    from app.api.routes.internal import router as internal_router

    app.include_router(health.router)
    app.include_router(internal_router)

    # ---- 异常处理器 ----
    @app.exception_handler(ApplicationError)
    async def app_error_handler(request: Request, exc: ApplicationError):
        """项目级异常 → 统一 code/message/data"""
        status = exc.code if 400 <= exc.code <= 599 else 500
        return JSONResponse(
            {"code": exc.code, "message": exc.message, "data": None},
            status_code=status,
        )

    @app.exception_handler(HTTPException)
    async def http_exception_handler(request: Request, exc: HTTPException):
        """FastAPI HTTPException → 统一 code/message/data"""
        if request.url.path.startswith("/internal"):
            return JSONResponse(
                {"code": exc.status_code, "message": str(exc.detail), "data": None},
                status_code=exc.status_code,
            )
        return JSONResponse({"detail": exc.detail}, status_code=exc.status_code)

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception):
        """未捕获异常 → 9000 未知错误（不泄露 traceback）"""
        logger.exception("unhandled error  path=%s", request.url.path)
        return JSONResponse(
            {"code": 9000, "message": "internal server error", "data": None},
            status_code=500,
        )

    return app


app = create_app()
