#!/usr/bin/env python3
"""Check original research sources, sprite bytes, animations and GUI geometry.
This is data/resource parity; it does not claim an in-game render comparison.
"""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--legacy-jar', type=Path, required=True)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    assert hashlib.sha256(args.legacy_jar.read_bytes()).hexdigest() == '9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f'
    definitions = json.loads((RES / 'thaumcraft-port/research-display-icons.json').read_text())
    original, entries, bitmap_hashes, visited, animated = {}, {}, {}, set(), set()
    focus_count = 0
    with zipfile.ZipFile(args.legacy_jar) as archive:
        for path in (RES / 'assets/thaumcraft/research').glob('*.json'):
            relative = path.relative_to(RES).as_posix()
            for entry in json.loads(archive.read(relative))['entries']: original[entry['key']] = entry
            for entry in json.loads(path.read_text())['entries']: entries[entry['key']] = entry
        assert set(original) == set(entries) and len(entries) == 148
        for key, entry in entries.items():
            for field in ['name', 'icons', 'parents', 'siblings', 'meta', 'location']:
                assert entry.get(field) == original[key].get(field), (key, field)
            suffix = ('scan_' + key[1:] if key.startswith('!') else key).lower()
            item = definitions['research_display_research_' + suffix]
            assert [v['source'] for v in item['variants']] == original[key]['icons'], key

        def texture(identifier):
            if not identifier.startswith('thaumcraft:'): return
            name = identifier.split(':')[1]
            dest = 'assets/thaumcraft/textures/' + name + '.png'
            source = dest
            if name.startswith('research/display_icons/'):
                source = 'assets/thaumcraft/textures/items/seals/' + Path(name).name + '.png'
                meta = json.loads((RES / (dest + '.mcmeta')).read_text())
                assert meta == {'animation': {'frametime': 3, 'width': 16, 'height': 16}}
                animated.add(dest)
            raw = (RES / dest).read_bytes()
            assert raw == archive.read(source), ('Changed original icon bitmap', dest, source)
            bitmap_hashes[dest] = hashlib.sha256(raw).hexdigest()

        def model(identifier):
            if not identifier.startswith('thaumcraft:') or identifier in visited: return
            visited.add(identifier)
            path = RES / 'assets/thaumcraft/models' / (identifier.split(':')[1] + '.json')
            data = json.loads(path.read_text())
            model(data.get('parent', ''))
            for value in data.get('textures', {}).values(): texture(value)
            for override in data.get('overrides', []): model(override['model'])
            if data.get('loader') == 'neoforge:obj':
                obj = RES / 'assets/thaumcraft' / data['model'].split(':')[1]
                assert obj.is_file() and any(line.startswith('f ') for line in obj.read_text().splitlines())
                for line in obj.read_text().splitlines():
                    if line.startswith('mtllib '):
                        for material in (obj.parent / line.split()[1]).read_text().splitlines():
                            if material.startswith(('map_Ka ', 'map_Kd ')): texture(material.split()[-1])

        for key, definition in definitions.items():
            path = definition['advancement'].split(':')[1]
            advancement = json.loads((RES / 'data/thaumcraft/advancement' / (path + '.json')).read_text())
            assert advancement['display']['icon'] == {'id': 'thaumcraft:' + key}, key
            primary = json.loads((RES / 'assets/thaumcraft/models/item' / (key + '.json')).read_text())
            expected_overrides = [{'predicate': {'thaumcraft:research_frame': n}, 'model': v['model']}
                                  for n, v in enumerate(definition['variants']) if n > 0]
            assert primary.get('overrides', []) == expected_overrides, key
            for variant in definition['variants']:
                model(variant['model'])
                if variant['source'].split(';')[0] in {
                    'thaumcraft:cloth_chest', 'thaumcraft:cloth_legs', 'thaumcraft:cloth_boots',
                    'thaumcraft:void_robe_chest', 'thaumcraft:void_robe_helm', 'thaumcraft:void_robe_legs'
                }:
                    assert variant.get('tint') == 6961280, ('Missing original robe dye', variant)
                if variant['source'].startswith('focus:'):
                    data = json.loads((RES / ('assets/thaumcraft/models/' + variant['model'].split(':')[1] + '.json')).read_text())
                    elements = data['elements']
                    if 'tint' in variant:
                        assert len(elements) == 2
                        assert elements[0]['from'] == [-2.8, -2.8, 8] and elements[0]['to'] == [18.8, 18.8, 8]
                        assert elements[1]['from'] == [2, 2, 8.5] and elements[1]['to'] == [14, 14, 8.5]
                        assert elements[0]['faces']['south']['tintindex'] == 0 and 'tintindex' not in elements[1]['faces']['south']
                    else:
                        assert len(elements) == 1 and elements[0]['from'] == [-4, -4, 8.5] and elements[0]['to'] == [20, 20, 8.5]
                    assert all(e['shade'] is False for e in elements)
                    focus_count += 1
    assert len(definitions) == 156 and len(animated) == 11
    report = {'original_research_entries': len(entries), 'research_variants': sum(len(e['icons']) for e in original.values()),
              'display_items': len(definitions), 'total_display_variants': sum(len(d['variants']) for d in definitions.values()),
              'source_fields_match': True, 'models_traversed': len(visited), 'exact_original_bitmaps': len(bitmap_hashes),
              'bitmap_sha256': bitmap_hashes, 'focus_models_checked': focus_count, 'animated_seal_strips': len(animated),
              'client_rendered': False}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(f'PASS: {len(entries)} original research entries / {report["research_variants"]} ordered icons; {len(definitions)} display items')
    print(f'PASS: {len(bitmap_hashes)} bitmap assets byte-identical to TC6; {focus_count} focus geometries; {len(animated)} original animated seal strips')


if __name__ == '__main__': main()
