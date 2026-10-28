"""Compatibility launcher: use the Firebase CLI login through the Node FCM HTTP v1 sender."""
import pathlib
import subprocess
import sys

if __name__ == "__main__":
    raise SystemExit(subprocess.call(["node", str(pathlib.Path(__file__).with_name("send_fcm_cli.mjs")), *sys.argv[1:]]))
