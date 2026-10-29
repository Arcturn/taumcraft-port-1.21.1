#!/usr/bin/env python3
"""Original workbench/crucible geometry and resources, plus explicitly migrated recipes."""
import argparse,hashlib,json,pathlib,zipfile
p=argparse.ArgumentParser();p.add_argument('--jar',type=pathlib.Path,required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];res=root/'src/main/resources';records=[]
def js(path,data):
 path=res/path;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2).replace('thaumcraft:blocks/','thaumcraft:block/').replace('thaumcraft:items/','thaumcraft:item/')+'\n')
def block(name,model):
 js(f'assets/thaumcraft/models/block/{name}.json',model)
 js(f'assets/thaumcraft/blockstates/{name}.json',{'variants':{'':{'model':f'thaumcraft:block/{name}'}}})
 js(f'assets/thaumcraft/models/item/{name}.json',{'parent':f'thaumcraft:block/{name}'})
 js(f'data/thaumcraft/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':f'thaumcraft:{name}'}]}]})
with zipfile.ZipFile(a.jar) as z:
 def copy(name):
  data=z.read(name);target=name.replace('/textures/blocks/','/textures/block/').replace('/textures/items/','/textures/item/');path=res/target;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
  records.append({'source':name,'target':target,'sha256':hashlib.sha256(data).hexdigest()})
 def texture(ref):
  if ref.startswith('thaumcraft:'):
   name='assets/thaumcraft/textures/'+ref.split(':',1)[1]+'.png';copy(name)
   if name+'.mcmeta' in z.namelist():copy(name+'.mcmeta')
 copy('assets/thaumcraft/textures/gui/arcaneworkbench.png')
 for name in ['arcane_workbench','crucible']:
  path=f'assets/thaumcraft/models/block/{name}.json';model=json.loads(z.read(path))
  # Keep TC6 geometry, but inherit standard block GUI/hand transforms in 1.21.1.
  model['parent']='minecraft:block/block';block(name,model)
  for ref in model['textures'].values():texture(ref)
  records.append({'source':path,'target':path,'transformation':'Texture paths flattened to singular block/item atlas directories; standard minecraft:block/block display transforms inherited; original elements and UVs retained'})
 for name,tex in [('arcane_stone','arcane_stone_1'),('arcane_brick','arcane_brick_stone')]:
  texture('thaumcraft:blocks/'+tex);block(name,{'parent':'minecraft:block/cube_all','textures':{'all':'thaumcraft:blocks/'+tex}})
 for name in ['tallow','alumentum']:
  texture('thaumcraft:items/'+name);js(f'assets/thaumcraft/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:items/'+name}})
 texture('thaumcraft:blocks/nitor');texture('thaumcraft:blocks/nitor_core')
 colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
 for color in colors:
  name='nitor_'+color;js(f'assets/thaumcraft/particles/{name}.json',{'textures':['thaumcraft:legacy']});block(name,{'parent':'minecraft:block/tinted_cross','render_type':'minecraft:cutout','textures':{'cross':'thaumcraft:blocks/nitor'}})
  js(f'assets/thaumcraft/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:blocks/nitor'}})
  js(f'data/thaumcraft/recipe/{name}_dye.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'tag':'thaumcraft:nitor'},{'item':f'minecraft:{color}_dye'}],'result':{'id':f'thaumcraft:{name}'}})
 js('data/thaumcraft/tags/item/nitor.json',{'values':['thaumcraft:nitor_'+c for c in colors]})
 js('data/thaumcraft/recipe/thaumometer.json',{'type':'thaumcraft:arcane','research':'FIRSTSTEPS@2','vis':20,'crystals':{s:1 for s in ['aer','terra','ignis','aqua','ordo','perditio']},'pattern':[' I ','IGI',' I '],'key':{'I':{'item':'minecraft:gold_ingot'},'G':{'item':'minecraft:glass_pane'}},'result':{'id':'thaumcraft:thaumometer'}})
 js('data/thaumcraft/recipe/arcane_stone.json',{'type':'minecraft:crafting_shaped','pattern':['KKK','KCK','KKK'],'key':{'K':{'item':'minecraft:stone'},'C':{'item':'thaumcraft:crystal_essence'}},'result':{'id':'thaumcraft:arcane_stone','count':9}})
 js('data/thaumcraft/recipe/arcane_brick.json',{'type':'minecraft:crafting_shaped','pattern':['KK','KK'],'key':{'K':{'item':'thaumcraft:arcane_stone'}},'result':{'id':'thaumcraft:arcane_brick','count':4}})
 recipes=[
 ('nitor','UNLOCKALCHEMY@3','minecraft:glowstone_dust','thaumcraft:nitor_yellow',{'potentia':10,'ignis':10,'lux':10}),
 ('alumentum','ALUMENTUM','#minecraft:coals','thaumcraft:alumentum',{'potentia':10,'ignis':10,'perditio':5}),
 ('brassingot','METALLURGY@1','minecraft:iron_ingot','thaumcraft:ingot_brass',{'instrumentum':5}),
 ('thaumiumingot','METALLURGY@2','minecraft:iron_ingot','thaumcraft:ingot_thaumium',{'praecantatio':5,'terra':5}),
 ('voidingot','BASEELDRITCH','thaumcraft:void_seed','thaumcraft:ingot_void',{'metallum':10,'vitium':5}),
 ('hedge_tallow','HEDGEALCHEMY@1','minecraft:rotten_flesh','thaumcraft:tallow',{'ignis':1}),
 ('hedge_leather','HEDGEALCHEMY@1','minecraft:rotten_flesh','minecraft:leather',{'aer':3,'bestia':3})]
 for metal in ['iron','gold','copper','cinnabar']:
  recipes.append(('metal_purification_'+metal,'METALPURIFICATION',('thaumcraft:ore_cinnabar' if metal=='cinnabar' else f'#c:ores/{metal}'),f'thaumcraft:cluster_{metal}',{'metallum':5,'ordo':5}))
 for name,research,catalyst,output,aspects in recipes:
  js(f'data/thaumcraft/recipe/crucible_{name}.json',{'type':'thaumcraft:crucible','research':research,'catalyst':{'tag' if catalyst.startswith('#') else 'item':catalyst.lstrip('#')},'aspects':aspects,'result':{'id':output}})
 for lang in ['en_us','ru_ru']:
  path=res/f'assets/thaumcraft/lang/{lang}.json';data=json.loads(path.read_text());ru=lang=='ru_ru'
  data.update({'block.thaumcraft.arcane_workbench':'Магический верстак' if ru else 'Arcane Workbench','block.thaumcraft.crucible':'Тигель' if ru else 'Crucible','block.thaumcraft.arcane_stone':'Магический камень' if ru else 'Arcane Stone','block.thaumcraft.arcane_brick':'Магические кирпичи' if ru else 'Arcane Stone Bricks','item.thaumcraft.tallow':'Сальный воск' if ru else 'Tallow','item.thaumcraft.alumentum':'Алюментум' if ru else 'Alumentum'})
  for color in colors:data['block.thaumcraft.nitor_'+color]=('Нитор' if ru else 'Nitor')+' ('+color+')'
  path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
(root/'reference/machine-provenance.json').write_text(json.dumps({'source_sha256':hashlib.sha256(a.jar.read_bytes()).hexdigest(),'assets':records,'recipes':'11 original crucible recipes; original thaumometer recipe; arcane stone/bricks; 16 dye variants'},indent=2)+'\n')
print('Imported',len(records),'machine asset records and',len(recipes),'crucible recipes')
