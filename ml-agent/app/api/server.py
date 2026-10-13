"""FastAPI 服务入口。

路径约定与 Java 侧完全一致：/api/v1/sprite/...
网关配了 StripPrefix=1，所以客户端访问 /agent-server/api/v1/sprite/ask
到这里就是 /api/v1/sprite/ask。

—— 鉴权与 CORS 都不在这里做 ——
请求统一从 ml-gateway(24101) 进来，网关的 TokenGlobalFilter 已经校验过令牌、
CorsConfig 已经处理过跨域。本服务不对外直接暴露（切勿让小程序直连 24107，
那等于绕过鉴权把「能查订单的工具 + 可被诱导的大模型」裸奔在公网上）。
"""

from typing import Optional

from fastapi import FastAPI, Header
from loguru import logger
from pydantic import BaseModel, Field

from app.agent.sprite import ask as agent_ask
from app.api.context import RequestContext, set_context
from app.clients import vectorstore
from app.clients.identity import resolve_user
from app.conf.settings import settings

app = FastAPI(
    title="知堂课堂 - 悬浮小精灵",
    description="课程学习答疑 AI 智能体（LangGraph + 外挂知识库）",
    version="0.1.0",
)


# ==================== 请求 / 响应模型 ====================


class AskRequest(BaseModel):
    """提问请求。

    注意这里**没有 fkUserId 字段**：用户身份由服务端从请求头的 token 解析，
    不接受前端传参，防止越权查询他人订单。
    """

    question: str = Field(description="学员的问题", min_length=1, max_length=500)

    # 播放上下文（可选）：只用于拼提示词，不参与鉴权
    courseId: Optional[int] = None
    courseTitle: Optional[str] = None
    episodeId: Optional[int] = None
    episodeTitle: Optional[str] = None
    currentTime: Optional[float] = Field(
        default=None, description="提问时的视频播放进度（秒）"
    )


class Result(BaseModel):
    """统一响应体，与 ml-common 的 Result 保持一致。

    小程序的 utils/api.js 靠 code === 1000 判断成功，所以这里必须对齐，
    否则前端会把成功响应当成失败。
    """

    code: int = 1000
    message: str = "操作成功"
    data: Optional[dict] = None


# 与 ml-common ResultCode 对齐
CODE_SUCCESS = 1000
CODE_FAIL = 5000


# ==================== 接口 ====================


@app.get("/api/v1/sprite/health", response_model=Result)
async def health():
    """健康检查：确认各依赖是否就绪。联调第一步先打这个。"""
    return Result(
        data={
            "status": "ok",
            "llm_ready": settings.llm_ready,
            "embedding_ready": settings.embedding_ready,
            "knowledge_indexed": vectorstore.is_indexed(),
        }
    )


@app.post("/api/v1/sprite/ask", response_model=Result)
async def ask(req: AskRequest, token: Optional[str] = Header(default=None)):
    """问小精灵。"""
    if not settings.llm_ready:
        return Result(
            code=CODE_FAIL,
            message="小精灵还没配置好大模型，请联系管理员",
            data={"reply": "小精灵暂时不可用（未配置大模型 API Key）。"},
        )

    # 身份只从 Redis 令牌解析，绝不采信前端传参
    user = resolve_user(token)

    set_context(
        RequestContext(
            user_id=user.get("id") if user else None,
            nickname=user.get("nickname") if user else None,
            token=token,
            course_id=req.courseId,
            course_title=req.courseTitle,
            episode_id=req.episodeId,
            episode_title=req.episodeTitle,
            current_time=req.currentTime,
        )
    )

    try:
        result = await agent_ask(req.question)
    except Exception as exc:
        logger.exception(f"小精灵回答失败，question={req.question}")
        return Result(
            code=CODE_FAIL,
            message="小精灵开小差了",
            data={"reply": f"抱歉，我这边出了点问题（{type(exc).__name__}），请稍后再试。"},
        )

    return Result(data=result)


# ==================== 启动 ====================


def main():
    import uvicorn

    logger.info(
        f"小精灵服务启动 http://{settings.agent_host}:{settings.agent_port}"
        f"  LLM={'ok' if settings.llm_ready else '未配置'}"
        f"  知识库={'已建索引' if vectorstore.is_indexed() else '未建索引'}"
    )
    uvicorn.run(
        app,
        host=settings.agent_host,
        port=settings.agent_port,
        log_level=settings.log_level.lower(),
    )


if __name__ == "__main__":
    main()
