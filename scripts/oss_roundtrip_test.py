import argparse
import base64
import hashlib
import hmac
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from email.utils import formatdate


def normalize_endpoint(endpoint: str) -> str:
    ep = (endpoint or "").strip().rstrip("/")
    if not ep:
        return ep
    if not ep.startswith(("http://", "https://")):
        ep = f"https://{ep}"
    return ep


def sign_oss_v1(method: str, ak: str, sk: str, canonical_resource: str, content_type: str = "", body: bytes = b""):
    date_value = formatdate(usegmt=True)
    content_md5 = ""
    if body:
        content_md5 = base64.b64encode(hashlib.md5(body).digest()).decode("ascii")
    string_to_sign = (
        f"{method}\n"
        f"{content_md5}\n"
        f"{content_type}\n"
        f"{date_value}\n"
        f"{canonical_resource}"
    )
    signature = base64.b64encode(
        hmac.new(sk.encode("utf-8"), string_to_sign.encode("utf-8"), hashlib.sha1).digest()
    ).decode("ascii")
    headers = {
        "Authorization": f"OSS {ak}:{signature}",
        "Date": date_value,
    }
    if content_md5:
        headers["Content-MD5"] = content_md5
    if content_type:
        headers["Content-Type"] = content_type
    return headers


def request_with_retry(req: urllib.request.Request, timeout: float):
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read()


def endpoint_candidates(endpoint: str):
    candidates = [endpoint]
    if "-internal." in endpoint:
        candidates.append(endpoint.replace("-internal.", "."))
    return candidates


def main() -> int:
    parser = argparse.ArgumentParser(description="Aliyun OSS upload/download roundtrip test")
    parser.add_argument("--image-path", required=True)
    parser.add_argument("--endpoint", default=os.getenv("BOS_ENDPOINT", ""))
    parser.add_argument("--access-key", default=os.getenv("BOS_ACCESS_KEY", ""))
    parser.add_argument("--secret-key", default=os.getenv("BOS_SECRET_KEY", ""))
    parser.add_argument("--bucket", default=os.getenv("BOS_BUCKET", ""))
    parser.add_argument("--prefix", default="detect/e2e")
    parser.add_argument("--timeout", type=float, default=20.0)
    parser.add_argument("--keep-object", action="store_true")
    args = parser.parse_args()

    if not os.path.exists(args.image_path):
        print(json.dumps({"ok": False, "error": f"image not found: {args.image_path}"}, ensure_ascii=False))
        return 1

    endpoint = normalize_endpoint(args.endpoint)
    if not endpoint or not args.access_key or not args.secret_key or not args.bucket:
        print(json.dumps({"ok": False, "error": "missing endpoint/access_key/secret_key/bucket"}, ensure_ascii=False))
        return 2

    with open(args.image_path, "rb") as f:
        source = f.read()

    object_key = f"{args.prefix.strip('/')}/{int(time.time())}_{os.path.basename(args.image_path)}"
    encoded_key = urllib.parse.quote(object_key, safe="/")
    canonical_resource = f"/{args.bucket}/{encoded_key}"
    last_network_error = None
    object_url = ""
    used_endpoint = ""

    put_status = 0
    put_body = b""
    for candidate in endpoint_candidates(endpoint):
        parsed = urllib.parse.urlparse(candidate)
        host = parsed.netloc
        object_url = f"{parsed.scheme}://{args.bucket}.{host}/{encoded_key}"

        put_headers = sign_oss_v1(
            "PUT",
            args.access_key,
            args.secret_key,
            canonical_resource,
            content_type="image/jpeg",
            body=source,
        )
        put_req = urllib.request.Request(object_url, data=source, method="PUT", headers=put_headers)
        try:
            put_status, put_body = request_with_retry(put_req, timeout=args.timeout)
            used_endpoint = candidate
            break
        except urllib.error.URLError as exc:
            last_network_error = str(exc)
            continue

    if not used_endpoint:
        print(
            json.dumps(
                {
                    "ok": False,
                    "step": "upload",
                    "error": last_network_error or "endpoint not reachable",
                },
                ensure_ascii=False,
            )
        )
        return 3

    if put_status not in (200, 201):
        print(
            json.dumps(
                {
                    "ok": False,
                    "step": "upload",
                    "status": put_status,
                    "response": put_body.decode("utf-8", errors="replace"),
                    "endpoint": used_endpoint,
                },
                ensure_ascii=False,
            )
        )
        return 3

    get_headers = sign_oss_v1("GET", args.access_key, args.secret_key, canonical_resource)
    get_req = urllib.request.Request(object_url, method="GET", headers=get_headers)
    get_status, get_body = request_with_retry(get_req, timeout=args.timeout)
    if get_status != 200:
        print(
            json.dumps(
                {
                    "ok": False,
                    "step": "download",
                    "status": get_status,
                    "response": get_body.decode("utf-8", errors="replace"),
                    "url": object_url,
                    "key": object_key,
                    "endpoint": used_endpoint,
                },
                ensure_ascii=False,
            )
        )
        return 4

    source_sha = hashlib.sha256(source).hexdigest()
    roundtrip_sha = hashlib.sha256(get_body).hexdigest()

    if not args.keep_object:
        delete_headers = sign_oss_v1("DELETE", args.access_key, args.secret_key, canonical_resource)
        delete_req = urllib.request.Request(object_url, method="DELETE", headers=delete_headers)
        request_with_retry(delete_req, timeout=args.timeout)

    ok = source_sha == roundtrip_sha
    print(
        json.dumps(
            {
                "ok": ok,
                "url": object_url,
                "key": object_key,
                "endpoint": used_endpoint,
                "size": len(get_body),
                "sourceSha256": source_sha,
                "downloadSha256": roundtrip_sha,
            },
            ensure_ascii=False,
        )
    )
    return 0 if ok else 5


if __name__ == "__main__":
    raise SystemExit(main())
