#!/usr/bin/env python3
"""Source-anchored BETA26 inventory compared with the real NeoForge datagen snapshot.

This is a deliberately narrow extractor, not a Java interpreter. Registry declarations,
metadata variants and recipe call sites are distinct. Conditional/looped recipe calls
retain their expression and enclosing control flow; they are NOT counted as recipes.
No registration, asset or successful compilation is treated as gameplay parity.
"""
import argparse
import collections
import hashlib
import json
import pathlib
import re
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
LEGACY_SHA = "9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f"
STR = r'"(?:\\.|[^"\\])*"'
DYES = "white orange magenta lightblue yellow lime pink gray silver cyan purple blue brown green red black".split()


def sha(data):
    return hashlib.sha256(data).hexdigest()


def masked(text):
    # Keep string literals and line positions, remove comments before matching calls.
    return re.sub(STR + r"|'(?:\\.|[^'\\])*'|//[^\n]*|/\*[\s\S]*?\*/",
                  lambda m: re.sub(r'[^\n]', ' ', m[0]) if m[0].startswith(('//', '/*')) else m[0], text)


def balanced(text, start, opening='(', closing=')'):
    assert text[start] == opening
    depth, quote, escaped = 0, None, False
    for i in range(start, len(text)):
        c = text[i]
        if quote:
            if escaped: escaped = False
            elif c == '\\': escaped = True
            elif c == quote: quote = None
        elif c in ('"', "'"): quote = c
        elif c == opening: depth += 1
        elif c == closing:
            depth -= 1
            if depth == 0: return text[start + 1:i], i + 1
    raise ValueError(f"Unbalanced expression at {start}")


def strings(text):
    return [json.loads(s) for s in re.findall(STR, text)]


class Sources:
    def __init__(self, folder):
        self.folder = folder
        self.files = {p.relative_to(folder).as_posix(): p.read_text() for p in folder.rglob('*.java')}
        self.by_class = {pathlib.Path(k).stem: k for k in self.files}
        self.used = set()

    def read(self, path):
        self.used.add(path)
        return masked(self.files[path])

    def evidence(self, path, offset):
        self.used.add(path)
        return {'path': path, 'line': self.files[path].count('\n', 0, offset) + 1}

    def constructor(self, cls):
        path = self.by_class.get(cls.split('.')[0])
        if not path: return '', None
        text = self.read(path)
        m = re.search(r'public\s+' + re.escape(cls.split('.')[-1]) + r'\s*\(', text)
        if not m: return '', path
        _, end = balanced(text, text.index('(', m.start()))
        body, _ = balanced(text, text.index('{', end), '{', '}')
        return body, path

    def identity(self, expression):
        m = re.search(r'new\s+([\w.]+)\s*\(', expression)
        if not m: raise ValueError('No constructor: ' + expression)
        cls = m[1]
        args, _ = balanced(expression, expression.index('(', m.start()))
        body, path = self.constructor(cls)
        direct = strings(args)
        candidates = direct
        variant_args = args if cls == 'ItemTCBase' else ''
        # Direct explicitly set registry name takes priority over constructor literals.
        reg = re.search(r'setRegistryName\s*\(([^;]*?)\)', expression)
        if reg and strings(reg[1]): candidates = strings(reg[1])[-1:]
        if not candidates:
            reg = re.search(r'setRegistryName\s*\(([^;]*?)\)', body)
            if reg and strings(reg[1]): candidates = strings(reg[1])[-1:]
            else:
                sup = re.search(r'super\s*\(', body)
                if sup:
                    super_args, _ = balanced(body, body.index('(', sup.start()))
                    # BETA26's two boolean-named block families.
                    ternary = re.search(r'\w+\s*\?\s*(' + STR + r')\s*:\s*(' + STR + ')', super_args)
                    if ternary and args.strip() in ('true', 'false'):
                        super_args = super_args.replace(ternary[0], ternary[1 if args.strip() == 'true' else 2])
                    candidates = strings(super_args)
                    variant_args = super_args
        if not candidates:
            raise ValueError(f"Unresolved registry name: {cls}({args}) in {path}")
        # Registry-bearing constructor/super arguments contain the name first.
        name = candidates[0]
        if not re.fullmatch(r'[a-z0-9_]+', name):
            raise ValueError(f"Invalid extracted name {name!r} from {cls}")
        variants = strings(variant_args)[1:]
        if cls == 'ItemChunksEdible':
            variants = strings(re.search(r'String\[\] variants = new String\[\]\{([^}]+)\}', self.read(path))[1])
        return name, cls, variants, path


def build(sources, jar, snapshot):
    rows = []
    unresolved = []
    def add(kind, name, source, **extra):
        row = {'kind': kind, 'id': name, 'source': source, **extra}
        rows.append(row)
        return row

    for kind, config, prefix in [('block', 'ConfigBlocks', 'BlocksTC'), ('item', 'ConfigItems', 'ItemsTC')]:
        path = f'thaumcraft/common/config/{config}.java'
        text = sources.read(path)
        start = text.index('public static void init' + ('Blocks(' if kind == 'block' else 'Items('))
        section, end = balanced(text, text.index('{', start), '{', '}')
        base = text.index('{', start) + 1
        for m in re.finditer(re.escape(prefix) + r'\.(\w+)\s*=\s*([^;]+);', section):
            expr = m[2]
            try:
                name, cls, variants, constructor = sources.identity(expr)
            except ValueError as error:
                unresolved.append({'kind': kind, 'field': m[1], 'expression': expr, 'reason': str(error)})
                continue
            add(kind, 'thaumcraft:' + name, sources.evidence(path, base + m.start()), field=m[1],
                legacy_class=cls, constructor_source=constructor, expression=expr,
                metadata_variants=[{'meta': i, 'name': v} for i, v in enumerate(variants)])
        # Dye loops are explicitly registered families, not blockstate-derived guesses.
        if kind == 'block':
            for family, cls in [('candle', 'BlockCandle'), ('banner', 'BlockBannerTC'), ('nitor', 'BlockNitor')]:
                m = re.search(r'new ' + cls + r'\("' + family + r'_"', section)
                assert m, family
                for dye in DYES:
                    assert f'assets/thaumcraft/blockstates/{family}_{dye}.json' in jar.namelist()
                    add(kind, f'thaumcraft:{family}_{dye}', sources.evidence(path, base + m.start()),
                        legacy_class=cls, dye=dye, metadata_variants=[])

    path = 'thaumcraft/common/config/ConfigEntities.java'
    text = sources.read(path)
    for m in re.finditer(r'EntityRegistry\.registerModEntity\s*\(', text):
        args, _ = balanced(text, text.index('(', m.start()))
        match = re.search(r'new ResourceLocation\("thaumcraft",\s*"([^"]+)"\),\s*(\w+)\.class', args)
        assert match, args
        # ResourceLocation 1.12 lowercases resource paths; retain original spelling too.
        add('entity_type', 'thaumcraft:' + match[1].lower(), sources.evidence(path, m.start()),
            legacy_spelling=match[1], legacy_class=match[2], expression=args)

    path = 'thaumcraft/Registrar.java'
    text = sources.read(path)
    for m in re.finditer(r'new (Potion\w+)\([^;]+?setRegistryName\("([^"]+)"\)', text):
        add('mob_effect', 'thaumcraft:' + m[2].lower(), sources.evidence(path, m.start()), legacy_class=m[1])
    for m in re.finditer(r'BiomeHandler\.(\w+) = new (BiomeGen\w+)\(', text):
        body, path2 = sources.constructor(m[2])
        name = re.search(r'setRegistryName\("thaumcraft", "([^"]+)"\)', body)[1]
        add('worldgen/biome', 'thaumcraft:' + name, sources.evidence(path, m.start()), legacy_class=m[2], constructor_source=path2,
            natural_generation=('configured' if name == 'magical_forest' else 'requires_behavior_audit'))

    path = 'thaumcraft/common/config/ConfigBlocks.java'
    text = sources.read(path)
    for m in re.finditer(r'GameRegistry\.registerTileEntity\((\w+)\.class, \(String\)"([^"]+)"', text):
        add('block_entity_type', m[2], sources.evidence(path, m.start()), legacy_class=m[1])
    for m in re.finditer(r'FluidRegistry\.registerFluid\(\(Fluid\)(\w+)\.instance\)', text):
        cls = m[1]
        body = text[text.index('class ' + cls):]
        name = re.search(r'String name = "([^"]+)"', body)[1]
        add('fluid', 'thaumcraft:' + name, sources.evidence(path, m.start()), legacy_class=cls)

    path = 'thaumcraft/common/lib/SoundsTC.java'
    text = sources.read(path)
    for m in re.finditer(r'getRegisteredSoundEvent\(event, "([^"]+)"\)', text):
        add('sound_event', m[1], sources.evidence(path, m.start()))

    for kind, config, pattern in [
        ('focus', 'ConfigItems', r'FocusEngine\.registerElement\((\w+)\.class'),
        ('seal', 'ConfigItems', r'SealHandler\.registerSeal\(new (\w+)\('),
        ('theory_card', 'ConfigResearch', r'TheorycraftManager\.registerCard\((\w+)\.class'),
        ('theory_aid', 'ConfigResearch', r'TheorycraftManager\.registerAid\(new ([\w.]+)\('),
    ]:
        path = f'thaumcraft/common/config/{config}.java'
        text = sources.read(path)
        for m in re.finditer(pattern, text):
            add(kind, m[1], sources.evidence(path, m.start()))

    path = 'thaumcraft/common/golems/GolemProperties.java'
    text = sources.read(path)
    for m in re.finditer(r'Golem(Material|Head|Arm|Leg|Addon)\.register\(new \w+\("([^"]+)"', text):
        add('golem_' + m[1].lower(), m[2], sources.evidence(path, m.start()))

    path = 'thaumcraft/api/golems/EnumGolemTrait.java'
    text = sources.read(path)
    for m in re.finditer(r'^    ([A-Z_]+)\(new ResourceLocation', text, re.M):
        add('golem_trait', m[1], sources.evidence(path, m.start()))

    path = 'thaumcraft/api/aspects/Aspect.java'
    text = sources.read(path)
    for m in re.finditer(r'public static final Aspect (\w+) = new Aspect\("([^"]+)"', text):
        add('aspect', m[2], sources.evidence(path, m.start()), symbol=m[1])

    path = 'thaumcraft/common/config/ModConfig.java'
    text = sources.read(path)
    for group in re.finditer(r'public static class (CONFIG_\w+)\s*\{', text):
        body, _ = balanced(text, text.index('{', group.start()), '{', '}')
        offset = text.index('{', group.start()) + 1
        for m in re.finditer(r'public static (boolean|int|float|double|String|int\[\]|String\[\]) (\w+) = ([^;]+);', body):
            add('setting', group[1] + '.' + m[2], sources.evidence(path, offset + m.start()), value_type=m[1], default_expression=m[3])

    for file in sorted(n for n in jar.namelist() if re.fullmatch(r'assets/thaumcraft/research/[^/]+\.json', n)):
        for entry in json.loads(jar.read(file))['entries']:
            add('research', entry['key'], {'jar_path': file}, category=entry['category'])

    # Recipe registration call sites: preserve expressions and nested control context.
    path = 'thaumcraft/common/config/ConfigRecipes.java'
    text = sources.read(path)
    patterns = r'(?:ThaumcraftApi\.(?:add\w*Recipe|addSmeltingBonus)|ConfigRecipes\.(?:oreDictRecipe|shapelessOreDictRecipe)|GameRegistry\.addSmelting|IDustTrigger\.registerDustTrigger|iForgeRegistry\.register)'
    for m in re.finditer(patterns + r'\s*\(', text):
        expr, end = balanced(text, text.index('(', m.start()))
        # Enclosing method/if/for/while blocks, without guessing runtime multiplicity.
        contexts = []
        for c in re.finditer(r'(?:public static void \w+|if|for|while)\s*\(', text[:m.start()]):
            header, ce = balanced(text, text.index('(', c.start()))
            pos = ce
            while pos < len(text) and text[pos].isspace(): pos += 1
            if pos < len(text) and text[pos] == '{':
                _, block_end = balanced(text, pos, '{', '}')
                if m.start() < block_end: contexts.append(text[c.start():ce].strip())
        add('recipe_call', f'{path}:{text.count(chr(10), 0, m.start()) + 1}', sources.evidence(path, m.start()),
            call=text[m.start():text.index('(', m.start())].strip(), expression=expr, contexts=contexts,
            runtime_expansion_verified=False)

    # Source code families are audit queues, deliberately not registered-object counts.
    source_families = {}
    for family, prefix in [('particles', 'thaumcraft/client/fx/'), ('renderers', 'thaumcraft/client/renderers/'),
                           ('worldgen', 'thaumcraft/common/world/'), ('warp', 'thaumcraft/common/lib/events/Warp'),
                           ('settings', 'thaumcraft/common/config/ModConfig')]:
        source_families[family] = sorted(p for p in sources.files if p.startswith(prefix))

    aliases = json.loads((ROOT / 'reference/parity-aliases.json').read_text())
    registry_keys = {'research': None, 'theory_card': None, 'theory_aid': None}
    for row in rows:
        kind, id = row['kind'], row['id']
        targets = aliases.get(kind, {}).get(id, [id])
        present = set(snapshot['registries'].get('minecraft:' + kind, []))
        if kind == 'research': present = set(snapshot['research'])
        if kind == 'theory_card':
            targets = [id.removeprefix('Card')]
            present = set(snapshot['theory_cards'])
        if kind == 'theory_aid':
            targets = [id.split('.')[-1].removeprefix('Aid')]
            present = set(snapshot['theory_aids'])
        if kind == 'aspect': present = set(snapshot.get('aspects', []))
        matches = [target for target in targets if target in present]
        # Exclude display-only carriers even if explicitly mapped by mistake.
        if kind == 'item':
            matches = [v for v in matches if not snapshot['items'][v]['display_only']]
        row['port'] = {'targets': targets, 'registered_matches': matches,
                       'status': 'definition_present' if matches and kind in registry_keys else 'registered_unverified' if matches else 'not_matched',
                       'gameplay_parity': 'not_verified', 'visual_parity': 'not_verified'}
        if kind == 'recipe_call': row['port']['status'] = 'recipe_mapping_pending'
        if kind == 'item':
            for variant in row['metadata_variants']:
                # A component-backed item needs a semantic check even when its ID exists.
                target = ('thaumcraft:' + variant['name'] if id == 'thaumcraft:baubles'
                          else id + '_' + variant['name'])
                if id in ('thaumcraft:celestial_notes', 'thaumcraft:phial', 'thaumcraft:label', 'thaumcraft:thaumonomicon'):
                    variant['port'] = {'target': id, 'status': 'component_behavior_audit_required'}
                else:
                    variant['port'] = {'target': target, 'status': 'registered_unverified' if target in present else 'not_matched'}

    seen = set()
    for row in rows:
        key = row['kind'], row['id']
        if key in seen: raise ValueError(f'Duplicate original identity: {key}')
        seen.add(key)
    media = []
    port_hashes = collections.defaultdict(list)
    for path in sorted((ROOT / 'src/main/resources/assets').rglob('*')):
        if path.is_file(): port_hashes[sha(path.read_bytes())].append(path.relative_to(ROOT).as_posix())
    for name in sorted(jar.namelist()):
        if name.startswith('assets/thaumcraft/') and name.endswith(('.png', '.jpg', '.ogg', '.obj', '.mtl')):
            digest = sha(jar.read(name))
            media.append({'source': name, 'sha256': digest, 'byte_identical_port_files': port_hashes.get(digest, []),
                          'event_hookup': 'not_audited', 'in_game_parity': 'not_verified'})
    return {'schema': 1, 'baseline': snapshot.get('mod_version', 'alpha.9.7'), 'legacy_sha256': LEGACY_SHA,
            'limitations': ['Source declarations are not a running Forge 1.12 registry dump.',
                            'Recipe call sites retain loops and conditions; counts are NOT concrete recipe counts.',
                            'Block items are represented by their blocks; standalone items are counted separately.',
                            'NBT-driven variants, dynamic recipes, particles and worldgen behavior require scenario audits.',
                            'Registration/definitions never imply survival availability or gameplay/visual parity.'],
            'source_sha256': {p: sha(sources.files[p].encode()) for p in sorted(sources.used)},
            'counts': dict(sorted(collections.Counter(r['kind'] for r in rows).items())),
            'unresolved_identities': unresolved, 'source_family_queues': source_families, 'media': media,
            'runtime_snapshot': snapshot, 'entries': sorted(rows, key=lambda r: (r['kind'], r['id']))}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--legacy-jar', type=pathlib.Path, required=True)
    parser.add_argument('--sources', type=pathlib.Path, required=True)
    parser.add_argument('--snapshot', type=pathlib.Path, required=True)
    parser.add_argument('--output', type=pathlib.Path, default=ROOT / 'reference/content-inventory.json')
    args = parser.parse_args()
    if sha(args.legacy_jar.read_bytes()) != LEGACY_SHA: raise SystemExit('Wrong legacy JAR')
    sources = Sources(args.sources)
    pinned = json.loads((ROOT / 'reference/coverage-inventory.json').read_text())
    for source in pinned['legacy_java_files']:
        if sha(sources.files[source['path']].encode()) != source['sha256']:
            raise SystemExit('Changed legacy source: ' + source['path'])
    with zipfile.ZipFile(args.legacy_jar) as jar:
        report = build(sources, jar, json.loads(args.snapshot.read_text()))
    report['snapshot_sha256'] = sha(args.snapshot.read_bytes())
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'counts': report['counts'], 'unresolved': report['unresolved_identities']}, ensure_ascii=False, indent=2))
    if report['unresolved_identities']: raise SystemExit(1)


if __name__ == '__main__':
    main()
