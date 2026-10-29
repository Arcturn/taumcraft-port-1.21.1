#!/usr/bin/env python3
"""Import unmodified TC6 sound data and the original particle sheet, with provenance."""
import argparse, hashlib, json, pathlib, zipfile

p = argparse.ArgumentParser()
p.add_argument('--jar', type=pathlib.Path, required=True)
args = p.parse_args()
root = pathlib.Path(__file__).resolve().parents[1]
res = root / 'src/main/resources'
records = []
with zipfile.ZipFile(args.jar) as source:
    def copy(old, new=None):
        data = source.read(old)
        target = new or old
        path = res / target
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        records.append({'source': old, 'target': target, 'sha256': hashlib.sha256(data).hexdigest()})
    sounds = json.loads(source.read('assets/thaumcraft/sounds.json'))
    for definition in sounds.values():
        definition.pop('category', None)  # category is selected by the playback call in modern Minecraft
    # TC6 registered this spelling but shipped the JSON event with an extra 'e'. Keep both IDs.
    sounds['runicshieldcharge'] = sounds['runicshieldecharge'].copy()
    (res / 'assets/thaumcraft/sounds.json').write_text(json.dumps(sounds, indent=2) + '\n')
    for name in source.namelist():
        if name.startswith('assets/thaumcraft/sounds/') and name.endswith('.ogg'):
            copy(name)
    copy('assets/thaumcraft/textures/misc/particles.png', 'assets/thaumcraft/textures/particle/legacy.png')
    for kind in ('wisp', 'taint', 'spark', 'rune', 'craft', 'alumentum'):
        path = res / f'assets/thaumcraft/particles/{kind}.json'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({'textures': ['thaumcraft:legacy']}) + '\n')
(root / 'reference/presentation-provenance.json').write_text(json.dumps({
    'source_sha256': hashlib.sha256(args.jar.read_bytes()).hexdigest(),
    'original_sound_events': len(sounds)-1, 'registered_sound_events': len(sounds),
    'notes': ['Removed obsolete sound category; retained volumes, pitches, choices and streaming flags.',
              'runicshieldcharge aliases the shipped runicshieldecharge event.'],
    'assets': records}, indent=2) + '\n')
print(f'Imported {len(records)} unchanged assets and {len(sounds)} sound events')
