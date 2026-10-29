#!/usr/bin/env python3
"""New vanilla advancement tree derived from the complete TC6 research catalog, not legacy achievements."""
from pathlib import Path
import argparse, hashlib, json, re, zipfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/thaumcraft'

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--legacy-jar', type=Path, required=True)
    args = parser.parse_args()
    legacy_hash = hashlib.sha256(args.legacy_jar.read_bytes()).hexdigest()
    assert legacy_hash == '9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f'
    with zipfile.ZipFile(args.legacy_jar) as archive:
        legacy_advancements = [n for n in archive.namelist() if '/advancements/' in n or '/achievements/' in n]
    assert not legacy_advancements, 'Reassess the source before inventing a replacement tree'
    entries = {}
    for path in sorted((ASSETS / 'research').glob('*.json')):
        for entry in json.loads(path.read_text())['entries']:
            assert entry['key'] not in entries
            entries[entry['key']] = entry
    assert len(entries) == 148
    categories = ['BASICS', 'AUROMANCY', 'ALCHEMY', 'ARTIFICE', 'INFUSION', 'GOLEMANCY', 'ELDRITCH']
    models = json.loads((RES / 'thaumcraft-port/book-model-index.json').read_text())
    overrides, icon_index, sprites, records = [], {}, set(), []

    def icon(source):
        parts = source.split(';')
        base, meta = parts[0], int(parts[2]) if len(parts) > 2 else 0
        vanilla_aliases = {'minecraft:hardened_clay': 'minecraft:terracotta'}
        if base.startswith('minecraft:'):
            return {'id': vanilla_aliases.get(base, base)}
        if base.startswith('thaumcraft:') and meta == 0 and len(parts) < 4 and (ASSETS / 'models/item' / (base[11:] + '.json')).is_file():
            return {'id': base}
        colour = None
        if ':textures/' in source:
            sprite = source.replace(':textures/', ':').removesuffix('.png')
            model = {'parent': 'minecraft:item/generated', 'textures': {'layer0': sprite}}
            sprites.add(sprite)
        elif source.startswith('focus:'):
            name = source.rsplit('.', 1)[-1].lower().replace('splittarget', 'split_target').replace('splittrajectory', 'split_trajectory')
            medium = name in {'touch', 'bolt', 'projectile', 'cloud', 'mine', 'plan', 'spellbat'}
            modifier = name == 'root' or name.startswith('split') or name == 'scatter'
            textures = {'layer0': 'thaumcraft:foci/' + name}
            if not modifier:
                textures = {'layer0': 'thaumcraft:foci/_' + ('medium' if medium else 'effect'), 'layer1': 'thaumcraft:foci/' + name}
                colour = {'air': 0xffff7e, 'flux': 0x800080, 'heal': 14548997, 'curse': 6946821, 'break': 9063176, 'exchange': 5735255, 'plan': 8760728, 'rift': 3084645, 'projectile': 11382149, 'bolt': 11377029, 'mine': 8760709, 'spellbat': 8760748, 'cloud': 10071429}[name]
            model = {'parent': 'minecraft:item/generated', 'textures': textures}
            sprites.update(textures.values())
        else:
            key = base.removeprefix('thaumcraft:') + '_' + str(meta)
            if base == 'thaumcraft:seal': key = 'seal_blank_0'
            model = {'parent': models[key]}
        index = len(overrides) + 1
        model_id = 'thaumcraft:research/advancements/icon_' + str(index)
        write(ASSETS / 'models/research/advancements' / ('icon_' + str(index) + '.json'), model)
        overrides.append({'predicate': {'custom_model_data': index}, 'model': model_id})
        icon_index[str(index)] = {'source': source, 'model': model_id}
        if colour is not None: icon_index[str(index)]['tint'] = colour
        return {'id': 'thaumcraft:research_icon', 'components': {'minecraft:custom_model_data': index}}

    def advancement(path, title, description, display_icon, conditions, parent=None, hidden=False, toast=True, frame='task'):
        display = {'icon': display_icon, 'title': title, 'description': description, 'frame': frame,
                   'show_toast': toast, 'announce_to_chat': False, 'hidden': hidden}
        result = {'display': display, 'criteria': {'completed': {'trigger': 'thaumcraft:research', 'conditions': conditions}},
                  'requirements': [['completed']], 'sends_telemetry_event': False}
        if parent: result['parent'] = parent
        else:
            background = 'thaumcraft:textures/blocks/arcane_stone_1.png'
            assert (ASSETS / background.split(':')[1]).is_file()
            display['background'] = background
        write(RES / 'data/thaumcraft/advancement' / (path + '.json'), result)
        return result

    advancement('root', {'text': 'Thaumcraft'}, {'translate': 'thaumcraft.advancement.root.description'},
                {'id': 'thaumcraft:thaumonomicon'}, {'research': '!gotthaumonomicon'}, toast=False)
    for category in categories:
        source = 'thaumcraft:textures/items/thaumonomicon_cheat.png' if category == 'BASICS' else 'thaumcraft:textures/research/cat_' + category.lower() + '.png'
        advancement('research/category/' + category.lower(), {'translate': 'tc.research_category.' + category},
                    {'translate': 'thaumcraft.advancement.category.description', 'with': [{'translate': 'tc.research_category.' + category}]},
                    icon(source), {'category': category}, 'thaumcraft:root', toast=False)

    def research_path(key):
        return 'research/' + ('scan/' + key[1:].lower() if key.startswith('!') else key.lower())
    parents = {}
    for key, entry in entries.items():
        candidates = [value.lstrip('~').split('@')[0] for value in entry.get('parents', [])]
        candidates = [value for value in candidates if value in entries and value != key and entries[value]['category'] == entry['category']]
        parents[key] = candidates[0] if candidates else None
    # A vanilla advancement has a single display parent; requirements still use the full server research state.
    for key in entries:
        visited, cursor = set(), key
        while cursor is not None:
            if cursor in visited: parents[key] = None; break
            visited.add(cursor); cursor = parents[cursor]
    for key, entry in entries.items():
        parent = 'thaumcraft:' + research_path(parents[key]) if parents[key] else 'thaumcraft:research/category/' + entry['category'].lower()
        result = advancement(research_path(key), {'translate': entry['name']},
                    {'translate': 'thaumcraft.advancement.research.description', 'with': [{'translate': entry['name']}]},
                    icon(entry['icons'][0]), {'research': key}, parent,
                    hidden='HIDDEN' in entry.get('meta', []), toast=False,
                    frame='goal' if 'SPIKY' in entry.get('meta', []) else 'task')
        records.append({'research': key, 'advancement': 'thaumcraft:' + research_path(key), 'original_icon': entry['icons'][0], 'icon': result['display']['icon'], 'display_parent': parent})
    write(ASSETS / 'models/item/research_icon.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'thaumcraft:items/thaumonomicon'}, 'overrides': overrides})
    write(RES / 'thaumcraft-port/advancement-icon-index.json', icon_index)
    atlas_path = RES / 'assets/minecraft/atlases/blocks.json'
    atlas = json.loads(atlas_path.read_text())
    known = {s.get('resource') for s in atlas['sources']}
    for sprite in sorted(sprites):
        assert (ASSETS / 'textures' / (sprite.split(':')[1] + '.png')).is_file(), sprite
        if sprite not in known: atlas['sources'].append({'type': 'minecraft:single', 'resource': sprite, 'sprite': sprite})
    write(atlas_path, atlas)
    for lang, messages in {
        'en_us': ['Research icon', 'Open the Thaumonomicon.', 'Complete a research entry in %s.', 'Complete the research “%s”.'],
        'ru_ru': ['Значок исследования', 'Откройте Таумономикон.', 'Завершите исследование в разделе «%s».', 'Завершите исследование «%s».']
    }.items():
        path = ASSETS / 'lang' / (lang + '.json'); data = json.loads(path.read_text())
        data.update(dict(zip(['item.thaumcraft.research_icon', 'thaumcraft.advancement.root.description', 'thaumcraft.advancement.category.description', 'thaumcraft.advancement.research.description'], messages)))
        write(path, data)
    from original_research_icons import build
    display_icons = build(ROOT, entries, categories)
    display_by_advancement = {value['advancement']: 'thaumcraft:' + key for key, value in display_icons.items()}
    for record in records:
        record['icon'] = {'id': display_by_advancement[record['advancement']]}
    write(ROOT / 'reference/advancement-migration.json', {'source_sha256': legacy_hash, 'original_advancement_files': legacy_advancements,
        'kind': 'New Minecraft 1.21.1 advancement tree derived from TC6 research, not a port of nonexistent TC6 achievements.',
        'research_entries': len(entries), 'category_nodes': len(categories), 'total': len(entries) + len(categories) + 1,
        'legacy_override_models': len(overrides), 'display_items': len(display_icons), 'records': records,
        'limitations': ['Unimplemented research mechanics are not completed by displaying advancements.', 'Vanilla supports one display parent; all original prerequisites remain enforced by the research system.', 'Original icon variants cycle every second; pixel-level parity needs an in-game comparison.']})
    print(f'Built {len(entries) + len(categories) + 1} advancements; {len(display_icons)} independent display items with every original icon variant')

if __name__ == '__main__': main()
