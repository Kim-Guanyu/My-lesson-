# -*- coding: utf-8 -*-
"""上传集次占位视频到 MinIO 的 episode-video/ 目录，并回写 episode.video。

对应 ML.MinIO.EPISODE_VIDEO_DIR = "episode-video"，与 EpisodeServiceImpl.uploadVideo()
写入的目录一致，所以后台再手动换视频时能无缝接上。
"""
import json
import sys
from pathlib import Path

import pymysql
from minio import Minio

ASSET_ROOT = Path(r"C:/Users/xiaoyuzi/Desktop/文件/my-lesson图片")
LOCAL_DIR = ASSET_ROOT / "episode-video"

BUCKET = "my-lesson"
OBJECT_DIR = "episode-video"
MINIO_ENDPOINTS = ["192.168.211.132:9000", "192.168.211.132:9001"]
MINIO_USER = "minioadmin"
MINIO_PASSWORD = "minioadmin"

COURSE_ID = 1

DB_CONFIG = {
    "host": "192.168.211.132",
    "user": "root",
    "password": "123456",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
    "autocommit": False,
}


def get_minio_client():
    last_error = None
    for endpoint in MINIO_ENDPOINTS:
        try:
            client = Minio(endpoint, access_key=MINIO_USER, secret_key=MINIO_PASSWORD, secure=False)
            client.bucket_exists(BUCKET)
            print(f"connected minio: {endpoint}")
            return client, endpoint
        except Exception as exc:
            last_error = exc
    raise RuntimeError(f"minio connect failed: {last_error}")


def ensure_bucket_ready(client: Minio):
    if not client.bucket_exists(BUCKET):
        client.make_bucket(BUCKET)

    # 小程序 <video> 直接按 URL 拉流，需要匿名读
    public_policy = {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": {"AWS": ["*"]},
                "Action": ["s3:GetObject"],
                "Resource": [f"arn:aws:s3:::{BUCKET}/*"],
            }
        ],
    }
    client.set_bucket_policy(BUCKET, json.dumps(public_policy))


def episode_ids_of_course(course_id: int):
    conn = pymysql.connect(database="ml_cms", **DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                SELECT e.id FROM episode e
                JOIN season s ON e.fk_season_id = s.id
                WHERE s.fk_course_id = %s
                ORDER BY e.id ASC
                """,
                (course_id,),
            )
            return [row["id"] for row in cursor.fetchall()]
    finally:
        conn.close()


def upload_videos(client: Minio, episode_ids):
    uploaded = []
    for episode_id in episode_ids:
        file_path = LOCAL_DIR / f"episode-{episode_id}.mp4"
        if not file_path.is_file():
            print(f"skip missing: {file_path.name}")
            continue
        object_name = f"{OBJECT_DIR}/{file_path.name}"
        client.fput_object(BUCKET, object_name, str(file_path), content_type="video/mp4")
        uploaded.append(episode_id)
        print(f"uploaded {object_name}  {file_path.stat().st_size / 1024:.0f} KB")
    print(f"uploaded total: {len(uploaded)}")
    return uploaded


def update_episode_video(episode_ids):
    """只更新本次真正上传成功的集次，避免把 DB 指向不存在的对象。"""
    if not episode_ids:
        print("nothing to update")
        return
    conn = pymysql.connect(database="ml_cms", **DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            placeholders = ",".join(["%s"] * len(episode_ids))
            cursor.execute(
                f"UPDATE episode SET video = CONCAT('episode-', id, '.mp4'), updated = NOW() "
                f"WHERE id IN ({placeholders})",
                tuple(episode_ids),
            )
            affected = cursor.rowcount
        conn.commit()
        print(f"database updated: {affected} rows")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def main():
    course_id = int(sys.argv[1]) if len(sys.argv) > 1 else COURSE_ID

    client, endpoint = get_minio_client()
    ensure_bucket_ready(client)

    episode_ids = episode_ids_of_course(course_id)
    if not episode_ids:
        print(f"no episodes for course {course_id}")
        return
    print(f"course {course_id}: {len(episode_ids)} episodes")

    uploaded = upload_videos(client, episode_ids)
    update_episode_video(uploaded)
    print(f"done endpoint={endpoint}")


if __name__ == "__main__":
    main()
