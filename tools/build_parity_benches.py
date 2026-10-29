#!/usr/bin/env python3
"""Paired manual-only test functions. Never installs or executes them in a world."""
import json
import pathlib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
TARGET = ROOT / 'verification/parity-benches'


def build():
    report = json.loads((ROOT / 'reference/content-inventory.json').read_text())
    original = {e['id'] for e in report['entries'] if e['kind'] in ('block', 'item')}
    current = set(report['runtime_snapshot']['registries']['minecraft:item'])
    blocks = ['arcane_workbench', 'crucible', 'table_wood', 'research_table', 'smelter_basic', 'smelter_thaumium',
              'smelter_void', 'alembic', 'jar_normal', 'jar_void', 'tube', 'tube_valve', 'tube_filter', 'tube_oneway',
              'tube_restrict', 'tube_buffer', 'centrifuge', 'bellows', 'thaumatorium', 'thaumatorium_top']
    blocks += ['nitor_' + s for s in 'white orange magenta lightblue yellow lime pink gray silver cyan purple blue brown green red black'.split()]
    blocks += ['crystal_' + s for s in 'aer ignis aqua terra ordo perditio vitium'.split()]
    items = ['thaumonomicon', 'thaumometer', 'scribing_tools', 'phial', 'label', 'jar_brace']
    checks = []
    for version, modern in [('mc1.12.2', False), ('mc1.21.1', True)]:
        folder = TARGET / version
        functions = folder / ('data/thaumcraft_parity/function' if modern else 'data/functions/thaumcraft_parity')
        functions.mkdir(parents=True, exist_ok=True)
        if modern:
            (folder / 'pack.mcmeta').write_text(json.dumps({'pack': {'pack_format': 48, 'description': 'Thaumcraft manual comparison bench — test worlds only'}}, indent=2) + '\n')
        setup = ['# TEST WORLD ONLY: replaces blocks in x/z -16..16, y 64..78.',
                 '# Execute as a player in the Overworld. No automatic load/tick functions.',
                 'gamemode creative @s', 'difficulty normal', 'gamerule doDaylightCycle false',
                 'gamerule doWeatherCycle false', 'time set 6000', 'weather clear',
                 'fill -16 64 -16 16 64 16 minecraft:stone',
                 'fill -16 65 -16 16 78 16 minecraft:air', 'tp @s 0.5 65 14.5 180 10']
        for i, name in enumerate(blocks):
            port_name = name.replace('nitor_lightblue', 'nitor_light_blue').replace('nitor_silver', 'nitor_light_gray')
            id = 'thaumcraft:' + (port_name if modern else name)
            assert id in (current if modern else original), id
            x, z = -12 + (i % 9) * 3, -12 + (i // 9) * 4
            y = 65
            if name == 'thaumatorium':
                setup.append(f'setblock {x} 65 {z} thaumcraft:crucible')
                y = 66
                thaumatorium_position = (x, z)
            elif name == 'thaumatorium_top':
                x, z = thaumatorium_position
                y = 67
            setup.append(f'setblock {x} {y} {z} {id}')
            checks.append({'version': version, 'id': id, 'position': [x, y, z]})
        (functions / 'setup.mcfunction').write_text('\n'.join(setup) + '\n')
        commands = []
        for name in items:
            id = 'thaumcraft:' + name
            assert id in (current if modern else original), id
            commands.append(f'give @s {id} 1')
        commands += ['give @s minecraft:paper 64']
        (functions / 'items.mcfunction').write_text('\n'.join(commands) + '\n')
        (functions / 'day.mcfunction').write_text('time set 6000\n')
        (functions / 'night.mcfunction').write_text('time set 18000\n')
        (functions / 'book.mcfunction').write_text('# Optional: permanently grants research in this TEST save. Never use for survival-progress comparison.\n' +
                ('thaumcraft research grant all\n' if modern else 'thaumcraft research @s all\n'))
        if modern:
            (functions / 'audit.mcfunction').write_text('thaumcraft audit\nthaumcraft audit research\n')
    (TARGET / 'bench-manifest.json').write_text(json.dumps({'schema': 1, 'executed_in_game': False,
        'scope': 'Static appearance and initial item setup; isolated blocks are not functional machine assemblies.',
        'placements': checks}, indent=2) + '\n')
    archive = TARGET / 'thaumcraft-parity-1.21.1.zip'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
        for path in sorted((TARGET / 'mc1.21.1').rglob('*')):
            if path.is_file(): z.write(path, path.relative_to(TARGET / 'mc1.21.1').as_posix())
    print(json.dumps({'paired_blocks': len(blocks), 'paired_items': len(items), 'executed': False}))


if __name__ == '__main__': build()
