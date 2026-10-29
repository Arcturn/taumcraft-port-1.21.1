#!/usr/bin/env python3
"""Import actual TC6/Forge OreIngredient arrays for book-only cycling."""
import argparse, hashlib, json, pathlib, re

parser = argparse.ArgumentParser()
parser.add_argument('--runtime', type=pathlib.Path, required=True)
parser.add_argument('--raw-runtime', type=pathlib.Path, required=True)
args = parser.parse_args()
root = pathlib.Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources'
expanded = json.loads(args.runtime.read_text())
raw = json.loads(args.raw_runtime.read_text())
colors = ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
woods = ['oak','spruce','birch','jungle','acacia','dark_oak']

def descriptor(stack):
    identifier, meta = stack['item'], stack['metadata']
    count = stack.get('count', 1)
    # Vanilla's metadata was flattened into separate registry entries in 1.13.
    if identifier in ('minecraft:planks','minecraft:wooden_slab'):
        identifier = 'minecraft:' + woods[0 if meta == 32767 else meta] + ('_planks' if identifier.endswith(':planks') else '_slab')
        meta = 0
    elif identifier in ('minecraft:stained_glass','minecraft:stained_glass_pane','minecraft:wool','minecraft:carpet'):
        identifier = 'minecraft:' + colors[0 if meta == 32767 else meta] + '_' + identifier.split(':')[1]
        meta = 0
    elif identifier == 'minecraft:dye':
        identifier = 'minecraft:' + ['ink_sac','red_dye','green_dye','cocoa_beans','lapis_lazuli','purple_dye','cyan_dye','light_gray_dye','gray_dye','pink_dye','lime_dye','yellow_dye','light_blue_dye','magenta_dye','orange_dye','bone_meal'][meta]
        meta = 0
    identifier = {'minecraft:slime':'minecraft:slime_block','minecraft:trapdoor':'minecraft:oak_trapdoor','minecraft:reeds':'minecraft:sugar_cane'}.get(identifier, identifier)
    identifier = identifier.replace('_lightblue','_light_blue').replace('_silver','_light_gray') if identifier.startswith(('thaumcraft:nitor_','thaumcraft:candle_')) else identifier
    result = f'{identifier};{count};{meta}'
    if stack.get('nbt'): result += ';' + stack['nbt']
    return result

corpus = (resources/'thaumcraft-port/book-recipes.json').read_text()
for path in (resources/'assets/thaumcraft/research').glob('*.json'): corpus += path.read_text()
used = set(re.findall(r'oredict:([A-Za-z0-9_]+)', corpus))
ingredients = {name:[descriptor(stack) for stack in expanded['ores'].get(name, [])] for name in sorted(used)}
raw_ingredients = {name:[descriptor(stack) for stack in raw['ores'].get(name, [])] for name in sorted(used)}
# This importer-only name denotes ConfigRecipes.ingredientsFromBlocks(BlocksTC.candles.values()).
# Both original maps use the same EnumDyeColor keys and HashMap traversal as nitor.
if 'tallowCandles' in used:
    candles = [descriptor(dict(stack, item=stack['item'].replace('nitor_', 'candle_'))) for stack in raw['ores']['nitor']]
    ingredients['tallowCandles'] = candles
    raw_ingredients['tallowCandles'] = candles
(resources/'thaumcraft-port/book-ingredient-variants.json').write_text(json.dumps({'ingredients':ingredients,'raw':raw_ingredients},ensure_ascii=False,indent=2)+'\n')
for name, path in [('book-oreingredient-runtime.json',args.runtime),('book-oredictionary-runtime.json',args.raw_runtime)]:
    (root/'reference'/name).write_bytes(path.read_bytes())
report = {'source':'Minecraft 1.12.2 + Forge 14.23.5.2860 + Thaumcraft 6.1.BETA26 + Baubles 1.5.2',
          'expanded_sha256':hashlib.sha256(args.runtime.read_bytes()).hexdigest(),
          'raw_sha256':hashlib.sha256(args.raw_runtime.read_bytes()).hexdigest(),
          'used_names':len(used),'expanded_variants':sum(map(len,ingredients.values())),
          'empty_original_names':[key for key, value in ingredients.items() if not value],
          'order_note':'The captured original runtime order is preserved. TC6 HashMap iteration can change between processes; the variant set is authoritative.',
          'synthetic_names':{'tallowCandles':'ConfigRecipes.ingredientsFromBlocks(BlocksTC.candles.values()), original EnumDyeColor HashMap order'}}
(root/'reference/book-ingredient-import.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False))
