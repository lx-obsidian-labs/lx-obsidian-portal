"""Original geometric Vista Watch avatars by LX Obsidian Labs. No third-party characters."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "artwork/source-originals"
NAMES = ["fox_01", "owl_01", "orbit_01", "ember_01", "summit_01", "moon_01"]

def generate():
    OUT.mkdir(parents=True, exist_ok=True)
    for name in NAMES:
        image = Image.new("RGB", (1024, 1024), "#0B0B0B")
        glow = Image.new("RGB", (1024, 1024), "#0B0B0B")
        draw = ImageDraw.Draw(glow)
        draw.ellipse((100, 60, 920, 940), fill="#421018")
        image = Image.blend(image, glow.filter(ImageFilter.GaussianBlur(90)), .75)
        d = ImageDraw.Draw(image)
        red, bright, grey, white = "#B90B19", "#E50914", "#242426", "#EEE8E5"
        if name == "fox_01":
            d.polygon([(260, 530), (230, 180), (445, 355), (580, 355), (795, 180), (765, 530), (660, 770), (512, 865), (365, 770)], fill=red)
            d.polygon([(270, 255), (290, 490), (410, 375)], fill=grey)
            d.polygon([(755, 255), (735, 490), (615, 375)], fill=grey)
            d.polygon([(280, 550), (465, 650), (512, 805), (380, 735)], fill=white)
            d.polygon([(745, 550), (560, 650), (512, 805), (645, 735)], fill=white)
            d.ellipse((358, 485, 413, 535), fill="#080808"); d.ellipse((612, 485, 667, 535), fill="#080808")
            d.polygon([(470, 738), (554, 738), (512, 794)], fill="#080808")
        elif name == "owl_01":
            d.polygon([(260, 380), (230, 180), (430, 290), (595, 290), (795, 180), (765, 380)], fill=red)
            d.ellipse((235, 270, 790, 855), fill=red)
            d.ellipse((280, 355, 510, 645), fill=white); d.ellipse((515, 355, 745, 645), fill=white)
            d.ellipse((340, 420, 455, 565), fill=grey); d.ellipse((570, 420, 685, 565), fill=grey)
            d.ellipse((378, 457, 420, 510), fill=bright); d.ellipse((606, 457, 648, 510), fill=bright)
            d.polygon([(470, 605), (555, 605), (512, 710)], fill=white)
            for y in [735, 785]: d.arc((420, y - 25, 605, y + 50), 0, 180, fill=grey, width=12)
        elif name == "orbit_01":
            d.ellipse((275, 260, 750, 735), fill=red, outline="#F43A44", width=12)
            d.arc((130, 375, 895, 650), 10, 170, fill=white, width=26)
            d.arc((130, 375, 895, 650), 185, 350, fill=bright, width=16)
            d.arc((335, 300, 725, 700), 200, 295, fill="#63111D", width=48)
            for x, y, r in [(210, 245, 10), (790, 245, 6), (815, 775, 10), (260, 800, 6)]: d.ellipse((x-r, y-r, x+r, y+r), fill=white)
        elif name == "ember_01":
            d.polygon([(505, 155), (645, 355), (610, 480), (760, 365), (810, 625), (730, 795), (510, 875), (300, 795), (220, 620), (335, 365), (390, 565), (470, 410)], fill=red)
            d.polygon([(510, 360), (605, 555), (570, 660), (660, 600), (655, 760), (510, 825), (375, 755), (370, 635)], fill=bright)
            d.polygon([(510, 570), (565, 705), (510, 790), (455, 705)], fill=white)
        elif name == "summit_01":
            d.ellipse((365, 190, 725, 550), fill=red)
            d.polygon([(130, 805), (415, 340), (600, 625), (720, 460), (905, 805)], fill=grey)
            d.polygon([(415, 340), (515, 495), (457, 470), (415, 510), (377, 470), (320, 495)], fill=white)
            d.polygon([(720, 460), (785, 580), (720, 555), (665, 580)], fill=white)
            d.line([(120, 827), (905, 827)], fill=bright, width=16)
        else:
            d.ellipse((260, 180, 760, 680), fill=white)
            d.ellipse((390, 115, 825, 600), fill="#181013")
            d.polygon([(100, 855), (315, 665), (510, 790), (695, 675), (925, 855)], fill=red)
            d.polygon([(95, 915), (375, 785), (580, 860), (765, 805), (940, 915)], fill=grey)
            for x, y in [(210, 410), (820, 345), (770, 585)]: d.ellipse((x-8, y-8, x+8, y+8), fill=bright)
        image.save(OUT / ("vista_avatar_" + name + ".png"), "PNG", optimize=True)
    return NAMES

if __name__ == "__main__":
    print("Generated original avatars:", ", ".join(generate()))
