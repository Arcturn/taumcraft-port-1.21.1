"""Original TC6 research art, registered as independent display items on 1.21.1."""
import hashlib
import json
from pathlib import Path
from PIL import Image


def build(root, entries, categories):
    res = root / 'src/main/resources'
    assets = res / 'assets/thaumcraft'
    models = json.loads((res / 'thaumcraft-port/book-model-index.json').read_text())
    definitions, image_hashes, singles = {}, {}, set()
    colours = dict(air=0xffff7e, frost=0xe1ffff, earth=5685248, flux=0x800080,
                   heal=14548997, curse=6946821, **{'break': 9063176}, exchange=5735255,
                   plan=8760728, rift=3084645, projectile=11382149, bolt=11377029,
                   mine=8760709, spellbat=8760748, cloud=10071429)
    crystal_colours = dict(aer=0xffff7e, ignis=0xff5a01, aqua=0x3cd4fc,
                           terra=0x56c000, ordo=0xd5d4ec, perditio=0x404040)
    robe_items = {'thaumcraft:cloth_chest', 'thaumcraft:cloth_legs', 'thaumcraft:cloth_boots',
                  'thaumcraft:void_robe_chest', 'thaumcraft:void_robe_helm', 'thaumcraft:void_robe_legs'}

    def write(path, data):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')

    def sprite(source):
        path = assets / source.split(':', 1)[1]
        image_hashes[source] = hashlib.sha256(path.read_bytes()).hexdigest()
        with Image.open(path) as image:
            width, height = image.size
        value = source.replace(':textures/', ':').removesuffix('.png')
        if height > width and height % width == 0:
            # GuiResearchBrowser cycles raw texture strips every 150ms, ignoring
            # the item's .mcmeta. Keep original PNG bytes, with a separate atlas alias.
            alias = 'research/display_icons/' + path.stem
            dest = assets / 'textures' / (alias + '.png')
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(path.read_bytes())
            write(dest.with_suffix('.png.mcmeta'), {'animation': {'frametime': 3, 'width': width, 'height': width}})
            value = 'thaumcraft:' + alias
        elif width != height:
            raise ValueError('Review nonsquare original icon: ' + source)
        singles.add(value)
        return value

    def plane(size, depth, texture, tint=-1):
        low, high = round(8-size/2, 6), round(8+size/2, 6)
        def face(uv):
            out = {'uv': uv, 'texture': '#' + texture}
            if tint >= 0: out['tintindex'] = tint
            return out
        return {'from': [low, low, depth], 'to': [high, high, depth], 'shade': False,
                'faces': {'south': face([0, 0, 16, 16]), 'north': face([16, 0, 0, 16])}}

    def original(source):
        if ':textures/' in source:
            return {'parent': 'minecraft:item/generated', 'textures': {'layer0': sprite(source)}}, None
        if source.startswith('focus:'):
            name = source.rsplit('.', 1)[-1].lower().replace('splittarget', 'split_target').replace('splittrajectory', 'split_trajectory')
            modifier = name.startswith('split') or name in {'root', 'scatter'}
            medium = name in {'touch', 'bolt', 'projectile', 'cloud', 'mine', 'plan', 'spellbat'}
            foreground = sprite('thaumcraft:textures/foci/' + name + '.png')
            if modifier:
                return {'gui_light': 'front', 'textures': {'particle': foreground, 'symbol': foreground},
                        'elements': [plane(24, 8.5, 'symbol')]}, None
            background = sprite('thaumcraft:textures/foci/_' + ('medium' if medium else 'effect') + '.png')
            # GuiFocalManipulator.drawPart(scale=24): background 24*.9, symbol 24/2.
            return {'gui_light': 'front', 'textures': {'particle': foreground, 'background': background, 'symbol': foreground},
                    'elements': [plane(21.6, 8, 'background', 0), plane(12, 8.5, 'symbol')]}, colours[name]
        parts = source.split(';')
        base, meta = parts[0], int(parts[2]) if len(parts) > 2 else 0
        if base.startswith('minecraft:'):
            assert meta == 0, source
            base = {'minecraft:hardened_clay': 'minecraft:terracotta'}.get(base, base)
            return {'parent': base.replace(':', ':item/')}, None
        key = base.removeprefix('thaumcraft:') + '_' + str(meta)
        if base == 'thaumcraft:seal': key = 'seal_blank_0'
        # Prefer the original item/block GUI model, not the current port's item
        # renderer (e.g. TC6 research crystals use the flat crystal_planter sprite).
        assert key in models, source
        tint = crystal_colours.get(base.removeprefix('thaumcraft:crystal_')) if base.startswith('thaumcraft:crystal_') else None
        # ColorHandler applies ItemRobeArmor/ItemVoidRobeArmor.getColor to layer0.
        # Research definitions use undyed stacks; their original default is purple.
        if base in robe_items:
            tint = 6961280
        return {'parent': models[key]}, tint

    def add(path, title, sources):
        identifier = 'research_display_' + path.replace('/', '_')
        variants, overrides = [], []
        first = None
        for number, source in enumerate(sources):
            model, tint = original(source)
            if number == 0:
                model_id = 'thaumcraft:item/' + identifier
                first = model
            else:
                model_id = 'thaumcraft:research/display/' + identifier + '_' + str(number)
                write(assets / 'models/research/display' / (identifier + '_' + str(number) + '.json'), model)
                overrides.append({'predicate': {'thaumcraft:research_frame': number}, 'model': model_id})
            variant = {'source': source, 'model': model_id}
            if tint is not None: variant['tint'] = tint
            variants.append(variant)
        if overrides: first['overrides'] = overrides
        write(assets / 'models/item' / (identifier + '.json'), first)
        definitions[identifier] = {'advancement': 'thaumcraft:' + path, 'title': title, 'variants': variants}
        advancement_path = res / 'data/thaumcraft/advancement' / (path + '.json')
        advancement = json.loads(advancement_path.read_text())
        advancement['display']['icon'] = {'id': 'thaumcraft:' + identifier}
        write(advancement_path, advancement)

    add('root', 'item.thaumcraft.thaumonomicon', ['thaumcraft:textures/items/thaumonomicon.png'])
    for category in categories:
        source = 'thaumcraft:textures/items/thaumonomicon_cheat.png' if category == 'BASICS' else 'thaumcraft:textures/research/cat_' + category.lower() + '.png'
        add('research/category/' + category.lower(), 'tc.research_category.' + category, [source])
    for key, entry in entries.items():
        path = 'research/' + ('scan/' + key[1:].lower() if key.startswith('!') else key.lower())
        add(path, entry['name'], entry['icons'])
    assert len(definitions) == 156
    write(res / 'thaumcraft-port/research-display-icons.json', definitions)
    for lang in ['en_us', 'ru_ru']:
        path = assets / 'lang' / (lang + '.json')
        data = json.loads(path.read_text())
        for key, definition in definitions.items():
            title = definition['title']
            assert title in data, title
            data['item.thaumcraft.' + key] = data[title]
        write(path, data)
    atlas_path = res / 'assets/minecraft/atlases/blocks.json'
    atlas = json.loads(atlas_path.read_text())
    known = {s.get('resource') for s in atlas['sources']}
    for value in sorted(singles-known):
        atlas['sources'].append({'type': 'minecraft:single', 'resource': value, 'sprite': value})
    write(atlas_path, atlas)
    write(root / 'reference/original-research-icons.json', {
        'source': 'Thaumcraft 6.1.BETA26 GuiResearchBrowser.drawResearchIcon / GuiFocalManipulator.drawPart / ColorHandler',
        'display_items': len(definitions), 'research_entries': len(entries),
        'research_icon_variants': sum(len(e['icons']) for e in entries.values()),
        'cycling_entries': [key for key, e in entries.items() if len(e['icons']) > 1],
        'original_bitmap_sha256': image_hashes,
        'focus_geometry': {'background': 21.6, 'symbol': 12, 'modifier': 24},
        'limits': ['Minecraft advancement tree is a new integration; TC6 has no native advancements.',
                   'Pixel-level parity requires a client comparison. Imported OBJ models require NeoForge.',
                   'Atlas strip cadence is 3 game ticks; research browser raw strips use wall-clock 150ms.']})
    return definitions
