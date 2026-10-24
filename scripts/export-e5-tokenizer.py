#!/usr/bin/env python3
"""Export official E5 XLM-R Unigram assets and Hugging Face golden cases.

Offline usage with the already downloaded tokenizer.json:
  ../semantic-venv/bin/python scripts/export-e5-tokenizer.py
No case folding, alternate Unicode normalization or vocabulary edits are made.
"""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import struct

PROJECT = Path(__file__).resolve().parent.parent


def export(source):
    raw = source.read_bytes()
    spec = json.loads(raw)
    normalizers = spec["normalizer"]["normalizers"]
    assert normalizers[0]["type"] == "Precompiled"
    assert normalizers[1] == {"type": "Replace", "pattern": {"Regex": " {2,}"}, "content": " "}
    assert spec["pre_tokenizer"] == {"type": "Metaspace", "replacement": "▁", "add_prefix_space": True}
    assert spec["model"]["type"] == "Unigram" and spec["model"]["unk_id"] == 3
    vocab = spec["model"]["vocab"]
    assert len(vocab) == 250002
    assert [vocab[i][0] for i in [0, 1, 2, 3, 250001]] == ["<s>", "<pad>", "</s>", "<unk>", "<mask>"]
    normalized = base64.b64decode(normalizers[0]["precompiled_charsmap"])
    trie_size = struct.unpack_from("<I", normalized)[0]
    assert trie_size > 0 and trie_size % 4 == 0 and trie_size + 4 < len(normalized)
    data = bytearray(b"XLU1")
    data.extend(struct.pack(">I", len(vocab)))
    for piece, score in vocab:
        text = piece.encode("utf-8")
        data.extend(struct.pack(">I", len(text)))
        data.extend(text)
        data.extend(struct.pack(">d", score))
    assets = PROJECT / "app/src/main/assets/quran_search"
    assets.mkdir(parents=True, exist_ok=True)
    (assets / "unigram-vocab.bin").write_bytes(data)
    (assets / "unigram-normalizer.bin").write_bytes(normalized)
    manifest = {
        "model": "intfloat/multilingual-e5-small", "source": "https://huggingface.co/intfloat/multilingual-e5-small",
        "sourceTokenizerSha256": hashlib.sha256(raw).hexdigest(), "vocabularyCount": len(vocab),
        "maxPieceCodePoints": max(len(piece) for piece, _ in vocab),
        "format": "XLU1; big-endian uint32 count; repeated uint32 UTF8 byte length, UTF8 piece, Float64 score; IDs implicit by order",
        "normalizerFormat": "Unchanged SentencePiece precompiled charsmap: little-endian uint32 Darts byte length; uint32 Darts units; null-terminated UTF8 replacement table",
        "files": {name: {"bytes": (assets / name).stat().st_size,
                         "sha256": hashlib.sha256((assets / name).read_bytes()).hexdigest()}
                  for name in ["unigram-vocab.bin", "unigram-normalizer.bin"]},
    }
    (assets / "UNIGRAM-SOURCE.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    from tokenizers import Tokenizer
    tokenizer = Tokenizer.from_file(str(source))
    texts = [
        "", " ", "  ", "  a\t\nb  ", "query: İnsanların arkasından konuşmak neden yanlış?",
        "passage: Birbirinizin gıybetini yapmayın.", "query: İçim daralıyor, geleceğimden endişe duyuyorum.",
        "query: Allah’a güvenirim — ümitsiz değilim!", "İ ı I i cafe\u0301 Café", "Allahـ sabır",
        "a\x00b\u200bc\ue000d", "ＡＢＣ ۱۲۳ ① ﬃ", "e\u0301 a\u0308 İ\u0323", "क्\u200dष",
        "abc<s>def<mask>ghi", "<s><pad></s><unk><mask>", "a <mask> b", "<mask> ",
        "汉字𠀀", "a▁b", "q\n  r", "a\u0378\u0379b", "\U00020000\U00020001",
        "🙂🙃", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا Çin 汉字", "query: Sabır ve şükür",
        "a\u2028b a\u2029b a\u0085b", "x\u00ady\u2060z", "query: " + "kelime " * 140,
        "passage: Geçmişte yaptığım hatalar için nasıl bağışlanabilirim?",
        "query: Başkasının malına haksız yere el koymak", "query: Hastalık karşısında umut ve dayanma gücü",
        # HF's precompiled map transforms short grapheme clusters by the first
        # matching prefix; clusters of six or more UTF-8 bytes map charwise.
        "query: a\u0308\u06d9", "query: A\u0308\u064f\u0654", "query: e\u0301\u0652",
        "query: \u200d\u064f", "query: \u0600A\u0308", "query: İ\u0323 ı I i",
        "passage: بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "query: İÇİM DARALIYOR sabır SABIR Sabır",
    ]
    cases = [{"text": text, "maxLength": 128, "ids": tokenizer.encode(text).ids[:127] + [2]
              if len(tokenizer.encode(text).ids) > 128 else tokenizer.encode(text).ids} for text in texts]
    for limit in [2, 3, 5, 8]:
        tokenizer.enable_truncation(max_length=limit)
        cases.append({"text": "query: İnsanların arkasından konuşmak neden yanlış?", "maxLength": limit,
                      "ids": tokenizer.encode("query: İnsanların arkasından konuşmak neden yanlış?").ids})
    tests = PROJECT / "app/src/test/resources/quran_search"
    tests.mkdir(parents=True, exist_ok=True)
    (tests / "e5_tokenizer_golden.json").write_text(json.dumps(cases, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(json.dumps({"vocabularyBytes": len(data), "normalizerBytes": len(normalized), "goldenCases": len(cases), "sourceSha256": manifest["sourceTokenizerSha256"]}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=PROJECT.parent / "e5-source/tokenizer.json")
    export(parser.parse_args().source)
