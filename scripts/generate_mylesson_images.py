# -*- coding: utf-8 -*-
from pathlib import Path
from textwrap import wrap

import pymysql
from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent / "generated-images"
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
    "connect_timeout": 10,
}

FONT_CANDIDATES = [
    r"C:\Windows\Fonts\msyh.ttc",
    r"C:\Windows\Fonts\msyhbd.ttc",
    r"C:\Windows\Fonts\simhei.ttf",
    r"C:\Windows\Fonts\simsun.ttc",
]

# Light theme palettes: (bg_start, bg_end, accent, text)
PALETTES = [
    ("#EFF6FF", "#DBEAFE", "#2563EB", "#1E3A8A"),
    ("#F0FDF4", "#DCFCE7", "#16A34A", "#14532D"),
    ("#FFF7ED", "#FFEDD5", "#EA580C", "#7C2D12"),
    ("#FDF4FF", "#FAE8FF", "#9333EA", "#581C87"),
    ("#F0F9FF", "#E0F2FE", "#0284C7", "#0C4A6E"),
    ("#FFF1F2", "#FFE4E6", "#E11D48", "#881337"),
    ("#FFFBEB", "#FEF3C7", "#D97706", "#78350F"),
    ("#F5F3FF", "#EDE9FE", "#7C3AED", "#4C1D95"),
]


def ensure_dirs():
    for path in DIRS.values():
        path.mkdir(parents=True, exist_ok=True)


def get_font(size: int):
    for candidate in FONT_CANDIDATES:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    return ImageFont.load_default()


def get_palette(seed: int):
    return PALETTES[seed % len(PALETTES)]


def hex_rgba(hex_color: str, alpha: int = 255):
    hex_color = hex_color.lstrip("#")
    r = int(hex_color[0:2], 16)
    g = int(hex_color[2:4], 16)
    b = int(hex_color[4:6], 16)
    return (r, g, b, alpha)


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

    draw.rounded_rectangle((80, 70, 420, 130), radius=20, fill=hex_rgba(palette[2], 48))
    draw.rounded_rectangle((80, 160, 1100, 480), radius=28, fill=(255, 255, 255, 200))
    draw.ellipse((1200, -60, 1580, 320), fill=hex_rgba(palette[2], 30))
    draw.rectangle((100, 500, 340, 512), fill=palette[2])

    badge_font = get_font(28)
    title_font = get_font(58)
    body_font = get_font(26)

    raw_info = (row["info"] or "").replace("《", "").replace("》", "")
    headline = raw_info[:16] if raw_info else f"Banner {row['id']}"
    desc = raw_info[:80] if raw_info else "MyLesson 推荐内容"

    draw.text((100, 82), "MyLesson 精选推荐", font=badge_font, fill=palette[2])
    draw.text((100, 185), headline, font=title_font, fill=palette[3])
    draw_text_block(draw, wrap_cn(desc, 26, 4), 102, 295, body_font, "#475569", 18)
    draw.rounded_rectangle((1020, 420, 1180, 490), radius=24, fill=palette[2])
    draw.text((1050, 438), "立即查看", font=badge_font, fill="#FFFFFF")

    img.save(DIRS["banner"] / f"banner-{row['id']}.png")


def create_course_image(row):
    palette = get_palette(row["id"])
    img = gradient_image((800, 1000), palette[0], palette[1])
    draw = ImageDraw.Draw(img, "RGBA")

    draw.rounded_rectangle((50, 50, 750, 950), radius=32, fill=(255, 255, 255, 210))
    draw.rounded_rectangle((80, 100, 260, 150), radius=16, fill=hex_rgba(palette[2], 40))
    draw.rectangle((80, 860, 220, 872), fill=palette[2])

    badge_font = get_font(24)
    title_font = get_font(46)
    foot_font = get_font(24)

    draw.text((96, 108), "精品课程", font=badge_font, fill=palette[2])
    draw_text_block(draw, wrap_cn(row["title"], 9, 5), 82, 260, title_font, palette[3], 32)
    draw.text((82, 890), "MyLesson", font=foot_font, fill="#64748B")
    draw.text((82, 928), "实战课程 / 系统学习 / 随学随练", font=foot_font, fill="#94A3B8")

    img.save(DIRS["course"] / f"course-{row['id']}.png")


def create_episode_image(row):
    palette = get_palette(row["id"])
    img = gradient_image((1280, 720), palette[0], palette[1], horizontal=True)
    draw = ImageDraw.Draw(img, "RGBA")

    draw.rounded_rectangle((60, 80, 880, 560), radius=28, fill=(255, 255, 255, 215))
    draw.ellipse((920, -40, 1260, 300), fill=hex_rgba(palette[2], 24))
    draw.rectangle((80, 590, 280, 602), fill=palette[2])
    draw.polygon([(960, 240), (960, 400), (1100, 320)], fill=palette[2])

    badge_font = get_font(26)
    title_font = get_font(40)
    desc_font = get_font(24)

    draw.text((88, 110), "精品视频", font=badge_font, fill=palette[2])
    draw_text_block(draw, wrap_cn(row["title"], 14, 4), 88, 200, title_font, palette[3], 22)
    draw.text((88, 620), "MyLesson 课程片段预览", font=desc_font, fill="#64748B")

    img.save(DIRS["episode"] / f"episode-{row['id']}.png")


def create_avatar_image(row):
    palette = get_palette(row["id"])
    img = gradient_image((512, 512), palette[0], palette[1])
    draw = ImageDraw.Draw(img, "RGBA")

    draw.ellipse((56, 56, 456, 456), fill=(255, 255, 255, 180))
    draw.ellipse((140, 120, 372, 340), fill=hex_rgba(palette[2], 32))
    draw.rounded_rectangle((130, 340, 382, 410), radius=20, fill=hex_rgba(palette[2], 48))

    label_font = get_font(26)
    title_font = get_font(56)

    nickname = (row.get("nickname") or row.get("username") or "U")[:1].upper()
    draw.text((200, 200), nickname, font=title_font, fill=palette[3])
    draw.text((168, 360), "MyLesson", font=label_font, fill=palette[2])

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
