#!/usr/bin/env python3
"""Create an atomic source checkpoint and verify every file after a fresh extraction."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
EXCLUDED = {'build', 'dist', 'run', 'logs', '.gradle', '.git', '__pycache__'}

def digest(data):
    return hashlib.sha256(data).hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    if output.is_relative_to(ROOT):
        parser.error('Place the archive outside the source project.')
    props = dict(line.split('=', 1) for line in (ROOT / 'gradle.properties').read_text().splitlines()
                 if '=' in line and not line.startswith('#'))
    manifest_path = ROOT / 'SOURCE-MANIFEST.json'
    files = sorted(p for p in ROOT.rglob('*') if p.is_file()
                   and not set(p.relative_to(ROOT).parts).intersection(EXCLUDED)
                   and p.suffix not in {'.pyc', '.class'} and p != manifest_path)
    manifest = {
        'version': props['mod_version'], 'minecraft': props['minecraft_version'], 'protocol': 7,
        'created_at': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'scope': 'Experimental Thaumcraft port source checkpoint. Verification reports state their exact scope; not a claim of complete TC6 parity.',
        'files': {p.relative_to(ROOT).as_posix(): {'bytes': p.stat().st_size, 'sha256': digest(p.read_bytes())}
                  for p in files}}
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(output.suffix + '.part')
    with zipfile.ZipFile(temporary, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in sorted(files + [manifest_path]):
            archive.write(path, 'thaumcraft-port/' + path.relative_to(ROOT).as_posix())
    with zipfile.ZipFile(temporary) as archive:
        assert archive.testzip() is None, 'ZIP CRC failure'
        names = archive.namelist()
        assert len(names) == len(set(names)) == len(files) + 1, 'Duplicate archive names'
        for relative, expected in manifest['files'].items():
            raw = archive.read('thaumcraft-port/' + relative)
            assert len(raw) == expected['bytes'] and digest(raw) == expected['sha256'], relative
        with tempfile.TemporaryDirectory(prefix='checkpoint-restore-', dir=output.parent) as directory:
            archive.extractall(directory)
            subprocess.run(['python3', 'tools/verify_source_checkpoint.py'],
                           cwd=Path(directory) / 'thaumcraft-port', check=True)
    os.replace(temporary, output)
    print(json.dumps({'path': str(output), 'version': props['mod_version'],
                      'bytes': output.stat().st_size, 'sha256': digest(output.read_bytes()),
                      'source_files': len(files), 'zip_crc': 'pass', 'all_file_sha256': 'pass',
                      'fresh_extraction': 'pass'}, indent=2))

if __name__ == '__main__':
    main()
