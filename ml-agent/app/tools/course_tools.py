"""课程相关工具：把现有 Java 接口包装成模型可调用的工具。

对应的 Java 接口（全部走网关，见 app/clients/gateway.py 的说明）：
    course-server /course/select/{id}            课程详情（含章节与集次目录）
    course-server /course/search                 ES + ik 分词的课程检索
    course-server /episode/select/{id}           集次详情
    course-server /episode/searchBarrage/{id}    该集弹幕
"""

from typing import Optional

from langchain_core.tools import tool
from pydantic import BaseModel, Field

from app.api.context import trace_tool
from app.clients import gateway
from app.clients.gateway import GatewayError

COURSE_SERVICE = "course-server"


class CourseIdArgs(BaseModel):
    """按课程主键查询"""

    course_id: int = Field(description="课程主键 ID")


class SearchCoursesArgs(BaseModel):
    """按关键词搜索课程"""

    keyword: str = Field(description="课程名称或作者名称的关键词")


class EpisodeIdArgs(BaseModel):
    """按集次主键查询"""

    episode_id: int = Field(description="集次（课时）主键 ID")


@tool(args_schema=CourseIdArgs)
async def get_course_outline(course_id: int) -> str:
    """查询课程的介绍和完整目录（含全部章节与课时标题）。

    用于回答「这门课讲什么」「一共几章」「某个知识点在第几章讲的」这类问题。
    """
    trace_tool("get_course_outline")
    try:
        course = await gateway.get(COURSE_SERVICE, "course", f"/select/{course_id}")
    except GatewayError as exc:
        return f"查询课程信息失败：{exc}"

    if not course:
        return f"未找到 ID 为 {course_id} 的课程。"

    lines = [
        f"课程：《{course.get('title', '')}》",
        f"作者：{course.get('author', '')}",
        f"价格：{course.get('price', '')} 元",
    ]
    if course.get("info"):
        lines.append(f"简介：{course['info']}")

    seasons = course.get("seasons") or []
    if seasons:
        lines.append("目录：")
        for s_idx, season in enumerate(seasons, start=1):
            lines.append(f"  第{s_idx}章 {season.get('title', '')}")
            for e_idx, ep in enumerate(season.get("episodes") or [], start=1):
                ep_line = f"    {s_idx}-{e_idx} {ep.get('title', '')}（集次ID={ep.get('id')}）"
                if ep.get("info"):
                    ep_line += f"｜{ep['info']}"
                lines.append(ep_line)
    else:
        lines.append("目录：暂无章节数据。")

    return "\n".join(lines)


@tool(args_schema=SearchCoursesArgs)
async def search_courses(keyword: str) -> str:
    """按关键词搜索平台上的课程。

    用于回答「有没有讲 XX 的课」「还有哪些课涉及这个主题」这类问题。
    走后端 Elasticsearch 中文分词检索。
    """
    trace_tool("search_courses")
    try:
        page = await gateway.get(
            COURSE_SERVICE, "course", "/search",
            params={"keyword": keyword, "pageNum": 1, "pageSize": 5},
        )
    except GatewayError as exc:
        return f"搜索课程失败：{exc}"

    records = (page or {}).get("records") or []
    if not records:
        return f"没有搜索到与「{keyword}」相关的课程。"

    lines = [f"搜索到 {len(records)} 门相关课程："]
    for item in records:
        lines.append(
            f"- 《{item.get('title', '')}》｜作者：{item.get('author', '')}"
            f"｜{item.get('price', '')} 元｜课程ID={item.get('id')}"
        )
    return "\n".join(lines)


@tool(args_schema=EpisodeIdArgs)
async def get_episode_info(episode_id: int) -> str:
    """查询某一节课时（集次）的标题和描述。

    用于确认用户当前正在看的这一节到底讲什么。
    """
    trace_tool("get_episode_info")
    try:
        ep = await gateway.get(COURSE_SERVICE, "episode", f"/select/{episode_id}")
    except GatewayError as exc:
        return f"查询课时信息失败：{exc}"

    if not ep:
        return f"未找到 ID 为 {episode_id} 的课时。"

    lines = [f"课时：{ep.get('title', '')}"]
    if ep.get("info"):
        lines.append(f"描述：{ep['info']}")
    return "\n".join(lines)


@tool(args_schema=EpisodeIdArgs)
async def search_episode_questions(episode_id: int) -> str:
    """查询这节课的弹幕，了解其他同学在这节课问过什么、困惑在哪里。

    弹幕里往往有大量「这里没听懂」「这段什么意思」的真实疑问。
    当用户问「大家都在问什么」，或你想了解这节课的常见困惑点时使用。
    """
    trace_tool("search_episode_questions")
    try:
        barrages = await gateway.get(
            COURSE_SERVICE, "episode", f"/searchBarrage/{episode_id}"
        )
    except GatewayError as exc:
        return f"查询弹幕失败：{exc}"

    if not barrages:
        return "这节课暂时还没有弹幕。"

    texts: list[str] = []
    for b in barrages[:30]:
        content = b.get("content") or b.get("text")
        if content:
            texts.append(str(content))

    if not texts:
        return "这节课暂时还没有弹幕。"

    return f"这节课共 {len(texts)} 条弹幕，节选：\n" + "\n".join(
        f"- {t}" for t in texts[:20]
    )
