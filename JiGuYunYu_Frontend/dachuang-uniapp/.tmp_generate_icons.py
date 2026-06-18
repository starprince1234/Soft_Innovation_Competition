from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

out_dir = Path('src/static/icons')
out_dir.mkdir(parents=True, exist_ok=True)

icons = {
    'about': 'i',
    'artifacts': 'A',
    'chat': 'Q',
    'edit': 'E',
    'favorite': 'F',
    'feedback': 'B',
    'filter': 'L',
    'help': '?',
    'history': 'H',
    'scan': 'C',
    'search': 'S',
    'settings': 'G',
    'trash': 'T',
}

for name, glyph in icons.items():
    img = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle((8, 8, 120, 120), radius=28, fill=(250, 240, 230, 255), outline=(93, 64, 55, 255), width=6)
    try:
        font = ImageFont.truetype('arial.ttf', 64)
    except Exception:
        font = ImageFont.load_default()
    bbox = draw.textbbox((0, 0), glyph, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    draw.text(((128 - tw) / 2, (128 - th) / 2 - 2), glyph, fill=(93, 64, 55, 255), font=font)
    img.save(out_dir / f'{name}.png', format='PNG')
    print(f'generated {name}.png')
