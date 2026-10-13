"""Java 网关客户端：小精灵的数据访问层。

—— 为什么不直连 MySQL / ES ——

参考项目的数据库子智能体是直连 MySQL 跑 SQL 的，那套在本项目不适用：
MyBatis-Flex 的逻辑删除（deleted 字段）、Result 统一包装、字段脱敏、Feign 聚合、
Redis 缓存全都在 Java 层。Python 直连库会绕过这些，等于维护第二套业务规则，
迟早和 Java 侧对不上。

所以一律走 http://{gateway}/{service}-server/api/v1/... ，Java 接口就是数据源。
慢一点，但一致性和维护成本上完全值得。

统一响应体（ml-common 的 Result + ResultAdvice 自动包装）：
    {"code": 1000, "message": "操作成功", "data": {}}
本模块负责解包并把 code != 1000 转成异常，避免把 {"code":5000} 原样喂给模型
——否则模型会把错误信息当成有效数据来推理。
"""

from typing import Any, Optional

import httpx
from loguru import logger

from app.api.context import get_context
from app.conf.settings import settings

# 与 ml-miniapp/utils/const.js 的 STATUS.SUCCESS 保持一致
CODE_SUCCESS = 1000
# ml-common ResultCode：登录过期 / 远程调用失败
CODE_UNAUTHORIZED = 6000


class GatewayError(RuntimeError):
    """网关调用失败。工具会捕获它并转成一句自然语言回给模型。"""

    def __init__(self, message: str, code: Optional[int] = None):
        super().__init__(message)
        self.code = code


async def _request(
    method: str,
    service: str,
    module: str,
    path: str,
    *,
    params: Optional[dict] = None,
    json_body: Optional[dict] = None,
) -> Any:
    """向网关发一次请求并解包 Result。

    Args:
        service: 服务前缀，如 course-server
        module: 模块名，如 course
        path: 模块内路径，如 /select/1

    Returns:
        解包后的 data 字段。
    """
    url = f"{settings.gateway_base_url}/{service}/api/v1/{module}{path}"

    # 透传调用者的令牌，让网关的 TokenGlobalFilter 继续生效（纵深防御）：
    # 即便小精灵这边逻辑有疏漏，越权请求在 Java 网关也会被拦掉。
    headers = {"Content-Type": "application/json"}
    token = get_context().token
    if token:
        headers["token"] = token

    try:
        async with httpx.AsyncClient(timeout=settings.gateway_timeout) as client:
            resp = await client.request(
                method, url, params=params, json=json_body, headers=headers
            )
    except httpx.TimeoutException as exc:
        raise GatewayError(f"调用后端接口超时：{module}{path}") from exc
    except httpx.HTTPError as exc:
        raise GatewayError(f"调用后端接口失败：{exc}") from exc

    if resp.status_code >= 400:
        raise GatewayError(f"后端接口返回 HTTP {resp.status_code}：{module}{path}")

    try:
        payload = resp.json()
    except ValueError as exc:
        raise GatewayError("后端接口返回的不是合法 JSON") from exc

    # 非 Result 包装的裸响应（少数接口）直接返回
    if not isinstance(payload, dict) or "code" not in payload:
        return payload

    code = payload.get("code")
    if code != CODE_SUCCESS:
        message = payload.get("message") or "请求失败"
        if code == CODE_UNAUTHORIZED:
            raise GatewayError("登录已过期，请重新登录", code=code)
        logger.debug(f"网关业务失败 code={code} module={module}{path} msg={message}")
        raise GatewayError(message, code=code)

    return payload.get("data")


async def get(service: str, module: str, path: str, params: Optional[dict] = None):
    return await _request("GET", service, module, path, params=params)


async def post(service: str, module: str, path: str, json_body: Optional[dict] = None):
    return await _request("POST", service, module, path, json_body=json_body)
