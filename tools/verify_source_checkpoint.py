#!/usr/bin/env python3
"""Verify the archived source checkpoint with Python's standard library only."""
from pathlib import Path
import hashlib
import json
import sys

root = Path(__file__).resolve().parents[1]
manifest = root / 'SOURCE-MANIFEST.json'
if not manifest.is_file():
    sys.exit('SOURCE-MANIFEST.json is missing. Run from an extracted source checkpoint.')
data = json.loads(manifest.read_text(encoding='utf-8'))
failures = []
for relative, expected in data['files'].items():
    path = root / relative
    if not path.is_file():
        failures.append('MISSING ' + relative)
    elif hashlib.sha256(path.read_bytes()).hexdigest() != expected['sha256']:
        failures.append('CHANGED ' + relative)
if failures:
    print('\n'.join(failures))
    sys.exit(1)
print('PASS: ' + str(len(data['files'])) + ' checkpoint files match SHA-256; version ' + data['version'])
