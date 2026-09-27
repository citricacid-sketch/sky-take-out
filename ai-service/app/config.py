"""服务配置 (pydantic-settings)。环境变量命名与 Java 端保持一致。"""

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # --- LLM ---
    llm_api_key: str = ""
    llm_base_url: str = "https://longcat.chat/v1/chat/completions"
    llm_model: str = "Longcat-Flash-Chat"
    llm_max_tokens: int = 1024
    llm_temperature: float = 0.7
    llm_sql_temperature: float = 0.1

    # --- 查询限制 ---
    query_timeout_ms: int = 5000
    query_max_rows: int = 100

    # --- MySQL (sky_ai_ro) ---
    mysql_host: str = "127.0.0.1"
    mysql_port: int = 3306
    mysql_database: str = "sky_take_out"
    mysql_user: str = "sky_ai_ro"
    mysql_password: str = <REDACTED>

    # --- Redis ---
    redis_host: str = "127.0.0.1"
    redis_port: int = 6379
    redis_password: <REDACTED> = ""
    redis_db: int = 0

    # --- 服务 ---
    ai_service_token: str = ""  # 空 = 认证关闭(仅开发)
    ai_service_port: int = 8000

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
