"""Generate a local, non-personal support QR for Vista Watch / LX Obsidian Labs."""
import hashlib
import json
import re
from pathlib import Path
import qrcode

ROOT = Path(__file__).resolve().parents[1]
contacts = (ROOT / "app/src/main/java/com/example/vista/support/VistaContacts.kt").read_text(encoding="utf-8")
url = re.search(r'const val SUPPORT_PAGE = "([^"]+)"', contacts).group(1)
assert url == "https://www.lxobsidianportal.co.za/vista#vista-support"
qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=8, border=4)
qr.add_data(url); qr.make(fit=True)
image = qr.make_image(fill_color="black", back_color="white").convert("RGB")
output = ROOT / "app/src/main/res/drawable-nodpi/vista_support_qr.webp"
output.parent.mkdir(parents=True, exist_ok=True)
image.save(output, "WEBP", lossless=True, method=6)
manifest = ROOT / "artwork/support-qr.json"
manifest.parent.mkdir(parents=True, exist_ok=True)
manifest.write_text(json.dumps({"developer":"LX Obsidian Labs", "product":"Vista Watch", "content":url,
    "contains_user_information":False, "dimensions":list(image.size), "bytes":output.stat().st_size,
    "sha256":hashlib.sha256(output.read_bytes()).hexdigest()}, indent=2) + "\n", encoding="utf-8")
print("Generated official support URL QR; no user data included.")
