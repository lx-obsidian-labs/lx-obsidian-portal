"""Vista Watch artwork preparation — LX Obsidian Labs. Originals are never modified."""
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageOps
from generate_original_avatars import generate

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "images resources"
OUTPUT = ROOT / "app/src/main/res/drawable-nodpi"
MANIFEST = ROOT / "artwork/asset-manifest.json"

# These mappings were verified by visual inspection, not inferred from export filenames.
ASSETS = [
    ("img.png", "vista_onboarding_portrait", (1080, 1920), "First-time welcome background", 350_000),
    ("ChatGPT Image Nov 15, 2026, 11_27_26 AM-2.png", "vista_banner_movie_night", (1280, 720), "Home/TV movie discovery action", 250_000),
    ("ChatGPT Image Nov 15, 2026, 11_27_27 AM-3.png", "vista_season_christmas", (1280, 720), "Seasonal campaign with native countdown", 250_000),
    ("ChatGPT Image Nov 15, 2026, 11_27_28 AM-4.png", "vista_avatar_ai_assistant", (512, 512), "Reserved Vista AI identity", 80_000),
    ("ChatGPT Image Nov 15, 2026, 11_27_29 AM-5.png", "vista_avatar_male_01", (512, 512), "Selectable avatar: male_01", 80_000),
    ("ChatGPT Image Nov 15, 2026, 11_27_30 AM-6.png", "vista_avatar_female_01", (512, 512), "Selectable avatar: female_01", 80_000),
]
MISSING = ["vista_ai_discovery", "vista_banner_relax", "vista_premium_background", "vista_banner_discover",
           "vista_season_christmas_alt", "vista_onboarding_portrait_alt"]

def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    rows, hashes = [], set()
    extras = [(f"artwork/source-originals/vista_avatar_{name}.png", f"vista_avatar_{name}", (512, 512),
               f"Selectable original geometric avatar: {name}", 80_000) for name in generate()]
    for original, name, bounds, purpose, target in ASSETS + extras:
        source = ROOT / original if original.startswith("artwork/") else SOURCE / original
        digest = hashlib.sha256(source.read_bytes()).hexdigest()
        if digest in hashes:
            raise ValueError("Duplicate source artwork: " + original)
        hashes.add(digest)
        with Image.open(source) as raw:
            image = ImageOps.exif_transpose(raw).convert("RGB")
            original_size = image.size
            image.thumbnail(bounds, Image.Resampling.LANCZOS)
            output = OUTPUT / (name + ".webp")
            for quality in [88, 84, 80, 76]:
                image.save(output, "WEBP", quality=quality, method=6, exif=b"", icc_profile=b"")
                if output.stat().st_size <= target:
                    break
            with Image.open(output) as check:
                assert check.size == image.size and check.format == "WEBP"
            rows.append({"id": name, "filename": output.name, "original": original, "source_sha256": digest,
                         "source_dimensions": list(original_size), "dimensions": list(image.size),
                         "bytes": output.stat().st_size, "quality": quality, "purpose": purpose,
                         "sha256": hashlib.sha256(output.read_bytes()).hexdigest()})
    manifest = {"product": "Vista Watch", "developer": "LX Obsidian Labs", "assets": rows,
                "total_webp_bytes": sum(row["bytes"] for row in rows),
                "unavailable_from_supplied_directory": [name + ".webp" for name in MISSING]}
    MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"converted": len(rows), "total_webp_bytes": manifest["total_webp_bytes"],
                      "missing": manifest["unavailable_from_supplied_directory"]}, indent=2))

if __name__ == "__main__":
    main()
