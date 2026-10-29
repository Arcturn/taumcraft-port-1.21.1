#!/usr/bin/env python3
"""Recover supported named complex/copy registrations; preserve source expressions and order.

Does not pretend to expand variable-target loops, ore dictionaries, or absent TC6 objects.
"""
import argparse, csv, json, pathlib, re, hashlib
p=argparse.ArgumentParser()
p.add_argument('--sources', type=pathlib.Path, required=True)
p.add_argument('--fields', type=pathlib.Path, required=True)
p.add_argument('--snapshot', type=pathlib.Path, required=True)
a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]
path=a.sources/'thaumcraft/common/config/ConfigAspects.java'
source=path.read_text()
fields={r['searge']:r['name'].lower() for r in csv.DictReader(a.fields.open())}
aspects=dict(re.findall(r'public static final Aspect (\w+) = new Aspect\("(\w+)"', (a.sources/'thaumcraft/api/aspects/Aspect.java').read_text()))
registered=json.loads(a.snapshot.read_text())['items']
alias={'boat':'oak_boat', 'melon_block':'melon', 'golden_rail':'powered_rail', 'wooden_pressure_plate':'oak_pressure_plate', 'wooden_button':'oak_button', 'noteblock':'note_block', 'trapdoor':'oak_trapdoor', 'grass':'grass_block', 'hardened_clay':'terracotta', 'stonebrick':'stone_bricks', 'brick_block':'bricks', 'speckled_melon':'glistering_melon_slice', 'silver_glazed_terracotta':'light_gray_glazed_terracotta', 'silver_shulker_box':'light_gray_shulker_box'}
colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
variants={'stonebrick':['stone_bricks','mossy_stone_bricks','cracked_stone_bricks','chiseled_stone_bricks'],'sandstone':['sandstone','chiseled_sandstone','cut_sandstone'],'stained_hardened_clay':[c+'_terracotta' for c in colors],'concrete':[c+'_concrete' for c in colors],'concrete_powder':[c+'_concrete_powder' for c in colors]}
tc={'arcaneWorkbench':'arcane_workbench','researchTable':'research_table','tableWood':'table_wood','tableStone':'table_stone','crucible':'crucible','thaumonomicon':'thaumonomicon','thaumometer':'thaumometer','celestialNotes':'celestial_notes'}
def resolve(owner, field, tail):
 meta=tail.split(',')[-1].strip() if ',' in tail else '0'
 if owner.endswith('TC'):
  name=tc.get(field)
  target='thaumcraft:'+name if name else None
  return target if target in registered and not registered[target]['display_only'] else None
 name=fields.get(field)
 if not name or name=='tripwire':return None
 if name in variants:return 'minecraft:'+variants[name][0 if meta=='Short.MAX_VALUE' else int(meta)]
 if name=='dye':return 'minecraft:'+{'0':'ink_sac','2':'green_dye','3':'cocoa_beans','4':'lapis_lazuli','15':'bone_meal'}.get(meta,'unknown_legacy_dye')
 if name=='golden_apple':return 'minecraft:'+('enchanted_golden_apple' if meta=='1' else 'golden_apple')
 if meta not in ('0','Short.MAX_VALUE'):return None
 return 'minecraft:'+alias.get(name,name)
stack=r'new ItemStack\((?:\((?:Item|Block)\))?(ItemsTC|BlocksTC|Items|Blocks)\.(\w+)([^)]*)\)'
head=re.compile(r'ThaumcraftApi\.register(Object|ComplexObject)Tag\('+stack+r', new AspectList\((.*?)\)((?:\.(?:add|merge|remove)\(Aspect\.\w+, \d+\))*)\);')
rules=[]; pending=[]; literals={}
for line, text in enumerate(source.splitlines(),1):
 m=head.search(text)
 if not m:continue
 kind, owner, field, tail, base, operations=m.groups()
 if kind=='Object' and not base and field!='celestialNotes':continue
 target=resolve(owner,field,tail)
 source_stack=re.fullmatch(stack,base) if base else None
 source_item=resolve(*source_stack.groups()) if source_stack else None
 ops=[{'op':op,'aspect':aspects[aspect],'amount':int(n)} for op,aspect,n in re.findall(r'\.(add|merge|remove)\(Aspect\.(\w+), (\d+)\)',operations)]
 provenance={'line':line,'expression':text.strip()}
 if not target or (base and not source_item):pending.append(provenance);continue
 if field=='celestialNotes':literals['item:'+target]={v['aspect']:v['amount'] for v in ops};continue
 targets=[target]
 if fields.get(field) in variants and 'Short.MAX_VALUE' in tail:targets=['minecraft:'+v for v in variants[fields[field]]]
 for target in targets:rules.append({'target':target,'kind':'complex' if kind=='ComplexObject' else 'copy','source':source_item,'operations':ops,**provenance})
(root/'src/main/resources/thaumcraft-port/aspect-recipe-rules.json').write_text(json.dumps(rules,ensure_ascii=False,indent=2)+'\n')
(root/'src/main/resources/thaumcraft-port/additional-aspect-tags.json').write_text(json.dumps(literals,indent=2)+'\n')
(root/'reference/aspect-recipe-rules.json').write_text(json.dumps({'source':'thaumcraft/common/config/ConfigAspects.java','sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'rules':len(rules),'pending_named_rules':pending,'not_expanded':['variable-target loops','ore-dictionary complex rules','historical eager-registration side effects'],'celestial_notes_source_line':584},ensure_ascii=False,indent=2)+'\n')
print(f'{len(rules)} named rules; {len(literals)} direct variants; {len(pending)} unresolved named rules')
