from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import List, Optional
import ipaddress


class Settings(BaseSettings):
    INTERNAL_TOKEN: str
    INTERNAL_IP_WHITELIST: Optional[str] = "127.0.0.1"
    ENV: str = "dev"
    LOG_LEVEL: str = "INFO"

    # LLM (Qianfan)
    QIANFAN_API_KEY: Optional[str] = None
    QIANFAN_SECRET_KEY: Optional[str] = None
    QIANFAN_BASE_URL: Optional[str] = None

    # Vector store (ChromaDB)
    VECTOR_BACKEND: str = "chroma"
    VECTOR_DB_URL: Optional[str] = None
    CHROMA_PERSIST_DIR: str = "./chroma_data"
    CHROMA_COLLECTION: str = "jigu_artifacts"

    # Cache
    REDIS_URL: Optional[str] = None

    # Object storage (BOS)
    BOS_ENDPOINT: Optional[str] = None
    BOS_ACCESS_KEY: Optional[str] = None
    BOS_SECRET_KEY: Optional[str] = None
    BOS_BUCKET: Optional[str] = None

    # VLM Detector (Qwen3-VL-LoRA via OpenAI-compatible API)
    VLM_BASE_URL: Optional[str] = None          # e.g. https://xxx.seetacloud.com:8443/v1
    VLM_API_KEY: Optional[str] = "sk-123456"
    VLM_MODEL_NAME: str = "qwen3-vl-lora"
    VLM_TIMEOUT: float = 60.0

    # Embedding (Sentence-BERT)
    EMBEDDING_MODEL: str = "paraphrase-multilingual-MiniLM-L12-v2"
    EMBEDDING_DIM: int = 384  # paraphrase-multilingual-MiniLM-L12-v2 输出维度

    # Java Backend（知识库重建时 Python 调 Java 拉取 APPROVED 文物）
    JAVA_BASE_URL: Optional[str] = "http://localhost:8080"

    # Timeouts (seconds)
    HTTP_TIMEOUT: float = 10.0
    LLM_TIMEOUT: float = 30.0
    VECTOR_TIMEOUT: float = 5.0

    model_config = SettingsConfigDict(env_file=".env")

    def internal_ip_whitelist(self) -> List[ipaddress._BaseAddress]:
        if not self.INTERNAL_IP_WHITELIST:
            return []
        items = [s.strip() for s in self.INTERNAL_IP_WHITELIST.split(",") if s.strip()]
        parsed: List[ipaddress._BaseAddress] = []
        for item in items:
            try:
                parsed.append(ipaddress.ip_address(item))
            except ValueError:
                continue
        return parsed


settings = Settings()
