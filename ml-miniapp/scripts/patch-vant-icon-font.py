# -*- coding: utf-8 -*-
"""Replace Vant icon CDN font with embedded base64 (WeChat mini program)."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LESS = ROOT / "node_modules/@vant/icons/src/encode-woff2.less"

CDN_FACE = (
    '@font-face{font-display:auto;font-family:vant-icon;font-style:normal;font-weight:400;'
    'src:url(//at.alicdn.com/t/c/font_2553510_kfwma2yq1rs.woff2?t=1694918397022) format("woff2"),'
    'url(//at.alicdn.com/t/c/font_2553510_kfwma2yq1rs.woff?t=1694918397022) format("woff")}'
)

TARGETS = [
    ROOT / "miniprogram_npm/@vant/weapp/icon/index.wxss",
    ROOT / "node_modules/@vant/weapp/lib/icon/index.wxss",
    ROOT / "node_modules/@vant/weapp/dist/icon/index.wxss",
]


CDN_FALLBACK = (
    ", url('//at.alicdn.com/t/c/font_2553510_kfwma2yq1rs.woff?t=1694918397022') format('woff')"
)


def strip_cdn_fallback(text: str) -> str:
    return text.replace(CDN_FALLBACK, "")


def main():
    less = LESS.read_text(encoding="utf-8")
    match = re.search(r"@font-face\s*\{[^}]+\}", less, re.S)
    if not match:
        raise RuntimeError(f"font-face not found in {LESS}")

    font_face = match.group(0)
    # Drop CDN fallback — unusable in WeChat mini programs.
    font_face = re.sub(
        r",\s*url\('//at\.alicdn\.com[^']+'\)\s*format\('woff'\)",
        "",
        font_face,
    )
    font_face_min = re.sub(r"\s+", " ", font_face).strip()

    static_wxss = ROOT / "static/vant-icon.wxss"
    static_wxss.parent.mkdir(parents=True, exist_ok=True)
    static_wxss.write_text(strip_cdn_fallback(font_face) + "\n", encoding="utf-8")
    print(f"wrote {static_wxss}")

    font_face_min = re.sub(r"\s+", " ", strip_cdn_fallback(font_face)).strip()

    patched = 0
    for path in TARGETS:
        if not path.exists():
            print(f"skip missing: {path.relative_to(ROOT)}")
            continue
        text = path.read_text(encoding="utf-8")
        if CDN_FACE in text:
            text = text.replace(CDN_FACE, font_face_min, 1)
            patched += 1
        elif CDN_FALLBACK in text or "at.alicdn.com" in text:
            text = strip_cdn_fallback(text)
            if "@font-face" not in text:
                # extremely old build: inject before :host
                text = text.replace(":host{", font_face_min + " }:host{", 1)
            patched += 1
        else:
            print(f"skip already patched: {path.relative_to(ROOT)}")
            continue
        path.write_text(text, encoding="utf-8")
        print(f"patched {path.relative_to(ROOT)}")

    print(f"done, patched {patched} file(s)")


if __name__ == "__main__":
    main()
