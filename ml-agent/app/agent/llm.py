"""统一创建大模型对象。

DeepSeek 走 OpenAI 兼容协议接入：用 init_chat_model(model_provider="openai")
配合 base_url，与参考教程里接百炼的写法一致，好处是换厂商只改 .env 不改代码。
"""

from functools import lru_cache

from langchain.chat_models import init_chat_model

from app.conf.settings import settings


@lru_cache
def get_llm():
    """创建（并缓存）聊天模型。"""
    if not settings.llm_ready:
        raise RuntimeError(
            "未配置 DEEPSEEK_API_KEY。请复制 ml-agent/.env.example 为 .env 并填入 API Key。"
        )
    return init_chat_model(
        model=settings.deepseek_model,
        model_provider="openai",
        api_key=settings.deepseek_api_key,
        base_url=settings.deepseek_base_url,
        temperature=settings.llm_temperature,
        max_tokens=settings.llm_max_tokens,
        timeout=settings.llm_timeout,
    )
