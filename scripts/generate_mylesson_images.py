# -*- coding: utf-8 -*-
from pathlib import Path
from textwrap import wrap

import pymysql
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"C:\Users\xiaoyuzi\Desktop\文件\my-lesson图片")
DIRS = {
    "banner": ROOT / "banner",
    "course": ROOT / "course-cover",
    "episode": ROOT / "episode-video-cover",
    "avatar": ROOT / "avatar",
}

DB_CONFIG = {
    "host": "192.168.211.132",
    "user": "root",
    "password": "123456",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
}

FONT_CANDIDATES = [
    r"C:\Windows\Fonts\msyh.ttc",
    r"C:\Windows\Fonts\msyhbd.ttc",
    r"C:\Windows\Fonts\simhei.ttf",
    r"C:\Windows\Fonts\simsun.ttc",
]

PALETTES = [
    ("#0F172A", "#2563EB", "#38BDF8", "#F8FAFC"),
    ("#111827", "#7C3AED", "#C084FC", "#F9FAFB"),
    ("#052E16", "#16A34A", "#4ADE80", "#F0FDF4"),
    ("#3F0D12", "#E11D48", "#FB7185", "#FFF1F2"),
    ("#1E1B4B", "#4F46E5", "#818CF8", "#EEF2FF"),
    ("#3B0764", "#9333EA", "#D8B4FE", "#FAF5FF"),
    ("#082F49", "#0284C7", "#7DD3FC", "#F0F9FF"),
    ("#422006", "#F59E0B", "#FCD34D", "#FFFBEB"),
]


def ensure_dirs():
    for path in DIRS.values():
        path.mkdir(parents=True, exist_ok=True)


def get_font(size: int, bold: bool = False):
    for candidate in FONT_CANDIDATES:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    return ImageFont.load_default()


def get_palette(seed: int):
    return PALETTES[seed % len(PALETTES)]


def gradient_image(size, start_hex, end_hex, horizontal=False):
    width, height = size
    base = Image.new("RGBA", size, start_hex)
    top = Image.new("RGBA", size, end_hex)
    mask = Image.new("L", size)
    mask_data = []
    steps = width if horizontal else height
    for y in range(height):
        for x in range(width):
            index = x if horizontal else y
            mask_data.append(int(255 * (index / max(steps - 1, 1))))
    mask.putdata(mask_data)
    return Image.composite(top, base, mask)


def wrap_cn(text: str, width: int, max_lines: int):
    lines = wrap(text or "", width=width, break_long_words=True, drop_whitespace=False)
    if not lines:
        return [""]
    if len(lines) > max_lines:
        lines = lines[:max_lines]
        lines[-1] = lines[-1][:-2] + ".." if len(lines[-1]) > 2 else lines[-1]
    return lines


def draw_text_block(draw, lines, x, y, font, fill, line_spacing):
    for line in lines:
        draw.text((x, y), line, font=font, fill=fill)
        y += font.size + line_spacing


def create_banner_image(row):
    palette = get_palette(row["id"])
    img = gradient_image((1600, 600), palette[0], palette[1], horizontal=True)
    draw = ImageDraw.Draw(img, "RGBA")

    draw.rounded_rectangle((80, 70, 900, 150), radius=20, fill=(255, 255, 255, 25))
    draw.rounded_rectangle((80, 170, 1180, 470), radius=26, fill=(0, 0, 0, 50))
    draw.ellipse((1130, -100, 1640, 410), fill=(255, 255, 255, 40))
    draw.ellipse((1040, 240, 1450, 650), fill=(255, 255, 255, 28))
    draw.rectangle((100, 505, 360, 518), fill=palette[2])

    title_font = get_font(36)
    hero_font = get_font(66)
    body_font = get_font(24)

    raw_info = (row["info"] or "").replace("《", "").replace("》", "")
    headline = raw_info[:16] if raw_info else f"Banner {row['id']}"
    desc = raw_info[:80] if raw_info else "MyLesson 推荐内容"

    draw.text((100, 88), "MyLesson 精选推荐", font=title_font, fill="#FFFFFF")
    draw.text((100, 195), headline, font=hero_font, fill="#FFFFFF")
    draw_text_block(draw, wrap_cn(desc, 24, 4), 102, 305, body_font, "#E5F3FF", 16)
    draw.text((1200, 468), "立即查看", font=title_font, fill="#FFFFFF")

    img.save(DIRS["banner"] / f"banner-{row['id']}.png")


def create_course_image(row):
    palette = get_palette(row["id"])
    img = gradient_image((800, 1000), palette[0], palette[1])
    draw = ImageDraw.Draw(img, "RGBA")

    draw.ellipse((460, -80, 790, 250), fill=(255, 255, 255, 24))
    draw.rounded_rectangle((60, 120, 320, 176), radius=18, fill=(255, 255, 255, 35))
    draw.rounded_rectangle((60, 220, 740, 780), radius=28, fill=(0, 0, 0, 58))
    draw.rectangle((60, 820, 205, 834), fill=palette[2])

    small_font = get_font(22)
    title_font = get_font(50)
    foot_font = get_font(26)

    draw.text((84, 132), "精品课程", font=small_font, fill="#FFFFFF")
    draw_text_block(draw, wrap_cn(row["title"], 9, 5), 82, 280, title_font, "#FFFFFF", 34)
    draw.text((82, 884), "MyLesson", font=foot_font, fill="#E5E7EB")
    draw.text((82, 924), "实战课程 / 系统学习 / 随学随练", font=foot_font, fill="#E5E7EB")

    img.save(DIRS["course"] / f"course-{row['id']}.png")


def create_episode_image(row):
    palette = get_palette(row["id"])
    img = gradient_image((1280, 720), palette[0], palette[1], horizontal=True)
    draw = ImageDraw.Draw(img, "RGBA")

    draw.ellipse((850, -70, 1260, 340), fill=(255, 255, 255, 28))
    draw.rounded_rectangle((70, 110, 900, 530), radius=26, fill=(0, 0, 0, 72))
    draw.rectangle((70, 580, 300, 594), fill=palette[2])
    draw.polygon([(980, 220), (980, 420), (1140, 320)], fill="#FFFFFF")

    badge_font = get_font(26)
    title_font = get_font(42)
    desc_font = get_font(24)

    draw.text((96, 140), "精品视频", font=badge_font, fill="#FFFFFF")
    draw_text_block(draw, wrap_cn(row["title"], 14, 4), 96, 220, title_font, "#FFFFFF", 24)
    draw.text((96, 620), "MyLesson 课程片段预览", font=desc_font, fill="#E2E8F0")

    img.save(DIRS["episode"] / f"episode-{row['id']}.png")


def create_avatar_image(row):
    palette = get_palette(row["id"])
    img = gradient_image((512, 512), palette[1], palette[2])
    draw = ImageDraw.Draw(img, "RGBA")

    draw.ellipse((0, 0, 512, 512), fill=None, outline=None)
    draw.ellipse((70, 70, 442, 442), fill=(255, 255, 255, 35))
    draw.rounded_rectangle((110, 330, 402, 410), radius=18, fill=(0, 0, 0, 36))

    label_font = get_font(28)
    title_font = get_font(64)

    draw.text((175, 116), "ADMIN", font=label_font, fill="#FFFFFF")
    mark = f"A{row['id']}"
    draw.text((170, 220), mark, font=title_font, fill="#FFFFFF")

    img.save(DIRS["avatar"] / f"user-{row['id']}.png")


def fetch_rows(database: str, sql: str):
    connection = pymysql.connect(database=database, **DB_CONFIG)
    try:
        with connection.cursor() as cursor:
            cursor.execute(sql)
            return cursor.fetchall()
    finally:
        connection.close()


def main():
    ensure_dirs()

    banners = fetch_rows("ml_sms", "SELECT id, info FROM banner ORDER BY id ASC")
    courses = fetch_rows("ml_cms", "SELECT id, title FROM course ORDER BY id ASC")
    episodes = fetch_rows("ml_cms", "SELECT id, title FROM episode ORDER BY id ASC")
    users = fetch_rows("ml_ums", "SELECT id, username, nickname FROM user ORDER BY id ASC")

    for row in banners:
        create_banner_image(row)
    for row in courses:
        create_course_image(row)
    for row in episodes:
        create_episode_image(row)
    for row in users:
        create_avatar_image(row)

    print(f"done: {ROOT}")
    print(f"banner={len(banners)} course={len(courses)} episode={len(episodes)} avatar={len(users)}")


if __name__ == "__main__":
    main()
