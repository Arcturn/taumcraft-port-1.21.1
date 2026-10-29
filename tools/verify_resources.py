#!/usr/bin/env python3
"""Check migration integrity and dangling resource references without starting Minecraft."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import re
import struct
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jar', type=Path)
    parser.add_argument('--sprite-ids-out', type=Path, help='Write model/MTL sprite IDs for the real Minecraft atlas smoke test')
    args = parser.parse_args()
    documents = {p.relative_to(RES).as_posix(): json.loads(p.read_text())
                 for p in RES.rglob('*') if p.suffix in ('.json', '.mcmeta')}
    models = RES / 'assets/thaumcraft/models'
    items = {p.stem for p in (models / 'item').glob('*.json')}
    blocks = {p.stem for p in (RES / 'assets/thaumcraft/blockstates').glob('*.json')}
    source = (ROOT / 'src/main/java/thaumcraft/port/content/PortContent.java').read_text()
    assert blocks <= items, 'Block without item model'
    assert len(items) >= 62 and len(blocks) >= 39

    sprite_ids = set()
    atlas_singles = {entry.get('sprite', entry['resource']) for entry in
                     documents['assets/minecraft/atlases/blocks.json']['sources']
                     if entry['type'] == 'minecraft:single'}
    def resource(identifier, folder, extension, atlas_sprite=False):
        if identifier.startswith('thaumcraft:'):
            path = RES / 'assets/thaumcraft' / folder / (identifier.split(':', 1)[1] + extension)
            assert path.is_file(), f'Missing resource: {path.relative_to(ROOT)}'
            if atlas_sprite:
                relative = identifier.split(':', 1)[1]
                assert relative.startswith(('item/', 'block/', 'items/', 'blocks/')) or identifier in atlas_singles, f'Sprite missing from vanilla directories and explicit blocks atlas sources: {identifier}'
                sprite_ids.add(identifier)

    for path in models.rglob('*.json'):
        model = json.loads(path.read_text())
        resource(model.get('parent', ''), 'models', '.json')
        for override in model.get('overrides',[]):
            resource(override['model'],'models','.json')
        if model.get('loader') in ('neoforge:obj', 'thaumcraft:crystal_cluster'):
            resource(model['model'], '', '')
        for texture in model.get('textures', {}).values():
            resource(texture, 'textures', '.png', atlas_sprite=True)
    for block in blocks:
        state = documents[f'assets/thaumcraft/blockstates/{block}.json']
        variants = list(state.get('variants', {}).values()) + [part['apply'] for part in state.get('multipart', [])]
        assert variants, block
        for variant in variants:
            for entry in variant if isinstance(variant,list) else [variant]:
                resource(entry['model'], 'models', '.json')
        loot = documents[f'data/thaumcraft/loot_table/blocks/{block}.json']
        assert loot['type'] == 'minecraft:block'
        for pool in loot['pools']:
            for entry in pool['entries']:
                if entry['type']=='minecraft:item' and entry['name'].startswith('thaumcraft:'):
                    assert entry['name'].split(':')[1] in items
    for mtl in models.rglob('*.mtl'):
        for line in mtl.read_text().splitlines():
            if line.strip().startswith(('map_Ka ', 'map_Kd ')):
                resource(line.split()[-1], 'textures', '.png', atlas_sprite=True)
    for obj in models.rglob('*.obj'):
        for line in obj.read_text().splitlines():
            if line.startswith('mtllib '):
                assert (obj.parent / line.split(maxsplit=1)[1]).is_file()

    aspect_source = (ROOT / 'src/main/java/thaumcraft/port/aspect/Aspect.java').read_text()
    aspects = set(re.findall(r'^    [A-Z]+\("([a-z]+)"', aspect_source, re.M))
    assert len(aspects) == 37
    for lang in ('en_us', 'ru_ru'):
        translations = documents[f'assets/thaumcraft/lang/{lang}.json']
        for name in items:
            key = f'block.thaumcraft.{name}' if name in blocks else f'item.thaumcraft.{name}'
            assert translations.get(key), f'Missing translation: {lang}:{key}'
        for aspect in aspects:
            assert translations.get('tc.aspect.' + aspect)
            resource('thaumcraft:aspects/' + aspect, 'textures', '.png')
    bindings = documents['thaumcraft-port/aspect-tags.json']
    migrated = json.loads((ROOT / 'reference/aspect-migration.json').read_text())
    assert set(bindings) == {row["target"] for row in migrated}
    for subject, amounts in bindings.items():
        assert re.fullmatch(r'(item|block|entity|tag):[a-z_]+:[a-z_0-9/]+', subject)
        assert set(amounts) <= aspects and all(type(n) is int and n > 0 for n in amounts.values())
        assert any(row['target'] == subject and row['aspects'] == amounts for row in migrated)

    world_bindings=documents['thaumcraft-port/world-aspect-tags.json']
    world_migrated=json.loads((ROOT/'reference/world-aspect-migration.json').read_text())
    for subject, amounts in world_bindings.items():
        assert set(amounts)<=aspects and all(type(n) is int and n>0 for n in amounts.values())
        assert any(row['target']==subject and row['aspects']==amounts for row in world_migrated)
    vanilla_bindings=documents['thaumcraft-port/vanilla-aspect-tags.json']
    vanilla_rows=json.loads((ROOT/'reference/vanilla-aspect-migration.json').read_text())['bindings']
    for subject,amounts in vanilla_bindings.items():
        assert set(amounts)<=aspects and all(type(n) is int and n>0 for n in amounts.values())
        assert any(row['target']==subject and row['aspects']==amounts for row in vanilla_rows)
    # Direct entity/effect textures do not belong to the blocks atlas, but must decode too.
    from PIL import Image
    pngs=list((RES/'assets/thaumcraft/textures').rglob('*.png'))
    for png in pngs:
        with Image.open(png) as im: im.verify()
    print(f'PASS: {len(pngs)} PNG files decode; {len(world_bindings)} world aspect bindings match migration records')

    player = documents['data/thaumcraft/curios/entities/player.json']
    for slot in player['slots']:
        definition = documents[f'data/thaumcraft/curios/slots/{slot}.json']
        assert definition['operation'] == 'SET' and definition['size'] > 0
        tag = documents[f'data/curios/tags/item/{slot}.json']
        assert tag['replace'] is False
        assert all(value.split(':')[1] in items for value in tag['values'])

    for path, recipe in documents.items():
        if '/recipe/' not in path:
            continue
        if recipe['type'] == 'thaumcraft:salis_mundus':
            assert 'SERIALIZERS.register("salis_mundus"' in source
            continue
        if recipe['type'] == 'thaumcraft:label_aspect':
            assert 'label_aspect' in (ROOT/'src/main/java/thaumcraft/port/essentia/EssentiaContent.java').read_text()
            continue
        if recipe['result']['id'].startswith('thaumcraft:'):
            assert recipe['result']['id'].split(':')[1] in items
        ingredients = recipe.get('ingredients', recipe.get('key', {}).values())
        def check_ingredient(value):
            if isinstance(value,list):return all(check_ingredient(v) for v in value)
            return not value.get('item','').startswith('thaumcraft:') or value['item'].split(':')[1] in items
        assert all(check_ingredient(value) for value in ingredients),path

    provenance = json.loads((ROOT / 'reference/provenance.json').read_text())
    for path, entry in provenance['assets'].items():
        assert hashlib.sha256((RES / path).read_bytes()).hexdigest() == entry['sha256'], path
    world_provenance = json.loads((ROOT / 'reference/worldgen-provenance.json').read_text())
    for path, entry in world_provenance['assets'].items():
        assert hashlib.sha256((RES / path).read_bytes()).hexdigest() == entry['sha256'], path
    assert world_provenance['fixed_mound_placements']==2441
    assert gzip.decompress((RES / 'data/thaumcraft/structure/empty.nbt').read_bytes())[0] == 10
    assert documents['pack.mcmeta']['pack']['pack_format'] == 34
    print(f'PASS: {len(documents)} JSON resources, {len(items)} item models, {len(blocks)} blocks, 37 aspects, {len(bindings)} migrated bindings')
    print(f'PASS: {len(provenance["assets"])} original resource hashes, Curios tags, recipes, locales and model references')
    print(f'PASS: {len(sprite_ids)} model/MTL sprites use vanilla directories or explicit atlas sources')
    if args.sprite_ids_out:
        args.sprite_ids_out.write_text('\n'.join(sorted(sprite_ids)) + '\n')

    if args.jar:
        with zipfile.ZipFile(args.jar) as jar:
            assert jar.testzip() is None
            classes = [name for name in jar.namelist() if name.endswith('.class')]
            assert classes
            for name in classes:
                assert name.startswith('thaumcraft/port/'), f'Unexpected bundled class: {name}'
                assert struct.unpack('>H', jar.read(name)[6:8])[0] == 65, name
            for path in RES.rglob('*'):
                if path.is_file() and path.name != 'neoforge.mods.toml':
                    name = path.relative_to(RES).as_posix()
                    if name == 'META-INF/jarjar/metadata.json':
                        # Gradle owns formatting of this generated JSON; its values must match.
                        assert json.loads(jar.read(name)) == json.loads(path.read_bytes()), name
                    else:
                        assert jar.read(name) == path.read_bytes(), name
            metadata = jar.read('META-INF/neoforge.mods.toml').decode()
            assert '${' not in metadata
            parsed = tomllib.loads(metadata)
            assert parsed['mods'][0]['modId'] == 'thaumcraft'
            assert {d['modId'] for d in parsed['dependencies']['thaumcraft']} == {'minecraft', 'neoforge', 'curios', 'terrablender'}
            nested=json.loads(jar.read('META-INF/jarjar/metadata.json'))['jars']
            assert len(nested)==1 and nested[0]['identifier']['artifact']=='TerraBlender-neoforge'
            assert jar.read(nested[0]['path']) == (ROOT/'libs'/Path(nested[0]['path']).name).read_bytes()
            print(f'PASS: JAR integrity, {len(classes)} Java 21 classes, expanded metadata and current resources')

if __name__ == '__main__':
    main()
