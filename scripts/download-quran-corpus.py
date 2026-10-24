#!/usr/bin/env python3
"""Download pinned public Quran snapshots for the offline search corpus."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import urllib.request

REVISION = "6523a9ad3b73bb7e8cb3a9cdf0ea774b8bab3123"
BASE = "https://raw.githubusercontent.com/risan/quran-json/" + REVISION + "/"
ROOT = Path(__file__).resolve().parent / "corpus-source"
FILES = ["LICENSE.txt", "README.md", "data/tanzil/uthmani.json", "data/tanzil/chapters.json", "data/quranenc/turkish_rwwad.json", "data/quranenc/catalogue.json", "data/meta/sources.json", "data/meta/qa.json"]

def fetch(path):
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(BASE + path, timeout=90) as response:
        target.write_bytes(response.read())
    print(path, target.stat().st_size, flush=True)

if __name__ == "__main__":
    with ThreadPoolExecutor(max_workers=4) as pool:
        list(pool.map(fetch, FILES))
