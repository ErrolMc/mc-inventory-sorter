"""Draws the mod icon: a chest panel with the sort buttons and sorted stacks, as 32x32 pixel art scaled to 128x128.

Run from the repo root:  python -I tools/make-icon.py
Writes src/main/resources/assets/inventory-sorter/icon.png. No dependencies beyond the standard library.
"""
import struct
import sys
import zlib
from pathlib import Path

SIZE = 32
SCALE = 4
OUT = Path("src/main/resources/assets/inventory-sorter/icon.png")

# Vanilla GUI colours, and the button colours from SortButton.java.
OUTLINE = (0x10, 0x10, 0x10)
HIGHLIGHT = (0xFF, 0xFF, 0xFF)
PANEL = (0xC6, 0xC6, 0xC6)
SHADOW = (0x55, 0x55, 0x55)
SLOT_DARK = (0x37, 0x37, 0x37)
SLOT_LIGHT = (0xFF, 0xFF, 0xFF)
SLOT_FILL = (0x8B, 0x8B, 0x8B)
LABEL = (0x40, 0x40, 0x40)
BUTTON = {"body": (0x6F, 0x6F, 0x6F), "light": (0x9A, 0x9A, 0x9A), "dark": (0x3F, 0x3F, 0x3F), "icon": (0xDD, 0xDD, 0xDD)}
BUTTON_HOVER = {"body": (0x6A, 0x78, 0xC8), "light": (0xA3, 0xAE, 0xF0), "dark": (0x3B, 0x43, 0x85), "icon": (0xFF, 0xFF, 0xFF)}
RED = ((0xFF, 0x7A, 0x7A), (0xD6, 0x30, 0x31), (0x8E, 0x1B, 0x1B))
BLUE = ((0x8C, 0xC8, 0xFF), (0x2A, 0x8C, 0xE8), (0x10, 0x55, 0x9A))
GOLD = ((0xFF, 0xE6, 0x80), (0xF0, 0xB0, 0x00), (0xA0, 0x72, 0x00))

# 4x4 button glyphs, matching the in-game icons.
GLYPHS = {
    "sort": ["####", "..#.", ".#..", "####"],
    "columns": ["#..#", "#..#", "#..#", "#..#"],
    "rows": ["####", "....", "....", "####"],
}
CUBE = [".ttt.", "ttttt", "LLLDD", "LLLDD", ".LLD."]

img = [[None] * SIZE for _ in range(SIZE)]


def px(x, y, c):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img[y][x] = c


def rect(x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(x, y, c)


def panel(x0, y0, x1, y1):
    """Vanilla-style GUI panel with rounded corners."""
    rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, PANEL)
    rect(x0 + 2, y0, x1 - 2, y0, OUTLINE)
    rect(x0 + 2, y1, x1 - 2, y1, OUTLINE)
    rect(x0, y0 + 2, x0, y1 - 2, OUTLINE)
    rect(x1, y0 + 2, x1, y1 - 2, OUTLINE)
    for cx, cy in ((x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)):
        px(cx, cy, OUTLINE)
    rect(x0 + 2, y0 + 1, x1 - 2, y0 + 1, HIGHLIGHT)
    rect(x0 + 1, y0 + 2, x0 + 1, y1 - 2, HIGHLIGHT)
    rect(x0 + 2, y1 - 1, x1 - 2, y1 - 1, SHADOW)
    rect(x1 - 1, y0 + 2, x1 - 1, y1 - 2, SHADOW)


def button(x, y, glyph, colours):
    rect(x, y, x + 5, y + 5, colours["body"])
    rect(x, y, x + 4, y, colours["light"])
    rect(x, y + 1, x, y + 4, colours["light"])
    rect(x + 1, y + 5, x + 5, y + 5, colours["dark"])
    rect(x + 5, y + 1, x + 5, y + 4, colours["dark"])
    for gy, row in enumerate(GLYPHS[glyph]):
        for gx, ch in enumerate(row):
            if ch == "#":
                px(x + 1 + gx, y + 1 + gy, colours["icon"])


def slot(x, y, item=None):
    """9x9 slot: dark top/left edge, light bottom/right edge, optional cube centred inside."""
    rect(x, y, x + 8, y + 8, SLOT_FILL)
    rect(x, y, x + 7, y, SLOT_DARK)
    rect(x, y + 1, x, y + 7, SLOT_DARK)
    rect(x + 1, y + 8, x + 8, y + 8, SLOT_LIGHT)
    rect(x + 8, y + 1, x + 8, y + 7, SLOT_LIGHT)
    if item:
        top, left, right = item
        for cy, row in enumerate(CUBE):
            for cx, ch in enumerate(row):
                colour = {"t": top, "L": left, "D": right}.get(ch)
                if colour:
                    px(x + 2 + cx, y + 2 + cy, colour)


panel(0, 0, 31, 31)
rect(3, 5, 8, 6, LABEL)  # the "Chest" title
button(10, 3, "sort", BUTTON)
button(17, 3, "columns", BUTTON)
button(24, 3, "rows", BUTTON_HOVER)
for col, item in enumerate((RED, RED, RED)):
    slot(3 + col * 9, 11, item)
for col, item in enumerate((BLUE, BLUE, GOLD)):
    slot(3 + col * 9, 20, item)


def write_png(path, pixels, scale):
    rows = []
    for row in pixels:
        line = bytearray()
        for c in row:
            rgba = (0, 0, 0, 0) if c is None else (*c, 255)
            line += bytes(rgba) * scale
        rows += [b"\x00" + bytes(line)] * scale
    width = len(pixels[0]) * scale
    height = len(pixels) * scale

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(b"".join(rows), 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else OUT
    write_png(out, img, SCALE)
    print(f"wrote {out} ({SIZE * SCALE}x{SIZE * SCALE})")
