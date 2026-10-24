#!/usr/bin/env python3
"""Select small JVM parity fixtures from the independent Python ONNX report."""
import argparse
import json
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("report", type=Path)
parser.add_argument("--output", type=Path, default=Path("app/src/test/resources/quran_search/e5_semantic_golden.json"))
args = parser.parse_args()
queries = [
    "İnsanların arkamdan konuşması ve kusurlarımı araştırması",
    "Borç verdiğim kişi ödeme yapamıyor, ona nasıl davranmalıyım?",
    "Anne ve babam yaşlanınca onlara karşı sorumluluklarım",
    "Öfkeliyken kendimi tutmak ve birini bağışlamak",
    "Aldığım bir haberin doğru olup olmadığını araştırmak",
]
report = {case["query"]: case for case in json.loads(args.report.read_text())}
fixtures = []
for query in queries:
    case = report[query]
    assert len(case["queryEmbedding"]) == 384, query
    fixtures.append({key: case[key] for key in ("query", "tokenIds", "queryEmbedding")})
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(fixtures, ensure_ascii=False, indent=2) + "\n")
print(f"Wrote {len(fixtures)} independent 384-dimensional query fixtures to {args.output}")
