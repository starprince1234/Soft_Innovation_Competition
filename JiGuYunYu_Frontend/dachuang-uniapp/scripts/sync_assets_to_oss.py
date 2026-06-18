#!/usr/bin/env python3
import json
import mimetypes
import os
import uuid
from pathlib import Path

import oss2
from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parents[1]
SRC_STATIC = ROOT / "src" / "static"
PYTHON_ENV = ROOT.parents[1] / "JiGuYunYu_Python" / ".env"
BACKEND_ENV = ROOT.parents[1] / ".env.backend"
OUT_MANIFEST = ROOT / "src" / "common" / "constants" / "assets-manifest.json"
PREFIX = "assets/weixin"


def _mask_secret(value: str):
    if not value:
        return ""
    if len(value) <= 6:
        return "*" * len(value)
    return f"{value[:3]}***{value[-3:]}"


def _normalize_endpoint(endpoint: str, bucket: str):
    ep = (endpoint or "").strip()
    if not ep:
        return ""
    ep = ep.replace("https://", "").replace("http://", "").rstrip("/")
    host = ep.split("/", 1)[0]
    # Accept bucket-prefixed endpoint values and normalize to region endpoint.
    bucket_prefix = f"{bucket}."
    if bucket and host.startswith(bucket_prefix):
        host = host[len(bucket_prefix):]
    return host


def load_config():
    env_file = os.getenv("BOS_ENV_FILE")
    source_file = Path(env_file).resolve() if env_file else (BACKEND_ENV if BACKEND_ENV.exists() else PYTHON_ENV)
    cfg = dotenv_values(source_file)

    endpoint = (os.getenv("BOS_ENDPOINT") or cfg.get("BOS_ENDPOINT") or "").strip()
    access_key = (os.getenv("BOS_ACCESS_KEY") or cfg.get("BOS_ACCESS_KEY") or "").strip()
    secret_key = (os.getenv("BOS_SECRET_KEY") or cfg.get("BOS_SECRET_KEY") or "").strip()
    bucket = (os.getenv("BOS_BUCKET") or cfg.get("BOS_BUCKET") or "").strip()
    endpoint = _normalize_endpoint(endpoint, bucket)

    if not endpoint or not access_key or not secret_key or not bucket:
        raise RuntimeError(f"Missing BOS_* config. checked={source_file}")

    print(
        "[sync_assets_to_oss] config"
        f" source={source_file} endpoint={endpoint} bucket={bucket} ak={_mask_secret(access_key)}"
    )
    return endpoint, access_key, secret_key, bucket


def endpoint_candidates(endpoint: str):
    # try configured endpoint first, then a public fallback.
    cands = [endpoint]
    if "-internal" in endpoint:
        cands.append(endpoint.replace("-internal", ""))
    return list(dict.fromkeys(cands))


def connect_bucket(endpoint_list, access_key, secret_key, bucket_name):
    auth = oss2.Auth(access_key, secret_key)
    last_err = None
    for ep in endpoint_list:
        try:
            bucket = oss2.Bucket(auth, f"https://{ep}", bucket_name)
            # Use a real PUT probe: some accounts can write objects but cannot read bucket info.
            probe_key = f"{PREFIX}/.probe-{uuid.uuid4().hex}.txt"
            bucket.put_object(probe_key, b"probe")
            return bucket, ep
        except Exception as ex:  # noqa: BLE001
            last_err = ex
    raise RuntimeError(f"Failed to connect OSS bucket with all endpoints: {last_err}")


def collect_static_files():
    if not SRC_STATIC.exists():
        return []
    exts = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"}
    files = []
    for p in SRC_STATIC.rglob("*"):
        if p.is_file() and p.suffix.lower() in exts:
            files.append(p)
    return files


def upload_file(bucket, local_path: Path, object_key: str):
    content_type = mimetypes.guess_type(local_path.name)[0] or "application/octet-stream"
    headers = {"Content-Type": content_type}
    with local_path.open("rb") as f:
        bucket.put_object(object_key, f, headers=headers)


def to_remote_url(bucket_name: str, endpoint_used: str, object_key: str):
    # ensure app can access over public endpoint host
    public_ep = endpoint_used.replace("-internal", "")
    return f"https://{bucket_name}.{public_ep}/{object_key}"


def main():
    endpoint, access_key, secret_key, bucket_name = load_config()
    cands = endpoint_candidates(endpoint)
    bucket, endpoint_used = connect_bucket(cands, access_key, secret_key, bucket_name)

    # create logical directory marker
    bucket.put_object(f"{PREFIX}/.keep", b"")

    files = collect_static_files()
    manifest = {}

    for f in files:
        rel = f.relative_to(SRC_STATIC).as_posix()
        object_key = f"{PREFIX}/{rel}"
        upload_file(bucket, f, object_key)
        manifest[f"/static/{rel}"] = to_remote_url(bucket_name, endpoint_used, object_key)

    OUT_MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    OUT_MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"uploaded={len(files)} endpoint={endpoint_used} manifest={OUT_MANIFEST}")


if __name__ == "__main__":
    main()
