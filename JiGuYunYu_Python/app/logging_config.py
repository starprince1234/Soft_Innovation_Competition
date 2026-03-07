import logging
import sys
from contextvars import ContextVar

# ---- 全局 context vars（供中间件写入，日志自动读取）----
request_id_ctx: ContextVar[str] = ContextVar("request_id", default="-")


class RequestIDFilter(logging.Filter):
    """把 contextvars 里的 request_id 注入到每条日志记录"""

    def filter(self, record: logging.LogRecord) -> bool:
        record.request_id = request_id_ctx.get("-")  # type: ignore[attr-defined]
        return True


def configure_logging(level: str = "INFO") -> None:
    """
    初始化结构化日志：
      - 格式：时间 | 级别 | request_id | logger名 | 消息
      - 敏感字段由调用方自行脱敏后再 log（此处不做全局替换）
    """
    log_level = getattr(logging, level.upper(), logging.INFO)

    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(log_level)

    fmt = (
        "%(asctime)s | %(levelname)-7s | rid=%(request_id)s | "
        "%(name)s | %(message)s"
    )
    handler.setFormatter(logging.Formatter(fmt, datefmt="%Y-%m-%d %H:%M:%S"))
    handler.addFilter(RequestIDFilter())

    root = logging.getLogger()
    root.setLevel(log_level)
    # 避免重复添加 handler（reload / 测试场景）
    root.handlers.clear()
    root.addHandler(handler)


def get_logger(name: str) -> logging.Logger:
    """获取带 request_id 注入的 logger"""
    logger = logging.getLogger(name)
    return logger
