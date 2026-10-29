#!/usr/bin/env python3
"""Rebuild TC6 world assets, templates and modern data. Uses supplied JAR, recovered source, MCP CSV archive and vanilla 1.21.1 data."""
import argparse,copy,csv,gzip,hashlib,io,json,re,struct,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
def put(path,obj):
 p=RES/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n')
def tag(path,values):
 p=RES/path;old=json.loads(p.read_text()) if p.exists() else {'replace':False,'values':[]};old['values']=list(dict.fromkeys(old['values']+values));put(path,old)
def item(name,**extra):return {'type':'minecraft:item','name':name,**extra}
def table(entries,rolls=1):return {'type':'minecraft:block','pools':[{'rolls':rolls,'entries':entries}]}
def selfloot(id):put(f'data/thaumcraft/loot_table/blocks/{id}.json',table([item('thaumcraft:'+id,conditions=[{'condition':'minecraft:survives_explosion'}])]))
def nbt(value):
 if isinstance(value,str):b=value.encode();return 8,struct.pack('>H',len(b))+b
 if isinstance(value,int):return 3,struct.pack('>i',value)
 if isinstance(value,dict):
  b=b''
  for k,v in value.items():t,p=nbt(v);name=k.encode();b+=bytes([t])+struct.pack('>H',len(name))+name+p
  return 10,b+b'\0'
 if isinstance(value,list):
  parts=[nbt(v) for v in value];typ=parts[0][0] if parts else 10;assert all(t==typ for t,p in parts);return 9,bytes([typ])+struct.pack('>i',len(parts))+b''.join(p for t,p in parts)
 raise TypeError(value)
def migrate(jar,sources,vanilla,mappings):
 z=zipfile.ZipFile(jar);mc=zipfile.ZipFile(vanilla);mf=zipfile.ZipFile(mappings)
 fields={r['searge']:r['name'].lower() for r in csv.DictReader(io.StringIO(mf.read('fields.csv').decode()))}
 provenance={};blocks=[];translated={}
 def asset(source,dest=None):
  dest=(dest or source).replace('/textures/blocks/','/textures/block/').replace('/textures/items/','/textures/item/')
  p=RES/dest;p.parent.mkdir(parents=True,exist_ok=True);b=z.read(source);p.write_bytes(b);provenance[dest]={'source':source,'sha256':hashlib.sha256(b).hexdigest()}
  if source+'.mcmeta' in z.namelist():asset(source+'.mcmeta',dest+'.mcmeta')
 def tex(name):asset('assets/thaumcraft/textures/blocks/'+name+'.png')
 def model(id,data,state=None,ru=None,en=None):
  blocks.append(id);put(f'assets/thaumcraft/models/block/{id}.json',data)
  put(f'assets/thaumcraft/models/item/{id}.json',{'parent':'thaumcraft:block/'+id})
  put(f'assets/thaumcraft/blockstates/{id}.json',state or {'variants':{'':{'model':'thaumcraft:block/'+id}}});selfloot(id)
  translated['block.thaumcraft.'+id]=(en or id.replace('_',' ').title(),ru or id.replace('_',' '))
 for wood,ru,en in [('greatwood','великого дерева','Greatwood'),('silverwood','серебряного дерева','Silverwood')]:
  for name in ['log_'+wood,'log_'+wood+'_top','leaves_'+wood,'sapling_'+wood,'plank_'+wood]:tex(name)
  id='log_'+wood
  model(id,{'parent':'minecraft:block/cube_column','textures':{'side':'thaumcraft:block/'+id,'end':'thaumcraft:block/'+id+'_top'}},{'variants':{'axis=y':{'model':'thaumcraft:block/'+id},'axis=x':{'model':'thaumcraft:block/'+id,'x':90,'y':90},'axis=z':{'model':'thaumcraft:block/'+id,'x':90}}},'Бревно '+ru,en+' Log')
  id='plank_'+wood;model(id,{'parent':'minecraft:block/cube_all','textures':{'all':'thaumcraft:block/'+id}},ru='Доски '+ru,en=en+' Planks')
  id='leaves_'+wood;model(id,{'parent':'minecraft:block/leaves','render_type':'minecraft:cutout_mipped','textures':{'all':'thaumcraft:block/'+id}},ru='Листва '+ru,en=en+' Leaves')
  leaf=json.loads(mc.read('data/minecraft/loot_table/blocks/oak_leaves.json'));leaf['pools']=leaf['pools'][:1];leaf.pop('random_sequence',None)
  children=leaf['pools'][0]['entries'][0]['children'];children[0]['name']='thaumcraft:'+id;children[1]['name']='thaumcraft:sapling_'+wood
  children[1]['conditions'][1]['chances']=[1/75,1/73,1/69,1/61,1/45]
  if wood=='silverwood':leaf['pools'].append({'rolls':1,'conditions':[{'condition':'minecraft:inverted','term':children[0]['conditions'][0]}],'entries':[item('thaumcraft:nugget_quicksilver',conditions=[{'condition':'minecraft:table_bonus','enchantment':'minecraft:fortune','chances':[1/(75*.75),1/(73*.75),1/(69*.75),1/(61*.75),1/(45*.75)]}])]})
  put(f'data/thaumcraft/loot_table/blocks/{id}.json',leaf)
  id='sapling_'+wood;model(id,{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':'thaumcraft:block/'+id}},ru='Саженец '+ru,en=en+' Sapling')
  for kind in ['log','plank']:
   put(f'data/thaumcraft/recipe/{wood}_{kind}_conversion.json',{'type':'minecraft:crafting_shapeless','category':'building','ingredients':[{'item':'thaumcraft:log_'+wood}] if kind=='log' else [{'item':'thaumcraft:plank_'+wood},{'item':'thaumcraft:plank_'+wood}],'result':{'id':'thaumcraft:plank_'+wood,'count':4} if kind=='log' else {'id':'minecraft:stick','count':4}})
  for typ in ['block','item']:
   for t,vals in [('logs',['log_'+wood]),('logs_that_burn',['log_'+wood]),('planks',['plank_'+wood]),('leaves',['leaves_'+wood]),('saplings',['sapling_'+wood])]:tag(f'data/minecraft/tags/{typ}/{t}.json',['thaumcraft:'+v for v in vals])
  tag('data/minecraft/tags/block/mineable/axe.json',['thaumcraft:log_'+wood,'thaumcraft:plank_'+wood])
 model('grass_ambient',{'parent':'minecraft:block/grass_block','textures':{'bottom':'minecraft:block/dirt','top':'minecraft:block/grass_block_top','side':'minecraft:block/grass_block_side','overlay':'minecraft:block/grass_block_side_overlay'}},ru='Магический дёрн',en='Ambient Grass')
 loot=json.loads(mc.read('data/minecraft/loot_table/blocks/grass_block.json').decode().replace('minecraft:grass_block','thaumcraft:grass_ambient'));loot.pop('random_sequence',None);put('data/thaumcraft/loot_table/blocks/grass_ambient.json',loot)
 tag('data/minecraft/tags/block/dirt.json',['thaumcraft:grass_ambient'])
 tag('data/minecraft/tags/block/mineable/shovel.json',['thaumcraft:grass_ambient','thaumcraft:taint_soil'])
 for material in ['amber','cinnabar','quartz']:
  for typ in ['item','block']:tag(f'data/c/tags/{typ}/ores/{material}.json',['thaumcraft:ore_'+material])
 for id,ru,en in [('shimmerleaf','Мерцающий лист','Shimmerleaf'),('cinderpearl','Пылающий жемчуг','Cinderpearl'),('vishroom','Висогриб','Vishroom')]:
  tex(id);model(id,{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':'thaumcraft:block/'+id}},ru=ru,en=en)
 tex('crystal')
 faces={d:{'texture':'#crystal','tintindex':0} for d in ['up','down','north','south','east','west']}
 for size in range(4):
  elements=[]
  for x,zs,h,w in [(8,8,3+size*2,2),(3,5,2+size,1),(12,11,2+size*1.5,1),(5,12,1+size*1.5,1)][:size+1]:
   elements.append({'from':[x-w,0,zs-w],'to':[x+w,h,zs+w],'rotation':{'origin':[x,0,zs],'axis':'y','angle':45,'rescale':False},'faces':faces})
  put(f'assets/thaumcraft/models/block/crystal_cluster_{size}.json',{'textures':{'crystal':'thaumcraft:block/crystal','particle':'thaumcraft:block/crystal'},'elements':elements,'ambientocclusion':False,'render_type':'minecraft:cutout'})
 for aspect in ['aer','ignis','aqua','terra','ordo','perditio','vitium']:
  id='crystal_'+aspect;parts=[]
  for size in range(4):
   for face,rot in [('down',{}),('up',{'x':180}),('north',{'x':90}),('south',{'x':90,'y':180}),('east',{'x':90,'y':90}),('west',{'x':90,'y':270})]:parts.append({'when':{'size':str(size),face:'true'},'apply':{'model':f'thaumcraft:block/crystal_cluster_{size}',**rot}})
  model(id,{'parent':'thaumcraft:block/crystal_cluster_3'},{'multipart':parts},'Скопление кристаллов '+aspect.capitalize(),aspect.capitalize()+' Crystal Cluster')
  functions=[{'function':'minecraft:set_components','components':{'thaumcraft:crystal_aspect':aspect}}]
  for size in range(4):functions.append({'function':'minecraft:set_count','count':size+1,'conditions':[{'condition':'minecraft:block_state_property','block':'thaumcraft:'+id,'properties':{'size':str(size)}}]})
  functions.append({'function':'minecraft:explosion_decay'})
  put(f'data/thaumcraft/loot_table/blocks/{id}.json',table([item('thaumcraft:crystal_essence',functions=functions)]))
 for t in ['crate','urn']:
  for name in [f'loot_{t}_top',*[f'loot_{t}_side_{i}' for i in range(3)]]:tex(name)
  for rarity,ru in [('common','обычной'),('uncommon','необычной'),('rare','редкой')]:
   id=f'loot_{t}_{rarity}';data=json.loads(z.read(f'assets/thaumcraft/models/block/{id}.json').decode().replace('thaumcraft:blocks/','thaumcraft:block/'))
   model(id,data,ru=('Урна' if t=='urn' else 'Ящик')+' с '+ru+' добычей',en=rarity.title()+' Loot '+t.title())
 tex('porous_stone');model('stone_porous',{'parent':'minecraft:block/cube_all','textures':{'all':'thaumcraft:block/porous_stone'}},ru='Пористый камень',en='Porous Stone')
 # World hazards retain the supplied pixels; the 1.12 immediate-mode renderers are replaced by modern renderers.
 for source in ['assets/thaumcraft/textures/entity/taintseed.png','assets/thaumcraft/textures/entity/tslime.png','assets/thaumcraft/textures/misc/wispy.png','assets/thaumcraft/textures/entity/cultist.png','assets/thaumcraft/textures/misc/cultist_portal.png']:
  asset(source)
 for id,texture,ru in [('taint_fibre','taint_fibres','Волокна заражения'),('taint_soil','taint_soil_0','Заражённая почва'),('taint_rock','taint_rock','Заражённый камень'),('taint_crust','taint_crust_0','Заражённая корка'),('taint_log','log_taintwood_0','Заражённая древесина')]:
  tex(texture)
  if id=='taint_fibre':
   parts=[]
   for face,rot in [('down',{}),('up',{'x':180}),('north',{'x':90}),('south',{'x':90,'y':180}),('east',{'x':90,'y':90}),('west',{'x':90,'y':270})]:parts.append({'when':{face:'true'},'apply':{'model':'thaumcraft:block/taint_fibre_face',**rot}})
   put('assets/thaumcraft/models/block/taint_fibre_face.json',{'render_type':'minecraft:cutout','textures':{'fibre':'thaumcraft:block/taint_fibres','particle':'thaumcraft:block/taint_fibres'},'elements':[{'from':[0,0,0],'to':[16,.1,16],'faces':{'up':{'texture':'#fibre'},'down':{'texture':'#fibre'}}}]})
   model(id,{'parent':'thaumcraft:block/taint_fibre_face'},{'multipart':parts},ru,id.replace('_',' ').title())
   put(f'data/thaumcraft/loot_table/blocks/{id}.json',table([]))
  else:model(id,{'parent':'minecraft:block/cube_all','textures':{'all':'thaumcraft:block/'+texture}},ru=ru)
 tex('flux_goo')
 for level in range(8):
  put(f'assets/thaumcraft/models/block/flux_goo_{level}.json',{'render_type':'minecraft:translucent','textures':{'all':'thaumcraft:block/flux_goo','particle':'thaumcraft:block/flux_goo'},'elements':[{'from':[0,0,0],'to':[16,2*(level+1),16],'faces':{d:{'texture':'#all'} for d in ['up','down','north','south','east','west']}}]})
 model('flux_goo',{'parent':'thaumcraft:block/flux_goo_7'},{'variants':{f'level={i}':{'model':f'thaumcraft:block/flux_goo_{i}'} for i in range(8)}},'Флюксовая слизь','Flux Goo')
 put('data/thaumcraft/loot_table/blocks/flux_goo.json',table([]))
 for id,ru,en in [('void_seed','Семя пустоты','Void Seed'),('primordial_pearl','Изначальная жемчужина','Primordial Pearl')]:
  asset(f'assets/thaumcraft/textures/items/{id}.png');put(f'assets/thaumcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:item/'+id}});translated['item.thaumcraft.'+id]=(en,ru)
 for metal,ru,dst in [('iron','железа','minecraft:iron_ingot'),('gold','золота','minecraft:gold_ingot'),('copper','меди','minecraft:copper_ingot'),('cinnabar','киновари','thaumcraft:quicksilver')]:
  id='cluster_'+metal;asset(f'assets/thaumcraft/textures/items/{id}.png');put(f'assets/thaumcraft/models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:item/'+id}});translated['item.thaumcraft.'+id]=(metal.title()+' Native Cluster','Самородок '+ru)
  for typ,time in [('smelting',200),('blasting',100)]:put(f'data/thaumcraft/recipe/{id}_{typ}.json',{'type':'minecraft:'+typ,'category':'misc','ingredient':{'item':'thaumcraft:'+id},'result':{'id':dst,'count':2},'experience':1.0,'cookingtime':time})
  for prefix in ['raw_materials','raw_materials/'+metal]:tag(f'data/c/tags/item/{prefix}.json',['thaumcraft:'+id])
 for id,ru,en in [('cultist_portal_lesser','Малый портал культистов','Lesser Cultist Portal'),('cultist_knight','Багровый рыцарь','Crimson Knight'),('cultist_cleric','Багровый жрец','Crimson Cleric'),('cultist_orb','Багровая сфера','Crimson Orb')]:translated['entity.thaumcraft.'+id]=(en,ru)
 # Fortune increases the original 1/15 rare-drop probability by exactly 1/15 per level.
 compounds=re.findall(r'^    [A-Z]+\("([a-z]+)"', (ROOT/'src/main/java/thaumcraft/port/aspect/Aspect.java').read_text(),re.M)[6:]
 porous=[item('thaumcraft:crystal_essence',weight=1,functions=[{'function':'minecraft:set_components','components':{'thaumcraft:crystal_aspect':a}}]) for a in compounds]
 for name,weight in [('thaumcraft:amber',20),('thaumcraft:cluster_iron',20),('thaumcraft:cluster_gold',10),('thaumcraft:cluster_cinnabar',10),('thaumcraft:cluster_copper',10),('minecraft:diamond',2),('minecraft:emerald',4),('minecraft:flint',8),('minecraft:prismarine_crystals',3),('minecraft:prismarine_shard',3),('minecraft:clay_ball',30),('minecraft:quartz',15)]:porous.append(item(name,weight=weight))
 put('data/thaumcraft/loot_table/porous_rare.json',table(porous))
 put('data/thaumcraft/loot_table/blocks/stone_porous.json',table([{'type':'minecraft:alternatives','children':[{'type':'minecraft:loot_table','value':'thaumcraft:porous_rare','conditions':[{'condition':'minecraft:table_bonus','enchantment':'minecraft:fortune','chances':[min(1,(1+i)/15) for i in range(15)]}]},item('minecraft:gravel')]}]))
 for id,ru,en in [('flux_rift','Флюксовый разлом','Flux Rift'),('wisp','Висп','Wisp'),('taint_seed','Семя заражения','Taint Seed'),('taint_seed_prime','Большое семя заражения','Prime Taint Seed'),('thaumic_slime','Таум-слизень','Thaumic Slime')]:translated['entity.thaumcraft.'+id]=(en,ru)
 for id,ru,en in [('flux_taint','Заражение флюксом','Flux Taint'),('infectious_vis_exhaust','Заразное истощение вис','Infectious Vis Exhaust'),('vis_exhaust','Истощение вис','Vis Exhaust')]:translated['effect.thaumcraft.'+id]=(en,ru)
 # Keep TC6 weighted pools for every item already supported by this port; preserve explicit omissions.
 config=(sources/'thaumcraft/common/config/ModConfig.java').read_text();pools=[[],[],[]];omissions=[]
 for line in config.splitlines():
  m=re.search(r'addLootBagItem\(new ItemStack\((ItemsTC|Items)\.(\w+)(?:, (\d+))?(?:, (\d+))?\), (\d+), ([\d, ]+)\)',line)
  if not m:continue
  namespace,name,count,meta,weight,rarities=m.groups();count=int(count or 1);meta=int(meta or 0);functions=[]
  if namespace=='Items':
   name=fields[name];name={'golden_apple':'enchanted_golden_apple' if meta==1 else 'golden_apple','potionitem':'potion','dye':'lapis_lazuli'}.get(name,name);name='minecraft:'+name
  else:
   mapping={'salisMundus':'salis_mundus','primordialPearl':'primordial_pearl','voidSeed':'void_seed','baubles':['amulet_mundane','ring_mundane','girdle_mundane','charm_undying','amulet_fancy','ring_fancy','girdle_fancy'][meta] if meta<7 else None}
   if name not in mapping or mapping[name] is None:omissions.append(line.strip());continue
   name='thaumcraft:'+mapping[name]
   if 'primordialPearl' in line and meta:functions.append({'function':'minecraft:set_damage','damage':1-meta/8})
  if count!=1:functions.append({'function':'minecraft:set_count','count':count})
  if name=='minecraft:book':functions.append({'function':'minecraft:enchant_with_levels','levels':{'type':'minecraft:uniform','min':5,'max':30}})
  for rarity in map(int,rarities.split(',')):pools[rarity].append(item(name,weight=int(weight),**({'functions':functions} if functions else {})))
 for i,rarity in enumerate(['common','uncommon','rare']):
  put(f'data/thaumcraft/loot_table/chests/{rarity}.json',{'type':'minecraft:chest','pools':[{'rolls':1,'entries':pools[i]}]})
  for t in ['urn','crate']:
   id=f'loot_{t}_{rarity}';put(f'data/thaumcraft/loot_table/blocks/{id}.json',table([{'type':'minecraft:loot_table','value':f'thaumcraft:chests/{rarity}'}],{'type':'minecraft:uniform','min':1+i,'max':3+i}))
 # Vanilla's validated modern ore table carries Silk Touch, Fortune and explosions.
 for ore,drop in [('ore_amber','thaumcraft:amber'),('ore_quartz','minecraft:quartz')]:
  loot=json.loads(mc.read('data/minecraft/loot_table/blocks/diamond_ore.json').decode().replace('minecraft:diamond_ore','thaumcraft:'+ore).replace('minecraft:diamond',drop));loot.pop('random_sequence',None)
  if ore=='ore_amber':loot['pools'][0]['entries'][0]['children'][1]['functions'].insert(0,{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':2}})
  put(f'data/thaumcraft/loot_table/blocks/{ore}.json',loot)
 for typ,time in [('smelting',200),('blasting',100)]:put(f'data/thaumcraft/recipe/cinnabar_{typ}.json',{'type':'minecraft:'+typ,'category':'misc','ingredient':{'item':'thaumcraft:ore_cinnabar'},'result':{'id':'thaumcraft:quicksilver'},'experience':1.0,'cookingtime':time})
 for src,dst in [('cinderpearl','minecraft:blaze_powder'),('shimmerleaf','thaumcraft:quicksilver')]:put(f'data/thaumcraft/recipe/{src}_crushing.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':'thaumcraft:'+src}],'result':{'id':dst}})
 tag('data/thaumcraft/tags/block/crystal_support.json',['#minecraft:base_stone_overworld','#minecraft:stone_bricks','#minecraft:terracotta','minecraft:cobblestone','minecraft:mossy_cobblestone','minecraft:obsidian','thaumcraft:stone_porous'])
 tag('data/minecraft/tags/block/mineable/pickaxe.json',['thaumcraft:stone_porous'])
 for name,feature in [('deposits','deposits'),('vegetation','vegetation'),('lesser_portals','lesser_portals'),('greatwood_sapling','greatwood'),('silverwood_sapling','silverwood')]:put(f'data/thaumcraft/worldgen/configured_feature/{name}.json',{'type':'thaumcraft:'+feature,'config':{}})
 for name,step in [('deposits','underground_ores'),('vegetation','vegetal_decoration'),('lesser_portals','surface_structures')]:
  put(f'data/thaumcraft/worldgen/placed_feature/{name}.json',{'feature':'thaumcraft:'+name,'placement':[]})
  put(f'data/thaumcraft/neoforge/biome_modifier/{name}.json',{'type':'neoforge:add_features','biomes':'#minecraft:is_overworld','features':'thaumcraft:'+name,'step':step})
 forest=json.loads(mc.read('data/minecraft/worldgen/biome/forest.json'));forest['temperature']=.8;forest['downfall']=.4;forest['effects'].update(foliage_color=5635969,grass_color=6750149,water_color=30702)
 forest['features'][9]=[f for f in forest['features'][9] if f!='minecraft:trees_birch_and_oak']
 forest['spawners']['creature'] += [{'type':'minecraft:wolf','weight':2,'minCount':1,'maxCount':3},{'type':'minecraft:horse','weight':2,'minCount':1,'maxCount':3}]
 forest['spawners']['monster'] += [{'type':'minecraft:vex','weight':1,'minCount':1,'maxCount':1}]
 forest['spawners']['monster'].append({'type':'thaumcraft:wisp','weight':20,'minCount':1,'maxCount':3})
 put('data/thaumcraft/worldgen/biome/magical_forest.json',forest)
 for b in ['is_overworld','is_forest']:tag(f'data/minecraft/tags/worldgen/biome/{b}.json',['thaumcraft:magical_forest'])
 tag('data/thaumcraft/tags/worldgen/biome/magical.json',['thaumcraft:magical_forest'])
 tag('data/thaumcraft/tags/worldgen/biome/has_mound.json',['#minecraft:is_forest','#minecraft:is_taiga','minecraft:plains','minecraft:sunflower_plains','minecraft:savanna'])
 put('data/thaumcraft/worldgen/structure/mound.json',{'type':'thaumcraft:mound','biomes':'#thaumcraft:has_mound','step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'none'})
 put('data/thaumcraft/worldgen/structure_set/mounds.json',{'structures':[{'structure':'thaumcraft:mound','weight':1}],'placement':{'type':'minecraft:random_spread','spacing':10,'separation':4,'salt':73429105}})
 # The 2,441 fixed original block placements, with exactly mapped metadata and modern BE NBT.
 source=(sources/'thaumcraft/common/world/objects/WorldGenMound.java').read_text().split('float rr =')[0]
 rows=re.findall(r'new BlockPos\(i \+ (\d+), j \+ (\d+), k \+ (\d+)\), Blocks\.(field_\w+)\.func_(176223_P|176203_a)\((\d*)\)',source);assert len(rows)==2441
 for trapped in [False,True]:
  palette=[];placed={}
  def place(pos,state,be=None):
   if state not in palette:palette.append(state)
   placed[tuple(pos)]={'pos':pos,'state':palette.index(state),**({'nbt':be} if be else {})}
  for x,y,zs,field,method,meta in rows:
   name=fields[field];meta=int(meta or 0);props={}
   if name=='stone_stairs':name='cobblestone_stairs';props={'facing':['east','west','south','north'][meta&3],'half':'top' if meta&4 else 'bottom','shape':'straight','waterlogged':'false'}
   elif name=='ladder':props={'facing':{2:'north',3:'south',4:'west',5:'east'}[meta],'waterlogged':'false'}
   elif name=='stonebrick':name=['stone_bricks','mossy_stone_bricks','cracked_stone_bricks','chiseled_stone_bricks'][meta]
   name={'grass':'grass_block','tallgrass':'short_grass'}.get(name,name)
   place([int(x),int(y)+1,int(zs)],{'Name':'minecraft:'+name,**({'Properties':props} if props else {})})
  for zs in [7,11]:place([9,2,zs],{'Name':'minecraft:structure_block','Properties':{'mode':'data'}},{'id':'minecraft:structure_block','mode':'DATA','metadata':'loot'})
  place([10,2,9],{'Name':'minecraft:'+('trapped_chest' if trapped else 'chest'),'Properties':{'facing':'west','type':'single','waterlogged':'false'}},{'id':'minecraft:'+('trapped_chest' if trapped else 'chest'),'LootTable':'minecraft:chests/simple_dungeon'})
  if trapped:place([10,0,9],{'Name':'minecraft:tnt'})
  for zs,mob in [(4,'skeleton'),(14,'zombie')]:place([4,6,zs],{'Name':'minecraft:spawner'},{'id':'minecraft:mob_spawner','SpawnData':{'entity':{'id':'minecraft:'+mob}}})
  data={'DataVersion':3955,'size':[19,17,19],'palette':palette,'blocks':list(placed.values()),'entities':[]}
  name='mound_trapped' if trapped else 'mound';p=RES/f'data/thaumcraft/structure/{name}.nbt';p.write_bytes(gzip.compress(b'\x0a\x00\x00'+nbt(data)[1],mtime=0))
 from PIL import Image
 atlas=Image.open(io.BytesIO(z.read('assets/thaumcraft/textures/misc/potions.png'))).convert('RGBA')
 for effect,(x,y) in {'flux_taint':(3,1),'infectious_vis_exhaust':(6,1),'vis_exhaust':(5,1)}.items():
  dest=f'assets/thaumcraft/textures/mob_effect/{effect}.png';p=RES/dest;p.parent.mkdir(exist_ok=True,parents=True)
  atlas.crop((x*18,y*18,x*18+18,y*18+18)).save(p)
  provenance[dest]={'source':'assets/thaumcraft/textures/misc/potions.png','transform':{'crop':[x*18,y*18,18,18]},'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
 # Exact explicit aspect bindings from the original source, kept separate from the core importer.
 source=(sources/'thaumcraft/common/config/ConfigAspects.java').read_text()
 enum=(sources/'thaumcraft/api/aspects/Aspect.java').read_text()
 aspect_ids=dict(re.findall(r'public static final Aspect (\w+) = new Aspect\("(\w+)"',enum))
 names={'logGreatwood':'log_greatwood','logSilverwood':'log_silverwood','leafGreatwood':'leaves_greatwood','leafSilverwood':'leaves_silverwood','saplingGreatwood':'sapling_greatwood','saplingSilverwood':'sapling_silverwood','shimmerleaf':'shimmerleaf','cinderpearl':'cinderpearl','vishroom':'vishroom','stonePorous':'stone_porous','taintFibre':'taint_fibre','taintSoil':'taint_soil','taintRock':'taint_rock','taintCrust':'taint_crust','taintLog':'taint_log','primordialPearl':'primordial_pearl','crystalAir':'crystal_aer','crystalFire':'crystal_ignis','crystalWater':'crystal_aqua','crystalEarth':'crystal_terra','crystalOrder':'crystal_ordo','crystalEntropy':'crystal_perditio','crystalTaint':'crystal_vitium'}
 bindings={};migration=[]
 for line in source.splitlines():
  m=re.search(r'registerObjectTag\(new ItemStack\((?:BlocksTC|ItemsTC)\.(\w+)(?:, [^)]*)?\), new AspectList\(\)(.*?)\);',line)
  if not m or m[1] not in names:continue
  amounts={aspect_ids[a]:int(n) for a,n in re.findall(r'\.add\(Aspect\.(\w+), (\d+)\)',m[2])}
  key='item:thaumcraft:'+names[m[1]];bindings[key]=amounts;migration.append({'target':key,'aspects':amounts,'source':line.strip()})
 for old,new in [('TaintSeed','taint_seed'),('TaintSeedPrime','taint_seed_prime')]:
  for line in source.splitlines():
   if 'registerEntityTag("Thaumcraft.'+old+'"' in line:
    amounts={aspect_ids[a]:int(n) for a,n in re.findall(r'\.add\(Aspect\.(\w+), (\d+)\)',line)};key='entity:thaumcraft:'+new;bindings[key]=amounts;migration.append({'target':key,'aspects':amounts,'source':line.strip()})
 put('thaumcraft-port/world-aspect-tags.json',bindings)
 (ROOT/'reference/world-aspect-migration.json').write_text(json.dumps(migration,ensure_ascii=False,indent=2)+'\n')
 for entity in ['flux_rift','wisp','taint_seed','taint_seed_prime','thaumic_slime','cultist_portal_lesser','cultist_orb']:
  put(f'data/thaumcraft/loot_table/entities/{entity}.json',{'type':'minecraft:entity','pools':[]})
 for entity in ['cultist_knight','cultist_cleric']:
  put(f'data/thaumcraft/loot_table/entities/{entity}.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[item('minecraft:gold_nugget',functions=[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':0,'max':2}},{'function':'minecraft:enchanted_count_increase','enchantment':'minecraft:looting','count':{'type':'minecraft:uniform','min':0,'max':1}}])]}]})
 for lang,index in [('en_us',0),('ru_ru',1)]:
  path=f'assets/thaumcraft/lang/{lang}.json';d=json.loads((RES/path).read_text());d.update({key:value[index] for key,value in translated.items()});d.update({'biome.thaumcraft.magical_forest':['Magical Forest','Магический лес'][index],'message.thaumcraft.aura':['Aura: base %s | vis %s | flux %s','Аура: база %s | вис %s | флюкс %s'][index]});put(path,d)
 (ROOT/'reference/worldgen-provenance.json').write_text(json.dumps({'input_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'assets':provenance,'fixed_mound_placements':len(rows),'loot_omissions':omissions},ensure_ascii=False,indent=2)+'\n')
 print('Worldgen resources:',len(blocks),'blocks,',len(provenance),'original assets,',len(rows),'mound placements; loot omissions',len(omissions))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--jar',type=Path,required=True);p.add_argument('--sources',type=Path,required=True);p.add_argument('--vanilla',type=Path,required=True);p.add_argument('--mappings',type=Path,required=True);a=p.parse_args();migrate(a.jar,a.sources,a.vanilla,a.mappings)
