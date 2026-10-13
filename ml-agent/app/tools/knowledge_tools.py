"""知识库检索工具：外挂 RAG 的入口。

这是小精灵回答「视频里讲了什么」「平台规则是什么」的主要依据。
知识库内容来自 ml-agent/knowledge/ 目录下的文档，由 scripts/ingest.py 建索引。
"""

from langchain_core.tools import tool
from loguru import logger
from pydantic import BaseModel, Field

from app.api.context import trace_tool
from app.clients import vectorstore

# 相关性低于此值的片段直接丢弃，避免把不相关内容塞进上下文误导模型
MIN_RELEVANCE = 0.2


class SearchKnowledgeArgs(BaseModel):
    """知识库检索参数"""

    query: str = Field(
        description="检索关键词或问题。用课程术语、概念名称效果最好，"
        "不要带「老师刚才说的」这类指代词，要换成具体概念"
    )


@tool(args_schema=SearchKnowledgeArgs)
def search_knowledge(query: str) -> str:
    """从课程知识库中检索资料，用于回答课程概念、知识点和平台规则问题。

    这是回答学习类问题的首选工具。遇到用户问某个概念是什么意思、
    某个知识点怎么理解、平台规则如何规定，都应该先用它检索。
    """
    trace_tool("search_knowledge")

    if not vectorstore.is_indexed():
        return (
            "知识库尚未建立索引。请管理员在 ml-agent/knowledge/ 放入文档后，"
            "执行 python scripts/ingest.py 建索引。"
            "现在请基于你自己的知识谨慎回答，并明确告知用户知识库暂不可用。"
        )

    try:
        results = vectorstore.search(query)
    except RuntimeError as exc:
        # 缺 DASHSCOPE_API_KEY 之类的配置问题
        logger.warning(f"知识库检索不可用：{exc}")
        return f"知识库当前不可用：{exc}"
    except Exception as exc:
        logger.exception("知识库检索异常")
        return f"知识库检索失败：{exc}"

    kept = [(doc, score) for doc, score in results if score >= MIN_RELEVANCE]
    if not kept:
        return (
            f"知识库中没有检索到与「{query}」相关的内容。"
            "请如实告知用户知识库暂无相关资料，不要编造。"
        )

    blocks = []
    for idx, (doc, score) in enumerate(kept, start=1):
        source = doc.metadata.get("source_name") or doc.metadata.get("source") or "未知"
        blocks.append(
            f"【片段{idx}｜来源：{source}｜相关度：{score:.2f}】\n{doc.page_content}"
        )
    return "\n\n".join(blocks)
