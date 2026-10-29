#!/usr/bin/env python3
"""Reproduce the reviewed data/resource migration from the supplied TC6 jar and CFR sources.
No downloads. The original jar is never modified and its old classes are never bundled in the port.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(data, encoding='utf-8')

def js(path, data):
    write(path, json.dumps(data, ensure_ascii=False, indent=2) + '\n')

def migrate(jar, sources):
    z = zipfile.ZipFile(jar)
    provenance = {}
    def copy(source, destination=None):
        destination = destination or source
        # In 1.21.1, model sprites must be included in an atlas. The vanilla
        # blocks atlas scans singular block/ and item/, not the 1.12 plural paths.
        destination = destination.replace('/textures/items/', '/textures/item/').replace('/textures/blocks/', '/textures/block/')
        raw = z.read(source)
        target = RES / destination
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(raw)
        provenance[destination] = {'source': source, 'sha256': hashlib.sha256(raw).hexdigest()}

    # These declarations and component pairs are recovered verbatim from the uploaded bytecode.
    aspect_source = (sources / 'thaumcraft/api/aspects/Aspect.java').read_text()
    matches = re.findall(r'public static final Aspect (\w+) = new Aspect\("(\w+)", (0x[0-9A-Fa-f]+|\d+), ([^;]+)\);', aspect_source)
    assert len(matches) == 37, 'Unexpected aspect table: stop and review the source version.'
    aspect_ids = {name: tag for name, tag, _, _ in matches}
    declarations = []
    for name, tag, color, rest in matches:
        components = re.search(r'new Aspect\[\]\{([^}]+)\}', rest)
        args = ', ' + components.group(1) if components else ''
        declarations.append(f'    {name}("{tag}", {color}{args})')
        copy(f'assets/thaumcraft/textures/aspects/{tag}.png')
    java = '''package thaumcraft.port.aspect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The 37 original TC6 aspects, colors and component pairs. Original definitions by Azanor. */
public enum Aspect {
''' + ',\n'.join(declarations) + ''';

    private final String id;
    private final int color;
    private final List<Aspect> components;
    Aspect(String id, int color, Aspect... components) {
        this.id = id;
        this.color = color;
        this.components = List.of(components);
    }
    public String id() { return id; }
    /** TC6 getName(): the capitalized Latin tag, independent of the translated description. */
    public String displayName() { return Character.toUpperCase(id.charAt(0)) + id.substring(1); }
    public static String describe(Map<Aspect, Integer> amounts) {
        return amounts.entrySet().stream().map(e -> e.getKey().displayName() + " ×" + e.getValue())
                .collect(java.util.stream.Collectors.joining(", "));
    }
    public int color() { return color; }
    public List<Aspect> components() { return components; }
    public boolean isPrimal() { return components.isEmpty(); }
    public static Optional<Aspect> byId(String id) {
        return Arrays.stream(values()).filter(aspect -> aspect.id.equals(id)).findFirst();
    }
    public static Optional<Aspect> combine(Aspect first, Aspect second) {
        return Arrays.stream(values()).filter(a -> a.components.size() == 2 &&
                ((a.components.get(0) == first && a.components.get(1) == second) ||
                 (a.components.get(0) == second && a.components.get(1) == first))).findFirst();
    }
}
'''
    write(ROOT / 'src/main/java/thaumcraft/port/aspect/Aspect.java', java)

    items = ['amber', 'quicksilver', 'ingot_brass', 'ingot_thaumium', 'ingot_void',
             'nugget_brass', 'nugget_thaumium', 'nugget_void', 'nugget_quicksilver', 'nugget_quartz',
             'crystal_essence', 'salis_mundus', 'thaumonomicon', 'charm_undying',
             'amulet_mundane', 'ring_mundane', 'girdle_mundane', 'amulet_fancy', 'ring_fancy', 'girdle_fancy']
    blocks = ['metal_brass', 'metal_thaumium', 'metal_void', 'ore_amber', 'ore_cinnabar', 'ore_quartz', 'amber_block', 'amber_brick']
    for item in items:
        texture = f'assets/thaumcraft/textures/items/{item}.png'
        copy(texture)
        if texture + '.mcmeta' in z.namelist(): copy(texture + '.mcmeta')
        js(RES / f'assets/thaumcraft/models/item/{item}.json', {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': f'thaumcraft:item/{item}'}})
    for block in blocks:
        if block == 'amber_block':
            for face in ['amber_side', 'amber_top']: copy(f'assets/thaumcraft/textures/blocks/{face}.png')
            model = {'parent': 'minecraft:block/cube_column', 'textures': {'side': 'thaumcraft:block/amber_side', 'end': 'thaumcraft:block/amber_top'}}
        else:
            copy(f'assets/thaumcraft/textures/blocks/{block}.png')
            model = {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'thaumcraft:block/{block}'}}
        js(RES / f'assets/thaumcraft/models/block/{block}.json', model)
        js(RES / f'assets/thaumcraft/models/item/{block}.json', {'parent': f'thaumcraft:block/{block}'})
        js(RES / f'assets/thaumcraft/blockstates/{block}.json', {'variants': {'': {'model': f'thaumcraft:block/{block}'}}})
        # Ores remain creative-only in this milestone; loot is explicit, not invented world generation.
        js(RES / f'data/thaumcraft/loot_table/blocks/{block}.json', {
            'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'thaumcraft:{block}'}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    js(RES / 'data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [f'thaumcraft:{b}' for b in blocks]})

    # Convert the original OBJ material paths and legacy Forge blockstate transform format.
    copy('assets/thaumcraft/models/item/scanner.obj')
    mtl = z.read('assets/thaumcraft/models/item/scanner.mtl').decode().replace('items\\', 'item/').replace('items/', 'item/').replace('.png', '')
    write(RES / 'assets/thaumcraft/models/item/scanner.mtl', mtl)
    for name in ['scanner', 'scanscreen']:
        copy(f'assets/thaumcraft/textures/items/{name}.png')
        if f'assets/thaumcraft/textures/items/{name}.png.mcmeta' in z.namelist():copy(f'assets/thaumcraft/textures/items/{name}.png.mcmeta')
    js(RES / 'assets/thaumcraft/models/item/thaumometer.json', {
        'loader': 'neoforge:obj', 'model': 'thaumcraft:models/item/scanner.obj', 'flip_v': True,
        'automatic_culling': False, 'textures': {'particle': 'thaumcraft:item/scanner'},
        'display': {
            'gui': {'rotation': [90, 0, 0], 'translation': [8, 8, 0], 'scale': [.30, .30, .30]},
            'firstperson_righthand': {'rotation': [90, 0, 0], 'translation': [0, 3, 0], 'scale': [.3, .3, .3]},
            'firstperson_lefthand': {'rotation': [90, 0, 0], 'translation': [0, 3, 0], 'scale': [.3, .3, .3]},
            'thirdperson_righthand': {'rotation': [90, 90, 0], 'translation': [0, 3, 0], 'scale': [.2, .2, .2]},
            'ground': {'rotation': [90, 0, 0], 'translation': [0, 3, 0], 'scale': [.2, .2, .2]}}})

    js(RES / 'data/thaumcraft/recipe/salis_mundus.json', {'type': 'thaumcraft:salis_mundus', 'category': 'misc'})
    for metal in ['brass', 'thaumium', 'void']:
        for src, dst, count, suffix in [(f'ingot_{metal}', f'nugget_{metal}', 9, 'nuggets'), (f'metal_{metal}', f'ingot_{metal}', 9, 'unpack')]:
            js(RES / f'data/thaumcraft/recipe/{metal}_{suffix}.json', {'type': 'minecraft:crafting_shapeless',
                'category': 'misc', 'ingredients': [{'item': f'thaumcraft:{src}'}], 'result': {'id': f'thaumcraft:{dst}', 'count': count}})
        for src, dst, suffix in [(f'nugget_{metal}', f'ingot_{metal}', 'ingot'), (f'ingot_{metal}', f'metal_{metal}', 'block')]:
            js(RES / f'data/thaumcraft/recipe/{metal}_{suffix}.json', {'type': 'minecraft:crafting_shaped',
                'category': 'misc', 'pattern': ['###','###','###'], 'key': {'#': {'item': f'thaumcraft:{src}'}}, 'result': {'id': f'thaumcraft:{dst}'}})
        for kind in ['ingot','nugget']:
            js(RES / f'data/c/tags/item/{kind}s/{metal}.json', {'replace': False, 'values': [f'thaumcraft:{kind}_{metal}']})

    slot_items = {'necklace': ['amulet_mundane','amulet_fancy'], 'ring': ['ring_mundane','ring_fancy'],
                  'belt': ['girdle_mundane','girdle_fancy'], 'charm': ['charm_undying']}
    for slot, ids in slot_items.items():
        js(RES / f'data/curios/tags/item/{slot}.json', {'replace': False, 'values': [f'thaumcraft:{i}' for i in ids]})
        js(RES / f'data/thaumcraft/curios/slots/{slot}.json', {'size': 2 if slot == 'ring' else 1, 'operation': 'SET'})
    js(RES / 'data/thaumcraft/curios/entities/player.json', {'entities': ['minecraft:player'], 'slots': list(slot_items)})

    # Reuse exact aspect amounts from explicitly named legacy OreDictionary registrations.
    tag_map = {'stone': 'item:minecraft:stone', 'stoneGranite': 'item:minecraft:granite', 'stoneDiorite': 'item:minecraft:diorite',
               'stoneAndesite': 'item:minecraft:andesite', 'cobblestone': 'item:minecraft:cobblestone', 'dirt': 'item:minecraft:dirt',
               'sand': 'tag:minecraft:sand', 'grass': 'item:minecraft:grass_block', 'endstone': 'item:minecraft:end_stone',
               'gravel': 'item:minecraft:gravel', 'netherrack': 'item:minecraft:netherrack', 'blockGlass': 'item:minecraft:glass',
               'obsidian': 'item:minecraft:obsidian', 'logWood': 'tag:minecraft:logs', 'treeSapling': 'tag:minecraft:saplings',
               'treeLeaves': 'tag:minecraft:leaves', 'vine': 'item:minecraft:vine', 'cropNetherWart': 'item:minecraft:nether_wart',
               'blockCactus': 'item:minecraft:cactus', 'sugarcane': 'item:minecraft:sugar_cane', 'cropWheat': 'item:minecraft:wheat',
               'cropCarrot': 'item:minecraft:carrot', 'cropPotato': 'item:minecraft:potato', 'string': 'item:minecraft:string',
               'slimeball': 'item:minecraft:slime_ball', 'leather': 'item:minecraft:leather', 'feather': 'item:minecraft:feather',
               'bone': 'item:minecraft:bone', 'egg': 'item:minecraft:egg', 'gunpowder': 'item:minecraft:gunpowder',
               'enderpearl': 'item:minecraft:ender_pearl', 'netherStar': 'item:minecraft:nether_star',
               'quicksilver': 'item:thaumcraft:quicksilver', 'gemAmber': 'item:thaumcraft:amber',
               'oreAmber': 'item:thaumcraft:ore_amber', 'oreCinnabar': 'item:thaumcraft:ore_cinnabar',
               'dustRedstone': 'item:minecraft:redstone', 'dustGlowstone': 'item:minecraft:glowstone_dust'}
    for prefix, modern in [('ore','ores'),('ingot','ingots'),('dust','dusts'),('gem','gems')]:
        for metal in ['Iron','Gold','Copper','Tin','Silver','Lead','Brass','Bronze','Uranium','Steel','Ruby','Sapphire','Diamond','Emerald','Quartz','Lapis','Redstone']:
            tag_map.setdefault(prefix+metal, f'tag:c:{modern}/{metal.lower()}')
    entities = {'Zombie':'zombie','Husk':'husk','Giant':'giant','Skeleton':'skeleton','WitherSkeleton':'wither_skeleton',
        'Creeper':'creeper','Horse':'horse','Donkey':'donkey','Mule':'mule','SkeletonHorse':'skeleton_horse',
        'ZombieHorse':'zombie_horse','Pig':'pig','XPOrb':'experience_orb','Sheep':'sheep','Cow':'cow','MushroomCow':'mooshroom',
        'SnowMan':'snow_golem','Ozelot':'ocelot','Chicken':'chicken','Squid':'squid','Wolf':'wolf','Bat':'bat',
        'Spider':'spider','Slime':'slime','Ghast':'ghast','PigZombie':'zombified_piglin','Enderman':'enderman',
        'CaveSpider':'cave_spider','Silverfish':'silverfish','Blaze':'blaze','LavaSlime':'magma_cube',
        'Villager':'villager','VillagerGolem':'iron_golem','Witch':'witch','Endermite':'endermite','Guardian':'guardian',
        'ElderGuardian':'elder_guardian','Rabbit':'rabbit','PolarBear':'polar_bear','Shulker':'shulker',
        'EvocationIllager':'evoker','VindicationIllager':'vindicator','IllusionIllager':'illusioner','Llama':'llama',
        'Parrot':'parrot','Stray':'stray','Vex':'vex','WitherBoss':'wither','EnderDragon':'ender_dragon'}
    config = (sources / 'thaumcraft/common/config/ConfigAspects.java').read_text()
    aspect_tags = {}
    migrated = []
    for line in config.splitlines():
        m = re.search(r'registerObjectTag\("([^\"]+)", new AspectList\(\)((?:\.add\(Aspect\.[A-Z]+, \d+\))+)', line)
        target = tag_map.get(m.group(1)) if m else None
        if not m:
            m = re.search(r'registerEntityTag\("([^\"]+)", new AspectList\(\)((?:\.add\(Aspect\.[A-Z]+, \d+\))+), new ThaumcraftApi.EntityTagsNBT\[0\]', line)
            target = 'entity:minecraft:' + entities[m.group(1)] if m and m.group(1) in entities else None
        if m and target:
            amounts = {}
            for aspect, amount in re.findall(r'\.add\(Aspect\.([A-Z]+), (\d+)\)',m.group(2)):
                key = aspect_ids[aspect];amounts[key] = amounts.get(key,0)+int(amount)
            aspect_tags.setdefault(target, amounts)
            migrated.append({'legacy':m.group(1),'target':target,'aspects':amounts})
    js(RES / 'thaumcraft-port/aspect-tags.json', aspect_tags)
    js(ROOT / 'reference/aspect-migration.json', migrated)

    for code in ['en_us','ru_ru']:
        old = {}
        for line in z.read(f'assets/thaumcraft/lang/{code}.lang').decode('utf-8-sig').splitlines():
            if '=' in line and not line.startswith('#'):
                k,v = line.split('=',1);old[k]=v
        lang = {key:value for key,value in old.items() if key.startswith('tc.aspect.')}
        for item in items + ['thaumometer']:
            key = f'item.{item}.name'
            if item.startswith(('ingot_','nugget_')): key='item.'+item.replace('_','.',1)+'.name'
            if item.startswith(('amulet_','ring_','girdle_')): key='item.baubles.'+item+'.name'
            if item=='thaumonomicon':key='item.thaumonomicon.normal.name'
            lang[f'item.thaumcraft.{item}'] = old.get(key, item.replace('_',' ').title())
        lang['item.thaumcraft.crystal_essence'] = 'Вис-кристалл' if code=='ru_ru' else 'Vis Crystal'
        for block in blocks:lang[f'block.thaumcraft.{block}'] = old['tile.'+block+'.name']
        ru = code=='ru_ru'
        lang.update({
          'itemGroup.thaumcraft':'Thaumcraft — экспериментальный порт' if ru else 'Thaumcraft — Experimental Port',
          'thaumcraft.port.scan.new':'Новое открытие:' if ru else 'New discovery:',
          'thaumcraft.port.scan.known':'Уже изучено:' if ru else 'Already scanned:',
          'thaumcraft.port.scan.unknown':'Аспекты этого объекта пока не перенесены.' if ru else 'This subject has no migrated aspects yet.',
          'thaumcraft.port.journal.count':'Изучено объектов: %s' if ru else 'Subjects scanned: %s',
          'thaumcraft.port.journal.empty':'Изучайте блоки и существ таумометром. Чтобы изучить предмет, возьмите его во вторую руку и используйте таумометр в воздух.' if ru else 'Scan blocks and creatures with the Thaumometer. To scan an item, hold it in your other hand and use the Thaumometer in the air.',
          'thaumcraft.port.journal.stage':'Журнал открытий • дерево исследований ещё не перенесено' if ru else 'Discovery journal • research tree not yet ported'
        })
        js(RES / f'assets/thaumcraft/lang/{code}.json', lang)

    js(RES / 'pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Thaumcraft experimental 1.21.1 port resources'}})
    js(ROOT / 'reference/provenance.json', {'input':jar.name, 'input_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
        'original_author':'Azanor', 'assets':provenance, 'notes':['scanner.mtl texture paths normalized',
        'Legacy textures/items and textures/blocks migrated to textures/item and textures/block for the vanilla 1.21.1 atlas.',
        'New registry IDs replace old metadata variants; automatic save conversion is not implemented.']})
    js(ROOT / 'reference/class-inventory.json', [{'class':n[:-6], 'bytes':z.getinfo(n).file_size}
                                               for n in z.namelist() if n.endswith('.class')])
    for n in z.namelist():
        if n.startswith('assets/thaumcraft/research/') and n.endswith('.json'):
            p=ROOT/'reference/research'/Path(n).name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(n))
    # Remove only exact duplicates of files migrated above from an earlier run.
    # Leave unrelated or user-modified files untouched.
    for old_name, new_name in [('items', 'item'), ('blocks', 'block')]:
        old_dir = RES / 'assets/thaumcraft/textures' / old_name
        if old_dir.exists():
            for old_file in old_dir.iterdir():
                new_file = old_dir.parent / new_name / old_file.name
                if old_file.is_file() and new_file.is_file() and old_file.read_bytes() == new_file.read_bytes():
                    old_file.unlink()
            if not any(old_dir.iterdir()): old_dir.rmdir()
    print(f'Imported {len(matches)} aspects, {len(aspect_tags)} aspect bindings, {len(items)+1} items and {len(blocks)} blocks.')

if __name__ == '__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('jar',type=Path)
    parser.add_argument('--sources',type=Path,required=True)
    args=parser.parse_args();migrate(args.jar,args.sources)
