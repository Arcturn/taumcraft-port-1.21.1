#!/usr/bin/env python3
"""Check the imported GUI transforms against original Forge blockstate data.

Uses the original source definitions and independent rotation matrices, including
all selected catalog transforms, rather than accepting the importer's report.
Does not claim a rendered-client comparison.
"""
import argparse
import json
import math
import re
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
MODELS = RES / 'assets/thaumcraft/models'
# Metadata order comes from ItemTCBase constructors, not JSON property order.
VARIANTS = {
    'ingot': 'thaumium,void,brass', 'plate': 'brass,iron,thaumium,void',
    'mind': 'clockwork,biothaumic',
    'nugget': 'iron,copper,tin,silver,lead,quicksilver,thaumium,void,brass,quartz,rareearth',
    'cluster': 'iron,gold,copper,tin,silver,lead,cinnabar,quartz',
    'thaumonomicon': 'normal,cheat',
    'baubles': 'amulet_mundane,ring_mundane,girdle_mundane,ring_apprentice,amulet_fancy,ring_fancy,girdle_fancy',
    'module': 'vision,aggression', 'turret': 'basic,advanced,bore',
    'curio': 'arcane,preserved,ancient,eldritch,knowledge,twisted,rites',
    'amulet_vis': 'found,crafted', 'label': 'blank,filled', 'phial': 'empty,filled',
    'chunk': 'beef,chicken,pork,fish,rabbit,mutton',
}
IDENTITY = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
OBJ_FLIPS = {}


def multiply(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def axis_matrix(axis, degrees):
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    return {'x': [[1, 0, 0], [0, c, -s], [0, s, c]],
            'y': [[c, 0, s], [0, 1, 0], [-s, 0, c]],
            'z': [[c, -s, 0], [s, c, 0], [0, 0, 1]]}[axis]


def rotation_matrix(rotation):
    if isinstance(rotation, dict): rotation = [rotation]
    if not isinstance(rotation[0], dict): rotation = [dict(zip('xyz', rotation))]
    result = IDENTITY
    for group in rotation:
        for axis, angle in group.items(): result = multiply(result, axis_matrix(axis, angle))
    return result


def chain(identifier):
    visited = set()
    while identifier.startswith('thaumcraft:'):
        assert identifier not in visited, ('Model parent cycle', identifier)
        visited.add(identifier)
        path = MODELS / (identifier.split(':', 1)[1] + '.json')
        assert path.is_file(), ('Missing imported model', identifier)
        data = json.loads(path.read_text())
        yield data
        identifier = data.get('parent', '')
    yield {'parent': identifier}


def verify_gui(key, expected, actual):
    assert actual is not None, ('Lost GUI transform', key)
    assert actual.get('translation', [0, 0, 0]) == expected['translation'], ('GUI translation', key, actual, expected)
    assert actual.get('scale', [1, 1, 1]) == expected['scale'], ('GUI scale', key, actual, expected)
    actual_matrix = rotation_matrix(actual.get('rotation', [0, 0, 0]))
    expected_matrix = rotation_matrix(expected['rotation'])
    assert all(abs(actual_matrix[i][j] - expected_matrix[i][j]) < 1e-6 for i in range(3) for j in range(3)), ('GUI rotation', key)


def verify_obj_materials(identifier):
    models = list(chain(identifier))
    textures = {}
    for model in reversed(models): textures.update(model.get('textures', {}))
    count = 0
    for model in models:
        if model.get('loader') != 'neoforge:obj': continue
        obj = RES / 'assets/thaumcraft' / model['model'].split(':', 1)[1]
        assert obj.is_file(), ('Missing OBJ', identifier)
        assert obj.name in OBJ_FLIPS, ('Unverified original OBJ UV convention', identifier)
        assert model.get('flip_v', False) == OBJ_FLIPS[obj.name], ('Wrong original OBJ V orientation', identifier)
        lines = obj.read_text().splitlines()
        used = {line.split(maxsplit=1)[1].strip() for line in lines if line.startswith('usemtl ')}
        materials = {}
        for line in lines:
            if not line.startswith('mtllib '): continue
            material = None
            mtl = obj.parent / line.split(maxsplit=1)[1].strip()
            assert mtl.is_file(), ('Missing material library', identifier)
            for value in mtl.read_text().splitlines():
                parts = value.strip().split(maxsplit=1)
                if not parts: continue
                if parts[0] == 'newmtl': material = parts[1]
                elif parts[0] == 'map_Kd': materials[material] = parts[1].strip()
        for material in used:
            assert material in materials, ('Missing diffuse material', identifier, material)
            value = materials[material]
            # Forge replaced materials by name; NeoForge only follows #texture
            # references. Merely preserving an old filesystem texture is wrong.
            if material in textures:
                assert value == '#' + material, ('Lost model texture override', identifier, material, value)
            visited = set()
            while value.startswith('#'):
                assert value not in visited and value[1:] in textures, ('Unbound material texture', identifier, value)
                visited.add(value)
                value = textures[value[1:]]
            assert re.fullmatch(r'[a-z0-9_.-]+:[a-z0-9/._-]+', value), ('Invalid texture resource location', identifier, value)
            namespace, path = value.split(':', 1)
            assert not path.endswith('.png'), ('Texture resource contains file suffix', identifier, value)
            if namespace == 'thaumcraft':
                assert (RES / f'assets/{namespace}/textures/{path}.png').is_file(), ('Missing OBJ texture', identifier, value)
            elif namespace == 'minecraft':
                assert not path.startswith(('blocks/', 'items/')), ('Unconverted legacy Minecraft texture', identifier, value)
        count += 1
    return count


def main():
    global RES, MODELS
    parser = argparse.ArgumentParser()
    parser.add_argument('--legacy-jar', type=Path, required=True)
    parser.add_argument('--resources', type=Path, default=RES)
    args = parser.parse_args()
    RES = args.resources
    MODELS = RES / 'assets/thaumcraft/models'
    index = json.loads((RES / 'thaumcraft-port/book-model-index.json').read_text())
    assert len(index) >= 287, 'An import error silently removed catalog models'
    checked, defaults, custom, obj_models = 0, 0, [], 0
    with ZipFile(args.legacy_jar) as archive:
        names = set(archive.namelist())
        # Read the source asset's explicit custom setting, with Forge 1.12
        # OBJModel.CustomData's false default. Texture padding cannot be
        # diagnosed by checking only that a sprite exists in the atlas.
        def obj_settings(value):
            if isinstance(value, dict):
                model = value.get('model', '')
                if model.endswith('.obj'):
                    filename = model.split(':')[-1].split('/')[-1]
                    flip = value.get('custom', {}).get('flip-v', False)
                    assert filename not in OBJ_FLIPS or OBJ_FLIPS[filename] == flip, ('Ambiguous source OBJ setting', filename)
                    OBJ_FLIPS[filename] = flip
                for child in value.values(): obj_settings(child)
            elif isinstance(value, list):
                for child in value: obj_settings(child)
        for name in names:
            if name.startswith('assets/thaumcraft/blockstates/') and name.endswith('.json'):
                obj_settings(json.loads(archive.read(name)))
        for key, identifier in index.items():
            list(chain(identifier))  # Every model chain must resolve, even when it has no Forge transform.
            obj_models += verify_obj_materials(identifier)
            name, meta = key.rsplit('_', 1)
            if name.startswith('nitor_'): continue  # ProxyBlock's intentional item/nitor alias.
            if name == 'metal': name = {'2': 'metal_brass', '3': 'metal_thaumium'}.get(meta, name)
            if name == 'arcane_stone': name = 'stone_arcane'
            name = name.replace('light_blue', 'lightblue').replace('light_gray', 'silver')
            if f'assets/thaumcraft/models/item/{name}.json' in names: continue
            source = f'assets/thaumcraft/blockstates/{name}.json'
            assert source in names, ('Missing original blockstate', key)
            state = json.loads(archive.read(source))
            variants = state.get('variants', {})
            original = state.get('defaults', {}).copy()
            if 'half' in variants and 'variant' in variants:
                original.update(variants['variant'].get('default', {}))
                selected = variants['half']['bottom']
            else:
                label = VARIANTS.get(name, '').split(',')
                label = label[int(meta)] if int(meta) < len(label) else ''
                if label and label in variants: selected = variants[label]
                elif variants.get('inventory'): selected = variants['inventory']
                elif variants.get('normal'): selected = variants['normal']
                else: selected = next(iter(variants.values()), {})
            if isinstance(selected, list): selected = selected[0] if selected else {}
            original.update(selected)
            transform = original.get('transform')
            if transform is None: continue
            if transform == 'forge:default-block':
                expected = {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [.625] * 3}
                defaults += 1
            elif isinstance(transform, str):
                assert transform in {'identity', 'forge:default-item', 'forge:default-tool'}, transform
                expected = {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1] * 3}
            else:
                gui = transform.get('gui', {})
                scale = gui.get('scale', 1)
                expected = {'rotation': gui.get('rotation', [0, 0, 0]),
                            'translation': [v * 16 for v in gui.get('translation', [0, 0, 0])],
                            'scale': scale if isinstance(scale, list) else [scale] * 3}
                custom.append(key)
            imported = next(chain(identifier)).get('display', {}).get('gui')
            verify_gui(key, expected, imported)
            checked += 1

    assert checked >= 113 and defaults >= 60, (checked, defaults)
    assert set(custom) == {'infernal_furnace_0', 'thaumatorium_0', 'golem_builder_0', 'thaumometer_0'}, custom
    for key, scale, translation in [('infernal_furnace_0', .275, [0, 0, 0]),
                                     ('thaumatorium_0', .65, [0, -4, 0]),
                                     ('golem_builder_0', .4, [0, -2, 0]),
                                     ('thaumometer_0', .35, [2.8, -2.8, 0])]:
        gui = next(chain(index[key]))['display']['gui']
        assert gui['scale'] == [scale] * 3 and gui['translation'] == translation, key

    # The source metadata must select different artwork, not silently fall back to variant zero.
    expected_textures = {'turret_1': 'turret_advanced', 'turret_2': 'turret_bore',
                         'module_1': 'module_aggression', 'curio_6': 'crimson_rites',
                         'amulet_vis_1': 'amulet_vis', 'label_1': 'label_overlay', 'phial_1': 'phial_overlay'}
    for key, suffix in expected_textures.items():
        textures = [value for model in chain(index[key]) for value in model.get('textures', {}).values()]
        assert 'thaumcraft:items/' + suffix in textures, (key, textures)
    for key in ['tube_0', 'tube_buffer_0', 'crystal_aer_0', 'nitor_4']:
        assert any(model.get('parent') == 'minecraft:item/generated' for model in chain(index[key])), ('Originally flat icon changed to 3D', key)

    world = json.loads((RES / 'thaumcraft-port/book-block-model-index.json').read_text())
    assert len(world) == 25, 'Incomplete source/target blueprint model catalog'
    for key, entry in world.items():
        obj_models += verify_obj_materials(entry['model'])
        models = list(chain(entry['model']))
        assert not any('display' in model or model.get('parent') == 'minecraft:item/generated' for model in models), ('World cell reused an inventory model', key)
        for model in models:
            if model.get('loader') == 'neoforge:obj':
                obj = RES / 'assets/thaumcraft' / model['model'].split(':', 1)[1]
                assert obj.is_file() and any(line.startswith('f ') for line in obj.read_text().splitlines()), ('Missing world geometry', key)
    for name in ['pillar_arcane', 'pillar_ancient', 'pillar_eldritch']:
        for meta, angle in [(0, 180), (1, 270), (2, 0), (3, 90)]:
            entry = world[f'{name}_{meta}']
            assert (entry['x'], entry['y']) == (0, angle), ('Wrong pillar facing', name, meta)
    assert (world['thaumatorium_0']['x'], world['thaumatorium_0']['y']) == (90, 90)
    assert next(chain(world['pedestal_ancient_2']['model']))['parent'].endswith('/block/pedestal_ancient')
    assert next(chain(world['pedestal_eldritch_1']['model']))['parent'].endswith('/block/pedestal_eldritch')
    with ZipFile(args.legacy_jar) as archive:
        for name, count in [('stone_arcane',4),('stone_ancient',16),('stone_eldritch_tile',4)]:
            source=json.loads(archive.read(f'assets/thaumcraft/blockstates/{name}.json'))['variants']['normal']
            source=sorted(source,key=lambda v:-v.get('weight',1))
            variants=world[name+'_0']['variants']
            assert len(variants)==count, ('Lost original stone rotations',name)
            assert [(v['x'],v['y'],v['weight']) for v in variants]==[(v.get('x',0),v.get('y',0),v.get('weight',1)) for v in source], ('Changed weighted variant order',name)
            assert all(v['model']==world[name+'_0']['model'] for v in variants), ('Unexpected geometry change in stone rotation',name)
    print(f'PASS: {len(index)} model chains; {checked} original GUI transforms ({defaults} default-block, {len(custom)} custom)')
    print('PASS: 7 metadata regressions and 4 intentionally flat item models')
    print('PASS: 25 world blueprint models, including all 12 directional pillar cells')
    print('PASS: 24 original weighted stone rotations, with stable weight/order and horizontal pillar metadata')
    print(f'PASS: {obj_models} OBJ model selections resolve material names, textures and original UV orientation')


if __name__ == '__main__': main()
