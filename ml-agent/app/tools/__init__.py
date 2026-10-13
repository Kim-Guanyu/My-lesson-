"""小精灵可调用的工具集合。

区分两类「工具」（沿用参考项目的划分）：
  - Agent Tool（本目录）：给模型调用，有 docstring 和 Pydantic 参数 schema
  - Python Utils（app/utils/）：给代码自己调用，模型看不到

工具清单：
  search_knowledge            外挂知识库检索（RAG，回答学习类问题的首选）
  get_course_outline          课程介绍与完整目录
  search_courses              按关键词搜课（ES 中文分词）
  get_episode_info            某一节课时的标题与描述
  search_episode_questions    该节课的弹幕，了解同学们的常见困惑
  get_my_purchased_courses    我的已购课程（身份自动注入）
  get_my_orders               我的订单（身份自动注入）
"""

from app.tools.course_tools import (
    get_course_outline,
    get_episode_info,
    search_courses,
    search_episode_questions,
)
from app.tools.knowledge_tools import search_knowledge
from app.tools.order_tools import get_my_orders, get_my_purchased_courses

# 传给 create_agent(tools=...) 的完整列表
ALL_TOOLS = [
    search_knowledge,
    get_course_outline,
    get_episode_info,
    search_courses,
    search_episode_questions,
    get_my_purchased_courses,
    get_my_orders,
]

__all__ = ["ALL_TOOLS"]
