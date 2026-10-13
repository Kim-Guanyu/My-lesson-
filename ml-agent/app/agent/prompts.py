"""提示词加载与上下文拼装。

读取 prompt/prompts.yml，并把当次请求的播放进度、用户身份填进系统提示词。
"""

from functools import lru_cache

import yaml

from app.api.context import RequestContext
from app.conf.settings import PROJECT_ROOT

PROMPTS_FILE = PROJECT_ROOT / "prompt" / "prompts.yml"


@lru_cache
def _load() -> dict:
    with open(PROMPTS_FILE, encoding="utf-8") as f:
        return yaml.safe_load(f)["sprite"]


def _format_seconds(seconds: float) -> str:
    """把播放秒数格式化成 mm:ss，方便模型和用户对齐。"""
    total = int(seconds)
    return f"{total // 60:02d}:{total % 60:02d}"


def build_playing_context(ctx: RequestContext) -> str:
    """拼「学员正卡在哪里」。

    这是小精灵区别于普通聊天框的关键：播放进度让「刚才那段」变得可解析。
    """
    prompts = _load()
    if not ctx.course_title and not ctx.episode_title:
        return prompts["no_playing_context"].strip()

    lines = ["【当前播放上下文】"]
    if ctx.course_title:
        lines.append(f"课程：《{ctx.course_title}》（课程ID={ctx.course_id}）")
    if ctx.episode_title:
        lines.append(f"正在看：{ctx.episode_title}（集次ID={ctx.episode_id}）")
    elif ctx.episode_id:
        lines.append(f"正在看的集次ID={ctx.episode_id}")
    if ctx.current_time is not None:
        lines.append(
            f"播放进度：{_format_seconds(ctx.current_time)}"
            f"（学员是在这个时间点提问的，「刚才」大概指这前后的内容）"
        )
    return "\n".join(lines)


def build_user_context(ctx: RequestContext) -> str:
    """拼用户身份。注意只写昵称，不把 userId 之外的敏感信息塞进提示词。"""
    prompts = _load()
    if not ctx.logged_in:
        return prompts["guest_context"].strip()

    who = ctx.nickname or "该学员"
    return (
        f"【用户身份】学员已登录，昵称：{who}。"
        "可以使用订单类工具查询他本人的订单与已购课程。"
    )


def build_system_prompt(ctx: RequestContext) -> str:
    """组装完整系统提示词。"""
    return (
        _load()["system"]
        .replace("{playing_context}", build_playing_context(ctx))
        .replace("{user_context}", build_user_context(ctx))
    )
