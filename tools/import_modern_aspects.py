#!/usr/bin/env python3
"""Rebuild deliberate 1.13–1.21.1 additions without changing migrated TC6 data.

Natural/loot-only materials seed the recipe graph. Crafted outputs normally derive
their materials; explicit copy rules cover world transformations and containers.
These are modern design decisions, not values attributed to Azanor or TC6.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/thaumcraft-port'
values, records, rules = {}, [], []


def seed(names, aspects, reason, kind='item'):
    for name in names.split():
        key = kind + ':' + (name if ':' in name else 'minecraft:' + name)
        assert key not in values, key
        values[key] = aspects.copy()
        records.append({'target': key, 'aspects': aspects.copy(), 'rationale': reason})


def rule(names, aspects, reason, source=None, kind='complex', remove=None):
    for name in names.split():
        target = 'minecraft:' + name
        assert all(r['target'] != target for r in rules), target
        ops = [{'op': 'add', 'aspect': a, 'amount': n} for a, n in aspects.items()]
        ops += [{'op': 'remove', 'aspect': a, 'amount': n} for a, n in (remove or {}).items()]
        rules.append({'target': target, 'kind': kind,
                      'source': None if source is None else 'minecraft:' + source,
                      'operations': ops, 'rationale': reason})


seed('deepslate', {'terra': 5}, 'New natural stone: same Terra budget as original stone; hardness does not multiply matter.')
seed('cobbled_deepslate', {'terra': 5, 'perditio': 1}, 'Broken deepslate parallels original cobblestone.')
seed('calcite', {'terra': 5, 'vitreus': 1}, 'Crystalline natural stone.')
seed('tuff basalt', {'terra': 5, 'ignis': 2}, 'Volcanic stone; polished and cut derivatives use recipes.')
seed('blackstone', {'terra': 5, 'tenebrae': 2}, 'Dark Nether stone.')
seed('gilded_blackstone', {'terra': 5, 'metallum': 5, 'desiderium': 5}, 'Natural blackstone containing a small gold inclusion.')
seed('nether_gold_ore', {'terra': 5, 'metallum': 5, 'desiderium': 5, 'ignis': 2}, 'Nether gold ore yields nuggets rather than a full gold ingot.')
seed('c:ores/coal', {'terra': 5, 'potentia': 15, 'ignis': 15}, 'Original coal-ore values also apply to deepslate and tagged compatible ores.', 'tag')
for metal, a in [('iron', {'terra': 2, 'metallum': 15}),
                 ('gold', {'terra': 2, 'metallum': 10, 'desiderium': 10}),
                 ('copper', {'terra': 2, 'metallum': 10, 'permutatio': 5})]:
    seed('c:raw_materials/' + metal, a, 'Raw metal keeps the original metal composition with a little residual rock.', 'tag')
seed('ancient_debris', {'terra': 5, 'metallum': 15, 'ignis': 5, 'ordo': 5}, 'Rare heat-resistant ancient metal; scrap, alloy and equipment derive through processing recipes.')
seed('crying_obsidian', {'terra': 5, 'ignis': 5, 'tenebrae': 5, 'praecantatio': 5}, 'Original obsidian composition with the magical property of respawn-anchor material.')
seed('soul_soil', {'terra': 3, 'spiritus': 3}, 'Soul-bearing soil without the trapping aspect of original soul sand.')
seed('rooted_dirt', {'terra': 5, 'herba': 1}, 'Dirt with roots.')
seed('mud', {'terra': 5, 'aqua': 3}, 'Waterlogged soil; packed mud and bricks derive from recipes.')
seed('dripstone_block pointed_dripstone', {'terra': 5, 'aqua': 1}, 'Mineral formations deposited by dripping water.')
seed('amethyst_shard', {'vitreus': 10, 'sensus': 5}, 'Resonant crystal; blocks, spyglass and tinted glass derive from recipes.')
seed('budding_amethyst', {'vitreus': 20, 'victus': 5}, 'Living source of growing crystals, distinct from a crafted amethyst block.')
for size, n in [('small', 2), ('medium', 4), ('large', 6)]:
    seed(size + '_amethyst_bud', {'vitreus': n, 'sensus': 1}, 'Immature resonant crystal, scaled with growth stage.')
seed('amethyst_cluster', {'vitreus': 10, 'sensus': 5}, 'Mature natural crystal cluster; no recipe can infer its composition.')

seed('bamboo', {'herba': 5}, 'Plant material, analogous to sugar cane without its water/air specialization.')
seed('mangrove_roots', {'herba': 10, 'aqua': 5}, 'Submerged woody roots; muddy roots derive from recipes.')
seed('hanging_roots', {'herba': 5, 'terra': 1}, 'Exposed roots with a small earth component.')
seed('azalea', {'herba': 15, 'victus': 5}, 'Growing shrub, using the original sapling scale.')
seed('flowering_azalea', {'herba': 15, 'victus': 5, 'sensus': 5}, 'Flowering shrub adds the original flower Sensus component.')
seed('moss_block', {'herba': 5, 'terra': 2, 'aqua': 1}, 'Moist ground vegetation; carpet derives by crafting.')
seed('big_dripleaf small_dripleaf', {'herba': 5, 'aqua': 2, 'motus': 1}, 'Aquatic leaves with a moving/tilting growth form.')
seed('spore_blossom', {'herba': 5, 'aer': 2, 'sensus': 5}, 'Flower dispersing visible airborne spores.')
seed('glow_lichen', {'herba': 3, 'lux': 3}, 'Small naturally luminous growth.')
seed('glow_berries', {'herba': 5, 'victus': 5, 'lux': 3}, 'Edible luminous berries; ordinary fruit uses the original Herba/Victus scale.')
seed('sweet_berries', {'herba': 5, 'victus': 5, 'aversio': 1}, 'Edible berries growing on a thorny bush.')
seed('cornflower lily_of_the_valley torchflower', {'herba': 5, 'victus': 1, 'sensus': 5}, 'New flowers use the same base values as original flowers.')
seed('wither_rose', {'herba': 5, 'mortuus': 5, 'perditio': 2}, 'A flower carrying the withering effect, not Thaumcraft taint.')
seed('pitcher_plant', {'herba': 5, 'aqua': 2, 'sensus': 5}, 'Large ornamental pitcher plant.')
seed('torchflower_seeds pitcher_pod', {'herba': 5, 'victus': 1, 'cognitio': 1}, 'Ancient viable seeds, using the original seed scale with a small history component.')
seed('pink_petals', {'herba': 2, 'sensus': 2}, 'Small decorative petals, less material than an entire flower.')
seed('kelp', {'herba': 5, 'aqua': 3}, 'Aquatic vegetation.')
seed('seagrass', {'herba': 3, 'aqua': 3}, 'Small aquatic vegetation.')
seed('tall_seagrass', {'herba': 5, 'aqua': 3}, 'Double-height aquatic vegetation has no inventory item.', 'block')
seed('sea_pickle', {'herba': 3, 'aqua': 3, 'lux': 3}, 'Small luminous aquatic colony.')
for coral in ['tube', 'brain', 'bubble', 'fire', 'horn']:
    seed(f'{coral}_coral {coral}_coral_fan', {'bestia': 5, 'aqua': 5, 'victus': 2}, 'Living coral is an aquatic animal colony.')
    seed(f'{coral}_coral_block', {'bestia': 10, 'aqua': 5, 'victus': 5}, 'Larger living coral colony.')
    seed(f'dead_{coral}_coral dead_{coral}_coral_fan', {'bestia': 2, 'terra': 3, 'mortuus': 2}, 'Dead mineralized remains of coral.')
    seed(f'dead_{coral}_coral_block', {'bestia': 5, 'terra': 5, 'mortuus': 5}, 'Large dead mineralized coral colony.')
seed('crimson_nylium', {'terra': 5, 'ignis': 2, 'herba': 2}, 'Nether substrate overgrown by crimson vegetation.')
seed('warped_nylium', {'terra': 5, 'alienis': 2, 'herba': 2}, 'Unusual warped Nether vegetation on stone.')
seed('crimson_fungus', {'herba': 5, 'tenebrae': 2, 'ignis': 2}, 'Nether fungus on the original mushroom scale.')
seed('warped_fungus', {'herba': 5, 'tenebrae': 2, 'alienis': 2}, 'Warped counterpart to the crimson fungus.')
seed('crimson_roots weeping_vines', {'herba': 5, 'ignis': 1}, 'Crimson Nether vegetation.')
seed('warped_roots twisting_vines nether_sprouts', {'herba': 5, 'alienis': 1}, 'Warped Nether vegetation; not automatically Vitium/taint.')
seed('warped_wart_block', {'herba': 10, 'alienis': 5}, 'Natural warped wart mass, not crafted from ordinary nether wart.')
seed('shroomlight', {'herba': 5, 'lux': 15}, 'Bright luminous fungal growth.')
seed('ochre_froglight verdant_froglight pearlescent_froglight', {'lux': 15, 'bestia': 5}, 'Luminous matter produced by frogs; colors share the same composition.')
seed('bee_nest', {'herba': 10, 'bestia': 5, 'vinculum': 5}, 'Natural wooden bee dwelling; crafted beehives derive separately.')
seed('honeycomb', {'bestia': 3, 'victus': 3, 'fabrico': 3}, 'Structured, biologically produced wax.')
seed('nautilus_shell', {'bestia': 5, 'aqua': 5, 'praemunio': 5}, 'Protective shell of an aquatic animal.')
seed('heart_of_the_sea', {'aqua': 20, 'praecantatio': 10, 'victus': 10}, 'Rare magical ocean heart powering a conduit.')
seed('turtle_scute armadillo_scute', {'bestia': 5, 'praemunio': 5}, 'Natural animal armor, parallel to original protective hide.')
seed('turtle_egg frogspawn', {'bestia': 5, 'victus': 5, 'aqua': 5}, 'New aquatic eggs on the original egg scale.')
seed('sniffer_egg', {'bestia': 10, 'victus': 10, 'cognitio': 5}, 'Large egg of an ancient animal.')
seed('phantom_membrane', {'exanimis': 5, 'volatus': 10, 'alkimia': 5}, 'Membrane from a flying undead creature, used in brewing.')
seed('goat_horn', {'bestia': 5, 'sensus': 10, 'aer': 5}, 'Animal horn used as a wind instrument; variants share material values.')
seed('glow_ink_sac', {'aqua': 2, 'bestia': 2, 'sensus': 5, 'lux': 5}, 'Original ink components plus luminescence.')
seed('trident', {'metallum': 15, 'aqua': 10}, 'Loot-only aquatic weapon; its weapon bonus is applied separately.')
seed('bell', {'metallum': 15, 'sensus': 15}, 'Cast metal sound instrument, obtained without a crafting recipe.')
seed('echo_shard', {'vitreus': 5, 'spiritus': 5, 'sensus': 5}, 'Resonant remnant linking sound and departed life.')
seed('sculk', {'tenebrae': 5, 'spiritus': 5, 'victus': 3}, 'Living dark growth sustained by deaths; not Thaumcraft corruption.')
seed('sculk_vein', {'tenebrae': 2, 'spiritus': 2}, 'Thin sculk growth has less material than a full block.')
seed('sculk_catalyst', {'spiritus': 10, 'mortuus': 10, 'victus': 5}, 'Converts the aftermath of deaths into new growth.')
seed('sculk_sensor', {'spiritus': 5, 'sensus': 10, 'motus': 5}, 'Detects vibrations and motion.')
seed('sculk_shrieker', {'spiritus': 5, 'sensus': 10, 'tenebrae': 10}, 'Dark warning organ responding to disturbances.')
seed('reinforced_deepslate', {'terra': 25, 'praemunio': 25, 'spiritus': 10}, 'Very resistant ancient-city structure; world-scannable despite no survival drop.')
seed('breeze_rod', {'aer': 15, 'motus': 5}, 'Wind counterpart to the original blaze rod; wind charges derive from its recipe.')
seed('heavy_core', {'metallum': 20, 'terra': 10, 'ordo': 10}, 'Dense durable core; the mace derives both this material and the breeze rod.')
seed('trial_key', {'metallum': 5, 'vinculum': 5, 'machina': 5}, 'Physical key to a trial vault.')
seed('ominous_trial_key', {'metallum': 5, 'vinculum': 5, 'machina': 5, 'praecantatio': 5}, 'Enchanted counterpart of the ordinary trial key.')
seed('ominous_bottle', {'vitreus': 5, 'praecantatio': 5, 'aversio': 5}, 'Bottled ominous effect; amplifier does not multiply its base material.')
seed('trial_spawner', {'bestia': 20, 'motus': 20, 'exanimis': 20, 'praecantatio': 20, 'ordo': 10}, 'Original spawner scale with ordered trial behavior.')
seed('vault', {'metallum': 20, 'vinculum': 20, 'desiderium': 20, 'praecantatio': 10}, 'Magical locked treasure container.')
seed('piglin_head', {'mortuus': 10, 'spiritus': 10, 'bestia': 5, 'humanus': 5}, 'Original skull scale adapted to a piglin.')
for name in 'angler archer arms_up blade brewer burn danger explorer flow friend guster heart heartbreak howl miner mourner plenty prize scrape sheaf shelter skull snort'.split():
    seed(name + '_pottery_sherd', {'terra': 5, 'cognitio': 5, 'sensus': 5}, 'Ancient inscribed ceramic fragment; illustrations do not change its bulk composition.')
for name in 'sentry dune coast wild ward eye vex tide snout rib spire wayfinder shaper silence raiser host flow bolt'.split():
    seed(name + '_armor_trim_smithing_template', {'cognitio': 5, 'fabrico': 5, 'sensus': 5}, 'Loot-first design template; explicit seed breaks the template-duplication recipe cycle.')
seed('netherite_upgrade_smithing_template', {'cognitio': 10, 'fabrico': 5, 'ordo': 5}, 'Ancient metallurgical instructions; explicit seed breaks duplication cycles.')
for name, special in [('globe', 'terra'), ('piglin', 'bestia'), ('flow', 'motus'), ('guster', 'aer')]:
    seed(name + '_banner_pattern', {'cognitio': 5, 'sensus': 5, special: 5}, 'Loot/trade-only decorative pattern; ordinary crafted patterns derive from ingredients.')
for name, special in [('pigstep', 'ignis'), ('otherside', 'alienis'), ('relic', 'cognitio'), ('5', 'spiritus'),
                      ('creator', 'fabrico'), ('creator_music_box', 'machina'), ('precipice', 'motus')]:
    seed('music_disc_' + name, {'sensus': 15, 'aer': 5, 'desiderium': 10, special: 5}, 'New recording uses the original music-disc scale plus one thematic component.')
seed('disc_fragment_5', {'sensus': 3, 'spiritus': 1, 'desiderium': 2}, 'Fragment of the new ancient recording, below the complete disc budget.')
seed('suspicious_sand', {'terra': 5, 'perditio': 5, 'cognitio': 1}, 'Original sand material with a small archaeological information component.')
seed('suspicious_gravel', {'terra': 5, 'perditio': 2, 'cognitio': 1}, 'Original gravel material with a small archaeological information component.')
seed('powder_snow', {'gelum': 20}, 'Loose snow in world; the filled bucket copies its container separately.', 'block')
seed('soul_fire', {'ignis': 20, 'spiritus': 5}, 'Original fire scale with the soul-bearing substrate.', 'block')

rule('exposed_copper weathered_copper oxidized_copper', {}, 'Oxidation/scraping changes state, not the metal quantity.', 'copper_block', 'copy')
rule('dried_kelp', {'ignis': 1}, 'Drying removes the water component, following original cooked-food registrations.', 'kelp', 'copy', {'aqua': 3})
rule('honey_bottle', {'victus': 10, 'bestia': 3}, 'Filled bottle retains the actual glass-container aspects.', 'glass_bottle', 'copy')
rule('powder_snow_bucket', {'gelum': 20}, 'Filled snow bucket retains the actual metal-container aspects.', 'bucket', 'copy')
rule('cod_bucket salmon_bucket tropical_fish_bucket pufferfish_bucket axolotl_bucket tadpole_bucket',
     {'bestia': 5, 'victus': 5}, 'Captured aquatic animal adds life to a complete water bucket.', 'water_bucket', 'copy')
rule('suspicious_stew', {'alkimia': 5}, 'Special-recipe mushroom stew gains a small effect/brewing component.', 'mushroom_stew', 'copy')
rule('decorated_pot', {'vacuos': 5, 'sensus': 5}, 'Special-recipe ceramic vessel; decoration patterns do not alter its base material.', 'terracotta', 'copy')
rule('bundle', {'vacuos': 5}, 'Container function; material derives if a feature pack enables its recipe.')
for family, additions in [('door', {'vinculum': 5, 'machina': 5}), ('trapdoor', {'motus': 5}),
                          ('button', {'machina': 5}), ('pressure_plate', {'machina': 5, 'sensus': 5}),
                          ('fence_gate', {'vinculum': 5, 'machina': 5})]:
    woods = 'mangrove cherry bamboo crimson warped'.split()
    if family in {'trapdoor', 'button', 'pressure_plate'}: woods += 'spruce birch jungle acacia dark_oak'.split()
    rule(' '.join(w + '_' + family for w in woods), additions, 'New wood variants inherit the corresponding original functional bonus; materials still derive.')
rule('polished_blackstone_button', {'machina': 5}, 'New stone button inherits the original button function.')
rule('polished_blackstone_pressure_plate', {'machina': 5, 'sensus': 5}, 'New stone pressure plate inherits the original function.')
rule('mangrove_boat cherry_boat bamboo_raft', {'aqua': 10, 'motus': 15}, 'New boats/raft inherit original boat function; wood derives.')
rule('barrel', {'vacuos': 15}, 'Chest-like storage capacity, with its own recipe materials.')
rule('smoker blast_furnace', {'ignis': 10, 'fabrico': 10}, 'Specialized crafting by heat.')
rule('stonecutter', {'fabrico': 10, 'instrumentum': 5}, 'Stone-working tool station.')
rule('grindstone', {'fabrico': 5, 'permutatio': 5}, 'Repair/disenchantment station transforms equipment.')
rule('cartography_table', {'cognitio': 10, 'fabrico': 10}, 'Makes and transforms maps.')
rule('fletching_table', {'fabrico': 10, 'volatus': 5}, 'Fletcher work station and ranged-projectile craft.')
rule('smithing_table', {'fabrico': 20}, 'Equipment-working table, matching the original crafting-table scale.')
rule('loom', {'fabrico': 10}, 'Patterned textile craft.')
rule('lectern', {'cognitio': 10}, 'Displays written knowledge.')
rule('composter', {'herba': 10, 'permutatio': 5}, 'Transforms organic matter into fertilizer.')
rule('campfire', {'ignis': 10, 'lux': 5}, 'Cooking fire and visible light.')
rule('soul_campfire', {'ignis': 10, 'lux': 5, 'spiritus': 5}, 'Soul-bearing campfire.')
rule('lantern', {'lux': 5}, 'Lamp function supplements its derived metal/torch material.')
rule('soul_lantern soul_torch', {'lux': 5, 'spiritus': 5}, 'Soul light supplements recipe materials.')
rule('chain', {'vinculum': 5}, 'Binding function of linked metal.')
rule('target', {'sensus': 10}, 'Detects projectile hits.')
rule('crafter', {'fabrico': 20, 'machina': 10}, 'Automated crafting combines the original table and mechanism roles.')
rule('copper_bulb exposed_copper_bulb weathered_copper_bulb oxidized_copper_bulb', {'lux': 10, 'machina': 5}, 'Switchable copper light; materials derive from each recipe.')
rule('lightning_rod', {'potentia': 10}, 'Conducts lightning energy.')
rule('calibrated_sculk_sensor', {'ordo': 5}, 'Restricts vibration detection to an ordered signal range.')
rule('chiseled_bookshelf', {'cognitio': 10, 'vacuos': 5}, 'Stores knowledge-bearing books.')

entities = {
    'allay': {'aer': 5, 'volatus': 10, 'spiritus': 10, 'permutatio': 5},
    'armadillo': {'bestia': 10, 'terra': 5, 'praemunio': 10},
    'axolotl': {'bestia': 10, 'aqua': 10, 'victus': 5},
    'bee': {'bestia': 5, 'volatus': 5, 'herba': 5},
    'bogged': {'exanimis': 20, 'humanus': 5, 'herba': 5, 'mortuus': 5},
    'breeze': {'aer': 15, 'motus': 15, 'volatus': 5},
    'camel': {'bestia': 15, 'terra': 10, 'aer': 5},
    'cat': {'bestia': 10, 'perditio': 10},
    'cod': {'bestia': 5, 'aqua': 10},
    'salmon': {'bestia': 5, 'aqua': 10},
    'tropical_fish': {'bestia': 5, 'aqua': 10},
    'pufferfish': {'bestia': 5, 'aqua': 10, 'mortuus': 5, 'praemunio': 5},
    'dolphin': {'bestia': 10, 'aqua': 10, 'motus': 5},
    'drowned': {'exanimis': 20, 'humanus': 10, 'aqua': 5},
    'fox': {'bestia': 10, 'terra': 5, 'desiderium': 5},
    'frog': {'bestia': 5, 'aqua': 5, 'motus': 5},
    'glow_squid': {'bestia': 5, 'aqua': 10, 'lux': 5},
    'goat': {'bestia': 10, 'terra': 5, 'motus': 5, 'aversio': 5},
    'hoglin': {'bestia': 20, 'ignis': 5, 'aversio': 5},
    'panda': {'bestia': 15, 'terra': 10, 'herba': 5},
    'phantom': {'exanimis': 15, 'volatus': 10, 'tenebrae': 5},
    'piglin': {'humanus': 10, 'bestia': 10, 'desiderium': 10},
    'piglin_brute': {'humanus': 10, 'bestia': 10, 'desiderium': 10, 'aversio': 10},
    'pillager': {'humanus': 10, 'aversio': 10},
    'ravager': {'bestia': 25, 'aversio': 15, 'perditio': 10},
    'sniffer': {'bestia': 15, 'herba': 10, 'cognitio': 5},
    'strider': {'bestia': 10, 'ignis': 15, 'motus': 5},
    'tadpole': {'bestia': 3, 'aqua': 5, 'victus': 5},
    'trader_llama': {'bestia': 15, 'aqua': 5},
    'turtle': {'bestia': 10, 'aqua': 10, 'praemunio': 10},
    'wandering_trader': {'humanus': 15, 'permutatio': 5, 'desiderium': 5},
    'warden': {'bestia': 30, 'spiritus': 20, 'sensus': 20, 'tenebrae': 20},
    'zoglin': {'bestia': 15, 'exanimis': 15, 'aversio': 5},
}
for name, aspects in entities.items():
    seed(name, aspects, 'New creature: original comparable-animal/undead scale, supplemented by its actual behavior.', 'entity')

seed('mushroom_stem', {'herba': 5, 'tenebrae': 2}, 'Flattened inventory item for the old giant-mushroom stem; neutral shared mushroom components, without cap-color specialization.')
rule('stripped_bamboo_block', {}, 'Stripping preserves the bamboo log material, as for the original log tag.', 'bamboo_block', 'copy')
rule('carved_pumpkin', {}, 'Modern separated carved state retains the original pumpkin material.', 'pumpkin', 'copy')
rule('purple_shulker_box', {}, 'Original default purple shulker box became an uncolored craft result in modern Minecraft; keep old color-copy rules linked to the actual modern base.', 'shulker_box', 'copy')
rule('chipped_anvil damaged_anvil', {}, 'Wear changes remaining service life, not the amount of metal in an existing anvil.', 'anvil', 'copy')
for host in ['stone', 'cobblestone', 'stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks', 'chiseled_stone_bricks', 'deepslate']:
    rule('infested_' + host, {'bestia': 5}, 'Explicit modern world-scan adaptation: host stone plus the hidden silverfish, not claimed as an original TC6 registration.', host, 'copy')
rule('copper_door', {'vinculum': 5, 'machina': 5}, 'Copper door inherits the original iron-door functional bonuses.')
rule('copper_trapdoor', {'motus': 5}, 'Copper hatch inherits the original trapdoor motion bonus.')
for shape in ['door', 'trapdoor']:
    rule(' '.join(stage + '_copper_' + shape for stage in ['exposed', 'weathered', 'oxidized']), {},
         'World oxidation/scraping changes the copper state, not its material or door function.', 'copper_' + shape, 'copy')
rule('candle', {'ignis': 1, 'lux': 5}, 'Candle lighting function uses the original torch scale, while wax and wick derive.')
rule(' '.join(color + '_candle' for color in 'white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()),
     {'sensus': 1}, 'Colored candle preserves its material/light composition and gains the small decorative Sensus used by original colored blocks.', 'candle', 'copy')

legacy = {}
for name in ['aspect-tags.json', 'world-aspect-tags.json', 'vanilla-aspect-tags.json', 'additional-aspect-tags.json', 'deepslate-aspect-tags.json']:
    legacy.update(json.loads((RES / name).read_text()))
assert not (set(values) & set(legacy)), 'Modern seeds must not overwrite migrated original definitions'
old_rules = {r['target'] for r in json.loads((RES / 'aspect-recipe-rules.json').read_text())}
assert not (old_rules & {r['target'] for r in rules}), 'Modern rules must not override original rules'
for name, data in [('modern-aspect-tags.json', values), ('modern-aspect-recipe-rules.json', rules)]:
    (RES / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
(ROOT / 'reference/modern-aspect-design.json').write_text(json.dumps({
    'scope': 'Deliberate port design for Minecraft 1.13–1.21.1 content; not original TC6 values.',
    'policy': 'Preserve migrated explicit values. Seed natural/loot-only sources, derive crafted materials, add named functional bonuses. Never assign arbitrary universal fallback aspects.',
    'seed_count': len(values), 'recipe_rule_count': len(rules),
    'records': records, 'recipe_rules': rules,
    'equipment_bonuses': {
        'crossbow': {'aversio': 10, 'volatus': 5, 'machina': 5},
        'trident': {'aversio': 24, 'volatus': 5}, 'mace': {'aversio': 24},
        'brush': {'instrumentum': 4}, 'spyglass': {'sensus': 10},
        'policy': 'Maximum merge with recipe/material aspects, exactly as in original getBonusTags; never add the same bonus twice.'
    },
    'modern_enchantment_roles': {
        'loyalty': ['vinculum'], 'impaling': ['aqua', 'aversio'], 'riptide': ['aqua', 'motus'],
        'channeling': ['potentia'], 'piercing': ['aversio'], 'multishot': ['volatus', 'aversio'],
        'quick_charge': ['motus'], 'swift_sneak': ['motus'], 'soul_speed': ['spiritus', 'motus'],
        'density': ['terra', 'aversio'], 'breach': ['perditio', 'aversio'], 'wind_burst': ['aer', 'motus'],
        'policy': 'Three units per level, shared equally with integer truncation for two roles. Original magic rarity bonus and seven-aspect culling remain active.'
    },
    'existing_coverage': 'Original common tags already cover newer logs, leaves, saplings, copper and most deepslate ores. Those are not redefined here.',
    'runtime_audit': 'ModernAspectFixtures writes build/modern-aspect-coverage.json using loaded game registries, tags and the actual recipe calculator. Unresolved items are listed, not silently marked supported.'
}, ensure_ascii=False, indent=2) + '\n')
print(f'Generated {len(values)} modern seed definitions and {len(rules)} recipe/function rules without legacy collisions.')
