"""登录身份解析：拿裸令牌换回真实用户。

复用 Java 侧 ml-common 的 LoginTokenStore 已经写好的 Redis 结构，不新增任何键：

    login:token:{uuid}   String   User 的 JSON（已脱去 password）   TTL 30min

所以小精灵不需要自己做一套登录，也不需要相信前端传来的 userId。

注意：这里读的是现有 6379 普通 Redis，只做 KV 读取。向量库是本地 Chroma，
和这个 Redis 无关，不需要 RediSearch 模块。
"""

import json
from typing import Optional

import redis
from loguru import logger

from app.conf.settings import settings

# 必须与 ml-common 的 ML.Redis.LOGIN_TOKEN_PREFIX 保持一致
LOGIN_TOKEN_PREFIX = "login:token:"

_client: Optional[redis.Redis] = None


def get_redis() -> redis.Redis:
    """懒加载 Redis 连接池。"""
    global _client
    if _client is None:
        _client = redis.Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            password=settings.redis_password or None,
            db=settings.redis_db,
            decode_responses=True,
            socket_connect_timeout=3,
            socket_timeout=3,
        )
    return _client


def resolve_user(token: Optional[str]) -> Optional[dict]:
    """用令牌换用户信息。

    Args:
        token: 客户端请求头里的裸 uuid（不含 Redis key 前缀）

    Returns:
        用户 dict（至少含 id、nickname）；令牌缺失/过期/Redis 异常时返回 None。

    这里刻意不抛异常：解析不出身份不等于请求失败，小精灵仍然可以回答
    不需要身份的通用问题（课程介绍、平台规则），只是查不了「我的订单」。
    """
    if not token:
        return None

    try:
        raw = get_redis().get(LOGIN_TOKEN_PREFIX + token)
    except Exception as exc:
        # Redis 挂了不应该让整个问答不可用，降级成未登录
        logger.warning(f"解析登录令牌失败，降级为未登录：{exc}")
        return None

    if not raw:
        # 令牌不存在：未登录、已过期或已被强制下线
        return None

    try:
        user = json.loads(raw)
    except json.JSONDecodeError:
        logger.warning("登录令牌内容不是合法 JSON，降级为未登录")
        return None

    if not isinstance(user, dict) or user.get("id") is None:
        return None
    return user
