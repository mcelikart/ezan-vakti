#!/usr/bin/env python3
"""Build the bundled Quran search corpus from pinned, locally archived snapshots.

No network access is used by this script. To restore the raw snapshots, run
scripts/download-quran-corpus.py, which fetches only the pinned public revision.
The source text, translations and footnotes are preserved exactly as decoded.
"""
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import shutil

PROJECT = Path(__file__).resolve().parent.parent
SOURCE = PROJECT / "work/corpus-source"
OUT = PROJECT / "app/src/main/assets/quran_search"
REVISION = "6523a9ad3b73bb7e8cb3a9cdf0ea774b8bab3123"
BASE_URL = "https://github.com/risan/quran-json/blob/" + REVISION + "/"
FILES = [
    "LICENSE.txt", "README.md", "data/tanzil/uthmani.json", "data/tanzil/chapters.json",
    "data/quranenc/turkish_rwwad.json", "data/quranenc/catalogue.json",
    "data/meta/sources.json", "data/meta/qa.json",
]


def read_json(relative):
    return json.loads((SOURCE / relative).read_text(encoding="utf-8"))


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def compact_json(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")) + "\n"


def build():
    assert read_json("risan-main-commit.json")["sha"] == REVISION, "Unexpected source revision"
    source_records = {item["path"]: item for item in read_json("data/meta/sources.json")}
    for relative in ["data/tanzil/uthmani.json", "data/tanzil/chapters.json", "data/quranenc/turkish_rwwad.json"]:
        assert digest(SOURCE / relative) == source_records[relative]["sha256"], relative + " hash mismatch"
        assert source_records[relative]["license_status"] == "granted", "Reuse not explicitly granted"

    # The application's existing metadata is an independent structural check and
    # supplies the established Turkish display names; it never supplies verse text.
    metadata_path = PROJECT / "app/src/main/java/com/example/ui/QuranData.kt"
    metadata = [(int(sid), name, int(count)) for sid, name, count in re.findall(
        r'SurahMeta\((\d+),\s*"([^"\n]+)",\s*\d+,\s*"[^"\n]+",\s*(\d+)\)',
        metadata_path.read_text(encoding="utf-8"),
    )]
    assert [sid for sid, _, _ in metadata] == list(range(1, 115)), "Invalid app surah metadata"
    assert sum(count for _, _, count in metadata) == 6236
    chapters = read_json("data/tanzil/chapters.json")["chapters"]
    assert [(c["id"], c["total_verses"]) for c in chapters] == [(sid, count) for sid, _, count in metadata]
    arabic = read_json("data/tanzil/uthmani.json")
    turkish = read_json("data/quranenc/turkish_rwwad.json")
    assert set(arabic) == set(turkish) == {str(sid) for sid in range(1, 115)}
    edition = next(t for t in read_json("data/quranenc/catalogue.json")["translations"] if t["key"] == "turkish_rwwad")
    assert edition["version"] == "1.0.4"

    verses, footnotes = [], {}
    for sid, name, count in metadata:
        ar_rows, tr_rows = arabic[str(sid)], turkish[str(sid)]
        assert len(ar_rows) == len(tr_rows) == count, f"Wrong verse count in surah {sid}"
        for number, (ar, tr) in enumerate(zip(ar_rows, tr_rows), 1):
            assert (ar["chapter"], ar["verse"]) == (tr["chapter"], tr["verse"]) == (sid, number)
            assert isinstance(ar["text"], str) and ar["text"].strip(), f"Empty Arabic {sid}:{number}"
            assert isinstance(tr["text"], str) and tr["text"].strip(), f"Empty Turkish {sid}:{number}"
            verses.append({"surahId": sid, "surahName": name + " Suresi", "verseNumber": number,
                           "arabic": ar["text"], "translation": tr["text"]})
            if tr.get("footnotes"):
                assert isinstance(tr["footnotes"], str)
                footnotes[f"{sid}:{number}"] = tr["footnotes"]
    assert len(verses) == len({(v["surahId"], v["verseNumber"]) for v in verses}) == 6236
    assert Counter(v["surahId"] for v in verses) == {sid: count for sid, _, count in metadata}
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "verses.json").write_text(compact_json(verses), encoding="utf-8")
    (OUT / "footnotes.json").write_text(compact_json(footnotes), encoding="utf-8")

    # Check a round-trip of every string; no trimming, diacritic conversion,
    # bismillah removal, HTML stripping, ellipses or generated text is permitted.
    bundled = json.loads((OUT / "verses.json").read_text(encoding="utf-8"))
    bundled_notes = json.loads((OUT / "footnotes.json").read_text(encoding="utf-8"))
    for v in bundled:
        sid, number = v["surahId"], v["verseNumber"]
        assert v["arabic"] == arabic[str(sid)][number - 1]["text"]
        assert v["translation"] == turkish[str(sid)][number - 1]["text"]
        original_note = turkish[str(sid)][number - 1].get("footnotes", "")
        assert bundled_notes.get(f"{sid}:{number}", "") == original_note

    manifest = {
        "repository": "https://github.com/risan/quran-json", "revision": REVISION,
        "arabicEdition": "Tanzil Uthmani 1.1", "translationEdition": "QuranEnc Turkish Rowwad 1.0.4",
        "verseCount": len(verses), "surahCount": 114, "footnoteCount": len(footnotes),
        "sources": [{"path": relative, "url": BASE_URL + relative, "sha256": digest(SOURCE / relative)} for relative in FILES],
        "outputs": {relative: digest(OUT / relative) for relative in ["verses.json", "footnotes.json"]},
    }
    (SOURCE / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (OUT / "SOURCE.json").write_text(compact_json(manifest), encoding="utf-8")
    shutil.copyfile(SOURCE / "LICENSE.txt", OUT / "LICENSE-CC-BY-SA-4.0.txt")
    notice = f"""OFFLINE QURAN SEARCH CORPUS — SOURCES AND TERMS

Coverage: 114 surahs, 6,236 ayahs, canonical Hafs/Kufan numbering.
Arabic, Turkish translation and {len(footnotes)} footnote entries are preserved verbatim.
verses.json holds the unchanged Arabic and Turkish text. footnotes.json holds
unchanged publisher footnotes keyed by surah:ayah; retain and display these notes
with their referenced translations. Surah display names come from this app's
QuranData metadata. Container keys/serialization changed; text strings did not.
The source convention includes the opening bismillah inside the first ayah text
of surahs other than 1 and 9; it is preserved rather than silently removed.

Dataset packaging: Quran JSON by Risan.
https://github.com/risan/quran-json
Pinned revision: {REVISION}
https://github.com/risan/quran-json/tree/{REVISION}
Packaging/adaptation license: Creative Commons Attribution-ShareAlike 4.0.
https://creativecommons.org/licenses/by-sa/4.0/
Full packaging license: LICENSE-CC-BY-SA-4.0.txt.
Underlying texts retain the source-specific conditions below. This notice does
not change the application's code license or grant new rights in source texts.
The preparation only joined/rekeyed the source records and added display names.
SOURCE.json records pinned source URLs and SHA-256 hashes.
verses.json SHA-256: {manifest['outputs']['verses.json']}
footnotes.json SHA-256: {manifest['outputs']['footnotes.json']}

ARABIC — TANZIL PROJECT, UTHMANI VERSION 1.1
Source: https://tanzil.net/
Download: https://tanzil.net/docs/download
Terms: https://tanzil.net/docs/text_license
Pinned snapshot: {BASE_URL}data/tanzil/uthmani.json

Tanzil Quran Text
Copyright (C) 2007-2021 Tanzil Project
License: Creative Commons Attribution 3.0
https://creativecommons.org/licenses/by/3.0/

This copy of the Quran text is carefully produced, highly
verified and continuously monitored by a group of specialists
in Tanzil Project.

TERMS OF USE:

- Permission is granted to copy and distribute verbatim copies
  of this text, but CHANGING IT IS NOT ALLOWED.

- This Quran text can be used in any website or application,
  provided that its source (Tanzil Project) is clearly indicated,
  and a link is made to tanzil.net to enable users to keep
  track of changes.

- This copyright notice shall be included in all verbatim copies
  of the text, and shall be reproduced appropriately in all files
  derived from or containing substantial portion of this text.

Please check updates at: http://tanzil.net/updates/

TURKISH — ROWWAD TRANSLATION CENTER / QURANENC.COM, VERSION 1.0.4
Title: {edition['title']}
Publisher description: {edition['description']}
Edition: https://quranenc.com/en/browse/turkish_rwwad/
Download: https://quranenc.com/downloads/sqlite/turkish_rwwad.zip
Terms: https://quranenc.com/en/home/api
Pinned snapshot: {BASE_URL}data/quranenc/turkish_rwwad.json

Contents of the translations can be downloaded and re-published, with the
following terms and conditions:
1. No modification, addition, or deletion of the content.
2. Clearly referring to the publisher and the source (QuranEnc.com).
3. Mentioning the version number when re-publishing the translation.
4. Keeping the transcript information inside the document.
5. Notifying the source (QuranEnc.com) of any note on the translation.
6. Updating the translation according to the latest version issued from the
   source (QuranEnc.com).
7. Inappropriate advertisements must not be included when displaying
   translations of the meanings of the Noble Quran.

This is a local retrieval corpus. Source texts and commentary are never
fabricated by the search engine. The shipped source notices and edition/version
must remain available to users when the content is displayed or redistributed.
"""
    (OUT / "NOTICE.txt").write_text(notice, encoding="utf-8")
    print(json.dumps({"surahs": 114, "verses": len(verses), "footnotes": len(footnotes),
                      "bytes": (OUT / "verses.json").stat().st_size, "sha256": manifest["outputs"]["verses.json"]}))


if __name__ == "__main__":
    build()
