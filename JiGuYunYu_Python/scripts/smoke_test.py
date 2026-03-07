"""冒烟测试：确认服务可用、鉴权生效、核心接口返回合约。

用法：
  python scripts/smoke_test.py
  或通过环境变量指定：
  BASE_URL=http://10.0.0.5:8000 INTERNAL_TOKEN=xxx python scripts/smoke_test.py
"""

import os
import sys
import requests

BASE = os.environ.get("BASE_URL", "http://127.0.0.1:8000")
TOKEN = os.environ.get("INTERNAL_TOKEN", "changeme")
AUTH = {"X-Internal-Token": TOKEN}

passed = 0
failed = 0


def check(name: str, ok: bool, detail: str = ""):
    global passed, failed
    status = "PASS" if ok else "FAIL"
    if ok:
        passed += 1
    else:
        failed += 1
    print(f"  [{status}] {name}" + (f"  ({detail})" if detail else ""))


def main():
    print(f"\n=== Smoke Test  base={BASE} ===\n")

    # 1. GET /health（不鉴权）
    try:
        r = requests.get(f"{BASE}/health", timeout=5)
        check("/health returns 200", r.status_code == 200, f"status={r.status_code}")
        check("/health body.status==ok", r.json().get("status") == "ok")
    except Exception as e:
        check("/health reachable", False, str(e))

    # 2. POST /internal/dialog/responses 无 token → 401
    try:
        r = requests.post(
            f"{BASE}/internal/dialog/responses",
            json={"dialogTaskId": "d1", "query": "test"},
            timeout=5,
        )
        check("dialog no-token -> 401", r.status_code == 401, f"status={r.status_code}")
    except Exception as e:
        check("dialog no-token", False, str(e))

    # 3. POST /internal/dialog/responses 正常
    try:
        r = requests.post(
            f"{BASE}/internal/dialog/responses",
            json={"dialogTaskId": "d1", "query": "hello", "top_k": 2},
            headers=AUTH,
            timeout=15,
        )
        body = r.json()
        check("dialog 200", r.status_code == 200, f"status={r.status_code}")
        check("dialog code==200", body.get("code") == 200, f"code={body.get('code')}")
        check("dialog has answer", bool(body.get("data", {}).get("answer")))
    except Exception as e:
        check("dialog ok", False, str(e))

    # 4. POST /internal/detect/process 正常
    try:
        r = requests.post(
            f"{BASE}/internal/detect/process",
            json={"taskId": "t1", "imageUrl": "http://example.com/img.jpg"},
            headers=AUTH,
            timeout=15,
        )
        body = r.json()
        check("detect 200", r.status_code == 200, f"status={r.status_code}")
        check("detect code==200", body.get("code") == 200, f"code={body.get('code')}")
        check("detect has detections", isinstance(body.get("data", {}).get("detections"), list))
    except Exception as e:
        check("detect ok", False, str(e))

    # 汇总
    total = passed + failed
    print(f"\n=== Result: {passed}/{total} passed, {failed} failed ===")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
