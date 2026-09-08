# -*- coding: utf-8 -*-
"""按集次生成占位视频：复用已有的集次封面图，叠加章节/集次标题与倒计时。

与 generate_mylesson_images.py 保持同一套路：从 DB 读元数据，输出到本地素材目录，
再由 upload_mylesson_videos.py 上传到 MinIO 并回写 episode.video。

每集画面上都写着自己的标题与倒计时，切集时能一眼确认视频真的换了。
"""
import subprocess
import sys
from pathlib import Path
from textwrap import wrap

import imageio_ffmpeg
import pymysql
from PIL import Image, ImageDraw, ImageFilter, ImageFont

# 本地素材根目录：与已有的 course-cover / episode-video-cover 同级
ASSET_ROOT = Path(r"C:/Users/xiaoyuzi/Desktop/文件/my-lesson图片")
COVER_DIR = ASSET_ROOT / "episode-video-cover"
OUT_DIR = ASSET_ROOT / "episode-video"

# 本次试点的课程
COURSE_ID = 1

# 视频参数：1280x720 与集次封面同尺寸，30 秒足够验证切集与弹幕
WIDTH, HEIGHT = 1280, 720
DURATION = 30
OUT_FPS = 15

DB_CONFIG = {
    "host": "192.168.211.132",
    "user": "root",
    "password": "123456",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
    "connect_timeout": 10,
}

FONT_CANDIDATES = [
    r"C:/Windows/Fonts/msyhbd.ttc",
    r"C:/Windows/Fonts/msyh.ttc",
    r"C:/Windows/Fonts/simhei.ttf",
    r"C:/Windows/Fonts/simsun.ttc",
]

ACCENT = (37, 99, 235)


def get_font(size: int):
    for candidate in FONT_CANDIDATES:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    return ImageFont.load_default()


def load_base(episode_id: int) -> Image.Image:
    """取集次封面作底图：重度模糊+压暗，只留配色，避免封面自带文字与叠加层打架。"""
    cover = COVER_DIR / f"episode-{episode_id}.png"
    if cover.exists():
        img = Image.open(cover).convert("RGB").resize((WIDTH, HEIGHT))
        img = img.filter(ImageFilter.GaussianBlur(radius=42))
    else:
        img = Image.new("RGB", (WIDTH, HEIGHT), "#1E3A8A")
    overlay = Image.new("RGB", (WIDTH, HEIGHT), (15, 23, 42))
    return Image.blend(img, overlay, 0.62)


def wrap_cn(text: str, width: int, max_lines: int):
    lines = wrap(text or "", width=width, break_long_words=True, drop_whitespace=False)
    if not lines:
        return [""]
    if len(lines) > max_lines:
        lines = lines[:max_lines]
        lines[-1] = lines[-1][:-2] + ".." if len(lines[-1]) > 2 else lines[-1]
    return lines


def draw_frame(base: Image.Image, row: dict, second: int) -> Image.Image:
    """画第 second 秒的画面：静态信息 + 秒表/进度条这类会动的部分。"""
    img = base.copy()
    draw = ImageDraw.Draw(img, "RGBA")

    badge_font = get_font(30)
    title_font = get_font(60)
    meta_font = get_font(28)
    timer_font = get_font(96)

    # 章节 / 集次标记
    chapter = f"第{row['season_idx']}章 第{row['idx']}集"
    badge_w = draw.textlength(chapter, font=badge_font)
    draw.rounded_rectangle((72, 66, 72 + 52 + badge_w, 128), radius=18, fill=ACCENT + (230,))
    draw.text((98, 80), chapter, font=badge_font, fill="#FFFFFF")

    # 集次标题：切集时最直观的判断依据
    y = 200
    for line in wrap_cn(row["title"], 15, 3):
        draw.text((76, y), line, font=title_font, fill="#F8FAFC")
        y += 74

    # 所属章节名与课程名
    draw.text((78, y + 24), f"{row['season_title']} · {row['course_title']}", font=meta_font, fill="#94A3B8")

    # 倒计时秒表：会动，用来确认视频确实在播。右对齐，避免长文本溢出画面
    remain = DURATION - second
    timer_text = f"{remain // 60:02d}:{remain % 60:02d}"
    right = WIDTH - 76
    draw.text((right - draw.textlength(timer_text, font=timer_font), 84), timer_text,
              font=timer_font, fill="#F8FAFC")
    tag = "占位素材 PLACEHOLDER"
    draw.text((right - draw.textlength(tag, font=meta_font), 196), tag, font=meta_font, fill="#94A3B8")

    # 进度条
    ratio = second / max(DURATION - 1, 1)
    draw.rounded_rectangle((76, 636, WIDTH - 76, 656), radius=10, fill=(255, 255, 255, 40))
    draw.rounded_rectangle((76, 636, 76 + int((WIDTH - 152) * ratio), 656), radius=10, fill=ACCENT + (255,))
    # 进度游标
    cx = 76 + int((WIDTH - 152) * ratio)
    draw.ellipse((cx - 16, 630, cx + 16, 662), fill="#F8FAFC")

    draw.text((76, 676), f"episode-{row['id']}.mp4   {second + 1}/{DURATION}s", font=meta_font, fill="#64748B")
    return img


def encode(row: dict, out_path: Path):
    """逐秒生成帧，raw 管道喂给 ffmpeg，输出带静音音轨的 H.264 MP4。"""
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    cmd = [
        ffmpeg, "-y", "-loglevel", "error",
        # 视频：每秒 1 帧的原始流
        "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{WIDTH}x{HEIGHT}",
        "-framerate", "1", "-i", "pipe:0",
        # 音频：静音轨，避免部分播放器对无音轨 MP4 的异常处理
        "-f", "lavfi", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100",
        "-shortest",
        "-c:v", "libx264", "-preset", "veryfast", "-crf", "28",
        "-pix_fmt", "yuv420p", "-r", str(OUT_FPS),
        "-c:a", "aac", "-b:a", "64k",
        "-movflags", "+faststart",
        str(out_path),
    ]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stderr=subprocess.PIPE)
    base = load_base(row["id"])
    try:
        for second in range(DURATION):
            proc.stdin.write(draw_frame(base, row, second).tobytes())
        proc.stdin.close()
    except BrokenPipeError:
        pass
    _, err = proc.communicate()
    if proc.returncode != 0:
        raise RuntimeError(f"ffmpeg failed for episode {row['id']}: {err.decode('utf-8', 'replace')}")


def fetch_episodes(course_id: int):
    conn = pymysql.connect(database="ml_cms", **DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                SELECT e.id, e.title, e.idx,
                       s.idx AS season_idx, s.title AS season_title,
                       c.title AS course_title
                FROM episode e
                JOIN season s ON e.fk_season_id = s.id
                JOIN course c ON s.fk_course_id = c.id
                WHERE c.id = %s
                ORDER BY s.idx ASC, e.idx ASC
                """,
                (course_id,),
            )
            return cursor.fetchall()
    finally:
        conn.close()


def main():
    course_id = int(sys.argv[1]) if len(sys.argv) > 1 else COURSE_ID
    only_ids = {int(x) for x in sys.argv[2:]} if len(sys.argv) > 2 else None

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    rows = fetch_episodes(course_id)
    if only_ids:
        rows = [r for r in rows if r["id"] in only_ids]
    if not rows:
        print(f"no episodes for course {course_id}")
        return

    total_bytes = 0
    for index, row in enumerate(rows, start=1):
        out_path = OUT_DIR / f"episode-{row['id']}.mp4"
        encode(row, out_path)
        size = out_path.stat().st_size
        total_bytes += size
        print(f"[{index}/{len(rows)}] episode-{row['id']}.mp4  {size / 1024:.0f} KB  {row['title']}")

    print(f"done: {OUT_DIR}")
    print(f"count={len(rows)} total={total_bytes / 1024 / 1024:.1f} MB")


if __name__ == "__main__":
    main()
