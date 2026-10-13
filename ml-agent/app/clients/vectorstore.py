"""外挂知识库：本地 Chroma 向量库 + 百炼 Embedding。

—— 选型说明 ——

参考教程用的是「百炼 embedding + Redis 向量库（26379）」。本项目沿用百炼做
向量化，但向量库换成 Chroma，原因有两个：

1. 现有 Redis 是 6379 普通 Redis，没有 RediSearch 模块，做不了向量检索；
   照教程起 Redis Stack 意味着多一个容器要维护。
2. Chroma 是进程内的，持久化到 ml-agent/data/vectorstore 本地目录，
   不新增任何服务。在「环境不能乱」这条前提下这是代价最小的方案。

至于 Embedding 为什么不用 DeepSeek：DeepSeek 只提供对话接口，没有 embedding
接口，所以向量化必须另找一家。百炼的 text-embedding-v3 与教程一致且有免费额度。
"""

from typing import Optional

from loguru import logger

from app.conf.settings import settings

_embeddings = None
_store = None


def get_embeddings():
    """百炼 text-embedding-v3。"""
    global _embeddings
    if _embeddings is None:
        if not settings.embedding_ready:
            raise RuntimeError(
                "未配置 DASHSCOPE_API_KEY，无法使用知识库检索。"
                "请在 ml-agent/.env 中填入百炼 API Key。"
            )
        # 延迟导入：没装依赖时报错信息更清晰，也避免拖慢服务启动
        from langchain_community.embeddings import DashScopeEmbeddings

        _embeddings = DashScopeEmbeddings(
            model=settings.embedding_model,
            dashscope_api_key=settings.dashscope_api_key,
        )
    return _embeddings


def get_vector_store():
    """打开（或创建）本地持久化的 Chroma 集合。"""
    global _store
    if _store is None:
        from langchain_chroma import Chroma

        settings.vector_store_path.mkdir(parents=True, exist_ok=True)
        _store = Chroma(
            collection_name=settings.vector_collection,
            embedding_function=get_embeddings(),
            persist_directory=str(settings.vector_store_path),
        )
    return _store


def reset_cache() -> None:
    """ingest 脚本重建索引后清掉进程内缓存。"""
    global _store
    _store = None


def is_indexed() -> bool:
    """索引是否已建好。

    未建索引时检索工具要给出「知识库还没建」的明确提示，而不是返回空结果
    让模型误以为「知识库里没有这条信息」——这两种情况对用户的含义完全不同。
    """
    if not settings.vector_store_path.exists():
        return False
    try:
        return get_vector_store()._collection.count() > 0
    except Exception as exc:
        logger.debug(f"检查向量库状态失败：{exc}")
        return False


def search(query: str, top_k: Optional[int] = None) -> list:
    """相似度检索，返回 (Document, score) 列表。"""
    k = top_k or settings.retrieve_top_k
    store = get_vector_store()
    return store.similarity_search_with_relevance_scores(query, k=k)
