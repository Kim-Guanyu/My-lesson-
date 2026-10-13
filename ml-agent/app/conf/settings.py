"""全局配置：统一从 .env 读取，供全项目取参数。

参考「电商问数」的做法，把配置收口到一个对象上：MySQL、ES、Embedding、LLM
都从同一套配置体系取值，避免各模块自己 os.getenv 导致口径不一致。
这里因为不直连数据库，配置项比参考项目少很多。
"""

from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# 从当前文件 app/conf/settings.py 出发回到 ml-agent/ 根目录
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent


class Settings(BaseSettings):
    """全部配置项，字段名与 .env 中的 KEY 一一对应（大小写不敏感）。"""

    model_config = SettingsConfigDict(
        env_file=PROJECT_ROOT / ".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # ---------- 大模型（对话）----------
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"
    llm_temperature: float = 0.3
    llm_max_tokens: int = 1024
    llm_timeout: int = 60

    # ---------- Embedding（向量化）----------
    # DeepSeek 无 embedding 接口，故单独用百炼
    dashscope_api_key: str = ""
    embedding_model: str = "text-embedding-v3"

    # ---------- Java 网关 ----------
    gateway_base_url: str = "http://192.168.211.132:24101"
    gateway_timeout: int = 10

    # ---------- Redis（解析登录身份，非向量库）----------
    redis_host: str = "192.168.211.132"
    redis_port: int = 6379
    redis_password: str = ""
    redis_db: int = 0

    # ---------- 本服务 ----------
    agent_host: str = "0.0.0.0"
    agent_port: int = 24107
    log_level: str = "INFO"

    # ---------- RAG ----------
    knowledge_dir: str = "knowledge"
    vector_store_dir: str = "data/vectorstore"
    vector_collection: str = "ml_knowledge"
    chunk_size: int = 500
    chunk_overlap: int = 80
    retrieve_top_k: int = 4

    # ---------- 派生路径：把相对配置解析成绝对路径 ----------

    @property
    def knowledge_path(self) -> Path:
        return PROJECT_ROOT / self.knowledge_dir

    @property
    def vector_store_path(self) -> Path:
        return PROJECT_ROOT / self.vector_store_dir

    @property
    def llm_ready(self) -> bool:
        """对话模型是否可用。没配 key 时接口要给出明确提示而不是 500。"""
        return bool(self.deepseek_api_key) and not self.deepseek_api_key.startswith(
            "sk-xxx"
        )

    @property
    def embedding_ready(self) -> bool:
        """向量化是否可用。没配百炼 key 时 RAG 检索要优雅降级而不是崩。"""
        return bool(self.dashscope_api_key) and not self.dashscope_api_key.startswith(
            "sk-xxx"
        )


@lru_cache
def get_settings() -> Settings:
    """带缓存的配置单例：.env 只读一次。"""
    return Settings()


settings = get_settings()
