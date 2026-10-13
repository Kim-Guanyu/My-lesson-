"""建知识库索引：读 knowledge/ 目录 -> 切片 -> 向量化 -> 写入 Chroma。

用法（在 ml-agent 目录下，虚拟环境已激活）：
    python scripts/ingest.py            # 增量：保留已有索引再追加
    python scripts/ingest.py --rebuild  # 重建：先清空索引再全量导入

支持 .md / .txt / .pdf / .docx（pdf 和 docx 需要 pip install -e ".[docs]"）。

流程沿用参考教程第 19 章的 RAG 索引阶段：加载 -> 分割 -> 向量化入库。
区别是向量库用本地 Chroma 而不是 Redis Stack，不需要额外起容器。
"""

import argparse
import shutil
import sys
from pathlib import Path

# 让脚本能 import app.*
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from langchain_core.documents import Document  # noqa: E402
from langchain_text_splitters import RecursiveCharacterTextSplitter  # noqa: E402

from app.clients import vectorstore  # noqa: E402
from app.conf.settings import settings  # noqa: E402

# 扩展名 -> 加载器。pdf / docx 的加载器延迟导入，没装依赖也不影响 md / txt
SUPPORTED = {".md", ".txt", ".pdf", ".docx"}


def load_file(path: Path) -> list[Document]:
    """按扩展名选加载器读一个文件。"""
    suffix = path.suffix.lower()

    if suffix in {".md", ".txt"}:
        # md / txt 直接读，不引 unstructured，省掉一大堆重依赖
        text = path.read_text(encoding="utf-8")
        return [Document(page_content=text)]

    if suffix == ".pdf":
        try:
            from langchain_community.document_loaders import PyPDFLoader
        except ImportError:
            print(f"  跳过 {path.name}：需要 pip install pypdf")
            return []
        return PyPDFLoader(str(path)).load()

    if suffix == ".docx":
        try:
            from langchain_community.document_loaders import Docx2txtLoader
        except ImportError:
            print(f"  跳过 {path.name}：需要 pip install docx2txt")
            return []
        return Docx2txtLoader(str(path)).load()

    return []


def collect_documents(root: Path) -> list[Document]:
    """遍历知识库目录，加载并打好来源标记。"""
    docs: list[Document] = []
    files = sorted(p for p in root.rglob("*") if p.suffix.lower() in SUPPORTED)

    if not files:
        print(f"知识库目录为空：{root}")
        print("请放入 .md / .txt / .pdf / .docx 文档后重新运行。")
        return docs

    for path in files:
        loaded = load_file(path)
        if not loaded:
            continue
        rel = path.relative_to(root)
        for d in loaded:
            # source_name 会在检索结果里回显给模型，让它能说明出处
            d.metadata["source"] = str(rel)
            d.metadata["source_name"] = path.stem
        docs.extend(loaded)
        print(f"  已加载 {rel}（{len(loaded)} 段）")

    return docs


def main():
    parser = argparse.ArgumentParser(description="建立小精灵知识库索引")
    parser.add_argument(
        "--rebuild", action="store_true", help="先清空已有索引再全量导入"
    )
    args = parser.parse_args()

    if not settings.embedding_ready:
        print("错误：未配置 DASHSCOPE_API_KEY。")
        print("向量化需要百炼 API Key（DeepSeek 不提供 embedding 接口）。")
        print("请在 ml-agent/.env 中填入后重试。")
        sys.exit(1)

    knowledge_dir = settings.knowledge_path
    if not knowledge_dir.exists():
        print(f"错误：知识库目录不存在：{knowledge_dir}")
        sys.exit(1)

    print(f"知识库目录：{knowledge_dir}")
    documents = collect_documents(knowledge_dir)
    if not documents:
        sys.exit(1)

    # 切片：中文课程文档 500/80 比较合适，太大检索不准，太小丢上下文
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=settings.chunk_size,
        chunk_overlap=settings.chunk_overlap,
        length_function=len,
        # 优先按中文段落和句子边界切，避免把一句话劈成两半
        separators=["\n\n", "\n", "。", "；", "，", " ", ""],
    )
    chunks = splitter.split_documents(documents)
    print(f"\n共 {len(documents)} 段原文，切成 {len(chunks)} 个片段")

    if args.rebuild and settings.vector_store_path.exists():
        print(f"清空已有索引：{settings.vector_store_path}")
        shutil.rmtree(settings.vector_store_path)
        vectorstore.reset_cache()

    print(f"正在向量化并写入 Chroma（模型 {settings.embedding_model}）...")
    store = vectorstore.get_vector_store()
    store.add_documents(chunks)

    print(f"\n完成。索引位置：{settings.vector_store_path}")
    print(f"当前索引片段总数：{store._collection.count()}")


if __name__ == "__main__":
    main()
