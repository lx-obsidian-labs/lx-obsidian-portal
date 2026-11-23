"""Small, explicit ADB QA helper for Vista Watch's isolated emulator; LX Obsidian Labs."""
import argparse
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADB = Path.home() / "AppData/Local/Android/Sdk/platform-tools/adb.exe"
SERIAL = "emulator-5580"

def adb(*args, binary=False):
    result = subprocess.run([str(ADB), "-s", SERIAL, *args], capture_output=True, timeout=120)
    if result.returncode:
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace"))
    return result.stdout if binary else result.stdout.decode("utf-8", errors="replace")

def nodes():
    output = adb("exec-out", "uiautomator", "dump", "/dev/tty")
    start = output.find("<?xml")
    if start < 0:
        raise RuntimeError("No UI hierarchy available")
    end = output.rfind("</hierarchy>") + len("</hierarchy>")
    return ET.fromstring(output[start:end]).iter("node")

parser = argparse.ArgumentParser()
parser.add_argument("action", choices=["inspect", "capture", "tap", "key", "swipe", "type"])
parser.add_argument("values", nargs="*")
args = parser.parse_args()
if args.action == "inspect":
    for node in nodes():
        label = node.get("text") or node.get("content-desc")
        if label:
            print(label, node.get("bounds"), "focused=" + node.get("focused", "false"))
elif args.action == "capture":
    name = args.values[0]
    if not re.fullmatch(r"[a-z0-9_-]+", name):
        raise ValueError("Use a simple screenshot name")
    output = ROOT / "artwork/screenshots"
    output.mkdir(parents=True, exist_ok=True)
    data = adb("exec-out", "screencap", "-p", binary=True)
    if not data.startswith(b"\x89PNG"):
        raise RuntimeError("Screenshot is not PNG")
    target = output / (name + ".png")
    target.write_bytes(data)
    print(str(target))
elif args.action == "tap":
    wanted = " ".join(args.values)
    for node in nodes():
        if wanted.lower() in (node.get("text", "") + " " + node.get("content-desc", "")).lower():
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
            adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
            print("Tapped:", wanted)
            break
    else:
        raise RuntimeError("Control not visible: " + wanted)
elif args.action == "key":
    print(adb("shell", "input", "keyevent", args.values[0]))
elif args.action == "swipe":
    print(adb("shell", "input", "swipe", *args.values))
elif args.action == "type":
    # Intended only for synthetic QA input, never production credentials.
    print(adb("shell", "input", "text", " ".join(args.values).replace(" ", "%s")))
