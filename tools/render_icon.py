from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


SIZE = 512
SCALE = 3
CANVAS = SIZE * SCALE
OUTPUT = Path(__file__).parents[1] / "src/main/resources/assets/sodiumcoreshadersupport/icon.png"


def point(x: float, y: float) -> tuple[int, int]:
    return round(x * SCALE), round(y * SCALE)


def polygon(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], fill: str) -> None:
    draw.polygon([point(x, y) for x, y in points], fill=fill)


image = Image.new("RGB", (CANVAS, CANVAS))
draw = ImageDraw.Draw(image)

for y in range(SIZE):
    t = y / (SIZE - 1)
    top = (8, 18, 33)
    bottom = (17, 38, 57)
    color = tuple(round(a + (b - a) * t) for a, b in zip(top, bottom))
    draw.line((0, y * SCALE, CANVAS, y * SCALE), fill=color, width=SCALE)

glow = Image.new("RGBA", image.size, (0, 0, 0, 0))
glow_draw = ImageDraw.Draw(glow)
glow_draw.ellipse((190 * SCALE, 65 * SCALE, 460 * SCALE, 400 * SCALE), fill=(49, 217, 195, 78))
glow_draw.ellipse((70 * SCALE, 190 * SCALE, 345 * SCALE, 450 * SCALE), fill=(90, 117, 245, 50))
glow = glow.filter(ImageFilter.GaussianBlur(56 * SCALE))
image = Image.alpha_composite(image.convert("RGBA"), glow)
draw = ImageDraw.Draw(image)

draw.rounded_rectangle(
    (34 * SCALE, 34 * SCALE, 478 * SCALE, 478 * SCALE),
    radius=34 * SCALE,
    outline=(105, 151, 174, 120),
    width=2 * SCALE,
)

# Three linked faces form a shader cube; each face uses a different render-pass color.
top = [(256, 92), (385, 166), (256, 241), (127, 166)]
left = [(127, 166), (256, 241), (256, 390), (127, 315)]
right = [(385, 166), (256, 241), (256, 390), (385, 315)]
polygon(draw, top, "#41CDB0")
polygon(draw, left, "#214F76")
polygon(draw, right, "#5C55A4")

# Cutout-like tiles on the left face and translucent bands on the right face.
polygon(draw, [(145, 218), (238, 271), (238, 296), (145, 243)], "#45D5B8")
polygon(draw, [(145, 255), (238, 308), (238, 333), (145, 280)], "#85E6C8")
polygon(draw, [(367, 219), (274, 273), (274, 297), (367, 243)], "#84E7E1")
polygon(draw, [(367, 258), (274, 312), (274, 336), (367, 282)], "#C6C6FF")

# A nested prism suggests the shader core without using the upstream project avatar.
polygon(draw, [(256, 123), (328, 165), (256, 207), (184, 165)], "#153144")
polygon(draw, [(256, 139), (310, 170), (256, 201), (202, 170)], "#61F0D1")
polygon(draw, [(256, 151), (283, 167), (256, 183), (229, 167)], "#F4D06F")
polygon(draw, [(256, 167), (283, 183), (256, 199), (229, 183)], "#F7FBFF")

edge = (211, 255, 246, 255)
draw.line([point(127, 166), point(256, 92), point(385, 166)], fill=edge, width=6 * SCALE, joint="curve")
draw.line([point(127, 166), point(127, 315), point(256, 390), point(385, 315), point(385, 166)], fill=edge, width=6 * SCALE, joint="curve")
draw.line([point(256, 241), point(256, 390)], fill=(166, 218, 255, 255), width=5 * SCALE)
draw.line([point(127, 166), point(256, 241), point(385, 166)], fill=(211, 255, 246, 255), width=6 * SCALE, joint="curve")

# Three small bars reinforce the separate solid, cutout, and translucent passes.
for x, color in ((173, "#52E1BC"), (226, "#F4D06F"), (279, "#9EBCFF")):
    draw.rounded_rectangle(
        (x * SCALE, 423 * SCALE, (x + 36) * SCALE, 432 * SCALE),
        radius=4 * SCALE,
        fill=color,
    )

image = image.convert("RGB").resize((SIZE, SIZE), Image.Resampling.LANCZOS)
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
image.save(OUTPUT, format="PNG", optimize=True)
print(f"Wrote {OUTPUT} ({image.width}x{image.height})")
