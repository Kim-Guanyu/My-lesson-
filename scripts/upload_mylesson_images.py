# -*- coding: utf-8 -*-
import json
from pathlib import Path

import pymysql
from minio import Minio
from minio.error import S3Error


ROOT = Path(r"C:\Users\xiaoyuzi\Desktop\文件\my-lesson图片")
BUCKET = "mylesson"
MINIO_ENDPOINTS = ["192.168.211.132:9000", "192.168.211.132:9001"]
MINIO_USER = "minioadmin"
MINIO_PASSWORD = "minioadmin"

DB_CONFIG = {
    "host": "192.168.211.132",
    "user": "root",
    "password": "123456",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
    "autocommit": False,
}

CONTENT_TYPES = {
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".webp": "image/webp",
    ".gif": "image/gif",
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


def upload_dir(client: Minio, local_dir: Path, object_dir: str):
    uploaded = 0
    for file_path in sorted(local_dir.glob("*")):
        if not file_path.is_file():
            continue
        object_name = f"{object_dir}/{file_path.name}"
        content_type = CONTENT_TYPES.get(file_path.suffix.lower(), "application/octet-stream")
        client.fput_object(BUCKET, object_name, str(file_path), content_type=content_type)
        uploaded += 1
    print(f"uploaded {object_dir}: {uploaded}")


def update_tables():
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            cursor.execute("UPDATE ml_sms.banner SET url = CONCAT('banner-', id, '.png')")
            cursor.execute("UPDATE ml_cms.course SET cover = CONCAT('course-', id, '.png')")
            cursor.execute("UPDATE ml_cms.episode SET cover = CONCAT('episode-', id, '.png')")
            cursor.execute("UPDATE ml_ums.user SET avatar = CONCAT('user-', id, '.png')")
            cursor.execute("UPDATE ml_sms.seckill_detail SET course_cover = CONCAT('course-', fk_course_id, '.png')")
        conn.commit()
        print("database updated")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def main():
    client, endpoint = get_minio_client()
    ensure_bucket_ready(client)

    upload_dir(client, ROOT / "banner", "banner")
    upload_dir(client, ROOT / "course-cover", "course-cover")
    upload_dir(client, ROOT / "episode-video-cover", "episode-video-cover")
    upload_dir(client, ROOT / "avatar", "avatar")

    update_tables()
    print(f"done endpoint={endpoint}")


if __name__ == "__main__":
    main()
