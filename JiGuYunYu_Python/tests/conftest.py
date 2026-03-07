"""
pytest fixtures：统一设置测试环境变量 → 创建 TestClient
"""

import os
import pytest

# 必须在导入 app 之前设置环境变量（settings 会在 import 时读取）
os.environ.setdefault("INTERNAL_TOKEN", "test-token")
os.environ.setdefault("INTERNAL_IP_WHITELIST", "")
os.environ.setdefault("ENV", "test")
os.environ.setdefault("LOG_LEVEL", "WARNING")

from fastapi.testclient import TestClient  # noqa: E402
from app.main import app  # noqa: E402

TOKEN = os.environ["INTERNAL_TOKEN"]
AUTH_HEADERS = {"X-Internal-Token": TOKEN}


@pytest.fixture()
def client():
    """TestClient fixture（每次测试函数重新创建）"""
    with TestClient(app) as c:
        yield c
