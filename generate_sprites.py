#!/usr/bin/env python3
"""Generates a placeholder sprite sheet for TamaHero.

Buildings: green rounded rectangles with initials, tile-aligned sizes.
Units: green circles with initials, tile-aligned sizes.
All sizes are multiples of TILE (32px).
Output: a single PNG sprite sheet + JSON atlas.
"""

from PIL import Image, ImageDraw, ImageFont
import json

TILE = 32
BG = (0, 0, 0, 0)
FG = (0, 255, 0, 255)
FG_DEFENSE = (255, 80, 80, 255)
FG_TRAP = (255, 200, 0, 255)
STROKE = 2
RADIUS = 8

# (label, full_name, kind, tiles_w, tiles_h)
SPRITES = [
    # Big buildings (4x4 = 128x128)
    ("TH", "town_hall", "building", 4, 4),
    # Large buildings (4x2 = 128x64)
    ("BK", "barracks", "building", 4, 2),
    ("AC", "army_camp", "building", 4, 2),
    # Medium buildings (2x2 = 64x64)
    ("LC", "lumber_camp", "building", 2, 2),
    ("GM", "gold_mine", "building", 2, 2),
    ("FG", "forge", "building", 2, 2),
    ("WS", "wood_storage", "building", 2, 2),
    ("GS", "gold_storage", "building", 2, 2),
    ("MS", "metal_storage", "building", 2, 2),
    ("LB", "laboratory", "building", 2, 2),
    # Defenses (2x2 = 64x64)
    ("CN", "cannon", "defense", 2, 2),
    ("AT", "archer_tower", "defense", 2, 2),
    ("MT", "mortar", "defense", 2, 2),
    ("SD", "shield_dome", "defense", 2, 2),
    # Wall (1x1 = 32x32)
    ("WL", "wall", "defense", 1, 1),
    # Traps (1x1 = 32x32)
    ("SK", "spike_trap", "trap", 1, 1),
    ("SP", "spring_trap", "trap", 1, 1),
    # Units - standard (1x1 = 32x32)
    ("HS", "human_soldier", "unit", 1, 1),
    ("EA", "elf_archer", "unit", 1, 1),
    ("DS", "dwarf_sapper", "unit", 1, 1),
    # Units - big (2x2 = 64x64)
    ("OB", "orc_berserker", "unit", 2, 2),
]

try:
    font_large = ImageFont.truetype("/System/Library/Fonts/Menlo.ttc", 18)
    font_medium = ImageFont.truetype("/System/Library/Fonts/Menlo.ttc", 13)
    font_small = ImageFont.truetype("/System/Library/Fonts/Menlo.ttc", 10)
except (OSError, IOError):
    font_large = ImageFont.load_default()
    font_medium = font_large
    font_small = font_large


def pick_font(tiles):
    if tiles >= 4:
        return font_large
    if tiles >= 2:
        return font_medium
    return font_small


# Pack sprites row by row, sorted by height descending then width descending
sorted_sprites = sorted(SPRITES, key=lambda s: (-s[4], -s[3]))

SHEET_W_TILES = 16
SHEET_W = SHEET_W_TILES * TILE
placements = []
cursor_x = 0
cursor_y = 0
row_height = 0

for label, name, kind, tw, th in sorted_sprites:
    w = tw * TILE
    h = th * TILE
    if cursor_x + w > SHEET_W:
        cursor_y += row_height
        cursor_x = 0
        row_height = 0
    placements.append((label, name, kind, tw, th, w, h, cursor_x, cursor_y))
    cursor_x += w
    row_height = max(row_height, h)

sheet_h = cursor_y + row_height

img = Image.new("RGBA", (SHEET_W, sheet_h), BG)
draw = ImageDraw.Draw(img)

atlas = {}

for label, name, kind, tw, th, w, h, x0, y0 in placements:
    cx = x0 + w // 2
    cy = y0 + h // 2
    margin = 3

    if kind == "building":
        draw.rounded_rectangle(
            [x0 + margin, y0 + margin, x0 + w - margin - 1, y0 + h - margin - 1],
            radius=RADIUS,
            outline=FG,
            width=STROKE,
        )
        color = FG
    elif kind == "defense":
        draw.rounded_rectangle(
            [x0 + margin, y0 + margin, x0 + w - margin - 1, y0 + h - margin - 1],
            radius=RADIUS,
            outline=FG_DEFENSE,
            width=STROKE,
        )
        color = FG_DEFENSE
    elif kind == "trap":
        # Diamond shape for traps
        draw.polygon(
            [(cx, y0 + margin), (x0 + w - margin, cy), (cx, y0 + h - margin), (x0 + margin, cy)],
            outline=FG_TRAP,
        )
        color = FG_TRAP
    else:
        r = min(w, h) // 2 - margin
        draw.ellipse(
            [cx - r, cy - r, cx + r, cy + r],
            outline=FG,
            width=STROKE,
        )
        color = FG

    font = pick_font(max(tw, th))
    bbox = font.getbbox(label)
    text_w = bbox[2] - bbox[0]
    text_h = bbox[3] - bbox[1]
    draw.text((cx - text_w // 2, cy - text_h // 2 - 1), label, fill=color, font=font)

    atlas[name] = {
        "label": label,
        "kind": kind,
        "x": x0,
        "y": y0,
        "width": w,
        "height": h,
        "tiles_w": tw,
        "tiles_h": th,
    }

out_dir = "composeApp/src/commonMain/composeResources/drawable"
img.save(f"{out_dir}/sprites.png")

with open(f"{out_dir}/sprites.json", "w") as f:
    json.dump(atlas, f, indent=2)

print(f"Sprite sheet: {out_dir}/sprites.png ({SHEET_W}x{sheet_h})")
print(f"Atlas: {out_dir}/sprites.json")
print(f"Sprites ({len(placements)}):")
for label, name, kind, tw, th, w, h, x, y in placements:
    print(f"  {label:2s} ({name:16s}) {kind:8s} {tw}x{th} tiles ({w:3d}x{h:3d}px) at ({x},{y})")
