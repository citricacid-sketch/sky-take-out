"""服务配置 (pydantic-settings)。环境变量命名与 Java 端保持一致。"""

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # --- LLM (与 Java 同名环境变量保持一致) ---
    llm_api_key: str = ""
    llm_base_url: str = "https://longcat.chat/v1/chat/completions"
    llm_model: str = "Longcat-Flash-Chat"
    llm_max_tokens: int = Field(default=1024, ge=64, le=4096)
    llm_temperature: float = Field(default=0.7, ge=0.0, le=2.0)
    llm_sql_temperature: float = Field(default=0.1, ge=0.0, le=2.0)

    # --- 查询限制 ---
    query_timeout_ms: int = Field(default=5000, ge=500, le=30000)
    query_max_rows: int = Field(default=100, ge=10, le=500)

    # --- MySQL (只读账号 sky_ai_ro) ---
    mysql_host: str = "127.0.0.1"
    mysql_port: int = Field(default=3306, ge=1, le=65535)
    mysql_database: str = "sky_take_out"
    mysql_user: str = "sky_ai_ro"
    mysql_password: str = ""

    # --- Redis (对话记忆) ---
    redis_host: str = "127.0.0.1"
    redis_port: int = Field(default=6379, ge=1, le=65535)
    redis_password: str = ""
    redis_db: int = Field(default=0, ge=0, le=15)
    chat_session_ttl: int = Field(default=1800, ge=60, le=7200)  # 30 分钟

    # --- 服务 ---
    ai_service_token: str = ""  # 空 = 认证关闭 (仅开发)
    ai_service_port: int = Field(default=8000, ge=1, le=65535)
    ai_service_url: str = "http://127.0.0.1:8000"

    @field_validator("llm_base_url")
    @classmethod
    def _strip_completions_suffix(cls, v: str) -> str:
        """Java 的 LLM_BASE_URL 是完整 /v1/chat/completions 地址；
        langchain-openai 需要 /v1 根并自动拼接 /chat/completions。"""
        suffix = "/chat/completions"
        v = v.rstrip("/")
        if v.endswith(suffix):
            return v[: -len(suffix)]
        return v


settings = Settings()
