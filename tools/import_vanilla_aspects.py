#!/usr/bin/env python3
"""Flatten explicit TC6 vanilla ItemStack registrations using the original MCP field map."""
import argparse,csv,io,json,pathlib,re,zipfile
p=argparse.ArgumentParser();p.add_argument('--sources',type=pathlib.Path,required=True);p.add_argument('--mappings',type=pathlib.Path,required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]
with zipfile.ZipFile(a.mappings) as z:fields={r['searge']:r['name'].lower() for r in csv.DictReader(io.StringIO(z.read('fields.csv').decode()))}
aspect_source=(a.sources/'thaumcraft/api/aspects/Aspect.java').read_text();ids=dict(re.findall(r'public static final Aspect (\w+) = new Aspect\("(\w+)"',aspect_source))
aliases={'lit_redstone_ore':'redstone_ore','farmland':'farmland','grass_path':'dirt_path','waterlily':'lily_pad','deadbush':'dead_bush','melon':'melon_slice','magma':'magma_block','chorus_fruit_popped':'popped_chorus_fruit','web':'cobweb','potionitem':'potion','portal':'nether_portal','mob_spawner':'spawner'}
variants={'dirt':['dirt','coarse_dirt','podzol'],'double_plant':['sunflower','lilac','tall_grass','large_fern','rose_bush','peony'],'sponge':['sponge','wet_sponge'],'skull':['skeleton_skull','wither_skeleton_skull','zombie_head','player_head','creeper_head','dragon_head']}
wild={'coal':['coal','charcoal'],'tallgrass':['dead_bush','short_grass','fern'],'red_flower':['poppy','blue_orchid','allium','azure_bluet','red_tulip','orange_tulip','white_tulip','pink_tulip','oxeye_daisy'],'yellow_flower':['dandelion'],'fish':['cod','salmon','tropical_fish','pufferfish'],'cooked_fish':['cooked_cod','cooked_salmon']}
rows=[];out={};unresolved=[]
source=(a.sources/'thaumcraft/common/config/ConfigAspects.java').read_text()
pattern=r'registerObjectTag\(new ItemStack\((?:\((?:Item|Block)\))?(?:Items|Blocks)\.(field_\w+)([^)]*)\), new AspectList\(\)((?:\.add\(Aspect\.[A-Z]+, \d+\))+)' 
for m in re.finditer(pattern,source):
 name=fields[m[1]];meta=m[2].split(',')[-1].strip() if ',' in m[2] else '0';targets=[]
 if name in variants:
  targets=variants[name] if meta=='Short.MAX_VALUE' else [variants[name][int(meta)]]
 elif name in wild:targets=wild[name] if meta=='Short.MAX_VALUE' else [wild[name][int(meta)]]
 elif name=='dye':targets=[{'0':'ink_sac','3':'cocoa_beans','4':'lapis_lazuli','15':'bone_meal'}[meta]]
 elif name=='wool':targets=['white_wool']
 elif name.startswith('record_'):targets=[name.replace('record_','music_disc_')]
 elif meta in ('0','Short.MAX_VALUE'):targets=[aliases.get(name,name)]
 else:unresolved.append(m.group(0));continue
 amounts={}
 for asp,n in re.findall(r'\.add\(Aspect\.(\w+), (\d+)\)',m[3]):amounts[ids[asp]]=amounts.get(ids[asp],0)+int(n)
 for target in targets:
  kind='block' if target in ('fire','nether_portal','end_portal') else 'item'
  key=kind+':minecraft:'+target;out[key]=amounts;rows.append({'legacy':name,'meta':meta,'target':key,'aspects':amounts})
(root/'src/main/resources/thaumcraft-port/vanilla-aspect-tags.json').write_text(json.dumps(out,indent=2)+'\n')
(root/'reference/vanilla-aspect-migration.json').write_text(json.dumps({'bindings':rows,'unresolved_simple_registrations':unresolved,'scope':'Explicit vanilla ItemStack + literal AspectList.add registrations; derived/loop/potion registrations handled separately.'},indent=2)+'\n')
print('Migrated',len(out),'vanilla bindings;',len(unresolved),'unresolved simple registrations')
