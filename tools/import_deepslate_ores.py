#!/usr/bin/env python3
"""Rebuild the modern deepslate ore data after the original TC6 import stages.

Keeps source ore loot/enchantments and aspects; only the Silk Touch/self-drop
block becomes the matching deepslate block. Textures are authored separately.
Run this final extension step after import_worldgen, import_theory_resources,
import_machines and import_book_recipes when regenerating those source datasets.
"""
from pathlib import Path
import copy
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ORES = ('amber', 'cinnabar', 'quartz')


def read(path):
    return json.loads((RES / path).read_text())


def write(path, value):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def tag(path, values):
    current = read(path) if (RES / path).exists() else {'replace': False, 'values': []}
    current['values'] = list(dict.fromkeys(current['values'] + values))
    write(path, current)


def replace_self(value, old, new):
    if isinstance(value, dict):
        return {key: replace_self(child, old, new) for key, child in value.items()}
    if isinstance(value, list):
        return [replace_self(child, old, new) for child in value]
    return new if value == old else value


def extend_crystal_support():
    # Run after the original worldgen importer: the new host-rock equivalents
    # must survive a clean regeneration, not only an in-place resource merge.
    tag('data/thaumcraft/tags/block/crystal_support.json',
        ['#minecraft:base_stone_nether', '#c:ores', 'minecraft:bedrock',
         *['thaumcraft:ore_' + ore for ore in ORES],
         *['thaumcraft:deepslate_ore_' + ore for ore in ORES]])


def main():
    source_aspects = read('thaumcraft-port/aspect-tags.json')
    aspects = {}
    records = []
    for ore in ORES:
        ordinary, deep = 'ore_' + ore, 'deepslate_ore_' + ore
        block = 'thaumcraft:' + deep
        write(f'assets/thaumcraft/models/block/{deep}.json', {
            'parent': 'minecraft:block/cube_all', 'textures': {'all': 'thaumcraft:block/' + deep}})
        write(f'assets/thaumcraft/models/item/{deep}.json', {'parent': 'thaumcraft:block/' + deep})
        write(f'assets/thaumcraft/blockstates/{deep}.json', {'variants': {'': {'model': 'thaumcraft:block/' + deep}}})
        source_loot = f'data/thaumcraft/loot_table/blocks/{ordinary}.json'
        write(f'data/thaumcraft/loot_table/blocks/{deep}.json',
              replace_self(read(source_loot), 'thaumcraft:' + ordinary, block))
        for kind in ('block', 'item'):
            tag(f'data/c/tags/{kind}/ores/{ore}.json', [block])
            tag(f'data/c/tags/{kind}/ores.json', ['#c:ores/' + ore])
            tag(f'data/c/tags/{kind}/ores_in_ground/deepslate.json', [block])
            tag(f'data/c/tags/{kind}/ores_in_ground/stone.json', ['thaumcraft:' + ordinary])
        tag('data/minecraft/tags/block/mineable/pickaxe.json', [block])
        if ore in ('amber', 'cinnabar'):
            tool = 'stone' if ore == 'amber' else 'iron'
            tag(f'data/minecraft/tags/block/needs_{tool}_tool.json', ['thaumcraft:' + ordinary, block])
        source_key = 'tag:c:ores/quartz' if ore == 'quartz' else 'item:thaumcraft:' + ordinary
        aspects['item:' + block] = copy.deepcopy(source_aspects[source_key])
        records.append({'id': block, 'source_ore': 'thaumcraft:' + ordinary,
                        'loot_source_sha256': hashlib.sha256((RES / source_loot).read_bytes()).hexdigest(),
                        'aspect_source_key': source_key, 'aspects': aspects['item:' + block]})
    write('thaumcraft-port/deepslate-aspect-tags.json', aspects)
    extend_crystal_support()
    names = {
        'en_us': {'amber': 'Amber Bearing Deepslate', 'cinnabar': 'Deepslate Cinnabar Ore', 'quartz': 'Deepslate Quartz Ore'},
        'ru_ru': {'amber': 'Янтароносный глубинный сланец', 'cinnabar': 'Глубинная киноварная руда', 'quartz': 'Глубинная кварцевая руда'}}
    for locale, translations in names.items():
        path = f'assets/thaumcraft/lang/{locale}.json'
        data = read(path)
        data.update({'block.thaumcraft.deepslate_ore_' + ore: name for ore, name in translations.items()})
        write(path, data)
    # The modern counterpart is a valid catalyst wherever the ordinary ore is used.
    changed_recipes = []
    for name, field in [('cinnabar_smelting', 'ingredient'), ('cinnabar_blasting', 'ingredient'),
                        ('crucible_metal_purification_cinnabar', 'catalyst')]:
        path = f'data/thaumcraft/recipe/{name}.json'
        data = read(path)
        data[field] = {'tag': 'c:ores/cinnabar'}
        write(path, data)
        changed_recipes.append(name)
    # Preserve the historical TC6 book entries; update only modern compatibility entries.
    catalog_path = 'thaumcraft-port/book-recipes.json'
    catalog = read(catalog_path)
    for name in changed_recipes:
        catalog['recipes']['thaumcraft:' + name] = read(f'data/thaumcraft/recipe/{name}.json')
    write(catalog_path, catalog)
    provenance = {'scope': 'Modern deepslate counterparts of existing TC6 ores; no claim these variants existed in 1.12.2.',
                  'run_after': ['import_worldgen.py', 'import_theory_resources.py', 'import_machines.py', 'import_book_recipes.py'],
                  'texture_generation': 'Separate asset step; this tool does not alter raster pixels.',
                  'ordinary_hardness': {'amber': 1.5, 'cinnabar': 2.0, 'quartz': 3.0},
                  'deepslate_hardness_factor': 1.5, 'records': records,
                  'cinnabar_recipes_accept_common_ore_tag': changed_recipes}
    (ROOT / 'reference/deepslate-ore-migration.json').write_text(json.dumps(provenance, ensure_ascii=False, indent=2) + '\n')
    print('Rebuilt three deepslate ore resource sets, compatible loot/tags/recipes/aspects and localization.')


if __name__ == '__main__':
    main()
