"""小精灵智能体：一个模型 + 一组工具 + 按会话隔离的短期记忆。

—— 为什么用 create_agent 而不是 DeepAgents ——

参考项目「深度研搜」用的是 DeepAgents 主从架构（主智能体 + 3 个子智能体 +
WebSocket 推进度 + 会话工作目录），那套适合「提交任务 → 跑几十秒 → 产出 PDF」
的研究型任务。小精灵要的是秒级问答，用 create_agent 的 ReAct 循环（模型思考
→ 调工具 → 再判断）就够了，底层同样是 LangGraph 图运行时。

以后要做「生成本章学习笔记并导出」这类长任务，再考虑把子智能体那套搬过来。
"""

from langchain.agents import create_agent
from langchain.agents.middleware import dynamic_prompt
from langgraph.checkpoint.memory import InMemorySaver
from loguru import logger

from app.agent.llm import get_llm
from app.agent.prompts import build_system_prompt
from app.api.context import get_context
from app.tools import ALL_TOOLS

_agent = None
# 短期记忆：按 thread_id 隔离。进程重启即清空，够用于「同一节课里的连续追问」。
# 要跨重启保留就换成 SqliteSaver 或 Redis checkpointer。
_checkpointer = InMemorySaver()


@dynamic_prompt
def _sprite_prompt(request) -> str:
    """每次调模型前重新拼系统提示词。

    播放进度、用户身份每次请求都不同，所以提示词不能在建图时写死。
    这里从 ContextVar 读当次请求的上下文——请求处理和模型调用在同一个
    asyncio 任务里，ContextVar 能正确传递，不会串台。
    """
    return build_system_prompt(get_context())


def get_agent():
    """创建（并缓存）智能体。图只编译一次。"""
    global _agent
    if _agent is None:
        _agent = create_agent(
            model=get_llm(),
            tools=ALL_TOOLS,
            middleware=[_sprite_prompt],
            checkpointer=_checkpointer,
        )
        logger.info(f"小精灵智能体已就绪，共装载 {len(ALL_TOOLS)} 个工具")
    return _agent


def build_thread_id(user_id: int | None, episode_id: int | None) -> str:
    """会话标识：每个用户在每一节课下有独立的对话历史。

    这样「切到下一节课」自然就是新会话，不会带着上一节的上下文串味。
    未登录用户按集次共用一条游客会话（拿不到个人数据，无泄露风险）。
    """
    who = f"u{user_id}" if user_id else "guest"
    where = f"e{episode_id}" if episode_id else "global"
    return f"{who}:{where}"


async def ask(question: str) -> dict:
    """问小精灵一个问题。

    Args:
        question: 学员的问题原文

    Returns:
        {"reply": 回复正文, "tools": 本轮调用的工具名列表}
    """
    ctx = get_context()
    agent = get_agent()
    thread_id = build_thread_id(ctx.user_id, ctx.episode_id)

    result = await agent.ainvoke(
        {"messages": [{"role": "user", "content": question}]},
        {"configurable": {"thread_id": thread_id}},
    )

    reply = ""
    messages = result.get("messages") or []
    if messages:
        reply = messages[-1].content or ""
    # 多模态/分块返回时 content 可能是 list，统一压成纯文本
    if isinstance(reply, list):
        reply = "".join(
            part.get("text", "") if isinstance(part, dict) else str(part)
            for part in reply
        )

    return {
        "reply": reply.strip() or "抱歉，我暂时没能生成有效回复，换个说法再问一次？",
        "tools": ctx.tool_trace,
    }
