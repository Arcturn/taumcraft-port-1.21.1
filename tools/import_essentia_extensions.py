#!/usr/bin/env python3
"""Run after import_essentia.py. Retain TC6 textures, geometry, recipe costs and provenance."""
import argparse,hashlib,json,pathlib,zipfile
p=argparse.ArgumentParser();p.add_argument('--jar',type=pathlib.Path,required=True);args=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];res=root/'src/main/resources';records=[]
def js(path,value):
 out=res/path;out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(value,ensure_ascii=False,indent=2).replace('thaumcraft:blocks/','thaumcraft:block/').replace('thaumcraft:items/','thaumcraft:item/')+'\n')
def loot(name,item=None):js(f'data/thaumcraft/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'thaumcraft:'+(item or name)}]}]})
def block(name,model=None):
 js(f'assets/thaumcraft/blockstates/{name}.json',{'variants':{'':{'model':'thaumcraft:block/'+(model or name)}}});js(f'assets/thaumcraft/models/item/{name}.json',{'parent':'thaumcraft:block/'+(model or name)});loot(name)
def ing(v):return {'tag' if v.startswith('#') else 'item':v.lstrip('#') if ':' in v else 'thaumcraft:'+v}
def recipe(name,research,vis,crystals,pattern,key):js(f'data/thaumcraft/recipe/{name}.json',{'type':'thaumcraft:arcane','research':research,'vis':vis,'crystals':crystals,'pattern':pattern,'key':{k:ing(v) for k,v in key.items()},'result':{'id':'thaumcraft:'+name}})
with zipfile.ZipFile(args.jar) as z:
 def copy(path):
  target=path.replace('/textures/blocks/','/textures/block/').replace('/textures/items/','/textures/item/');data=z.read(path);out=res/target;out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(data);records.append({'source':path,'target':target,'sha256':hashlib.sha256(data).hexdigest()})
 def model(name):
  path=f'assets/thaumcraft/models/block/{name}.json';data=json.loads(z.read(path));data['render_type']='minecraft:cutout'
  for ref in set(data.get('textures',{}).values()):
   if ref.startswith('thaumcraft:'):copy('assets/thaumcraft/textures/'+ref.split(':')[1]+'.png')
  js(path,data);records.append({'source':path,'target':path,'transformation':'Original geometry/UVs; singular texture paths and modern cutout render layer'})
 for name in ['tube_buffer_core','bellows','smelter_aux','smelter_vent','brain_box']:model(name)
 block('tube_buffer','tube_buffer_core');parts=[{'apply':{'model':'thaumcraft:block/tube_buffer_core'}}]
 orientations={'down':{},'up':{'x':180},'north':{'x':270},'south':{'x':90},'west':{'x':90,'y':90},'east':{'x':90,'y':270}}
 for d,angles in orientations.items():parts.append({'when':{d:'true'},'apply':{'model':'thaumcraft:block/tube_side',**angles}})
 js('assets/thaumcraft/blockstates/tube_buffer.json',{'multipart':parts})
 for name in ['bellows','smelter_aux','smelter_vent','brain_box']:
  block(name)
  # Reuse the original explicit facing transforms instead of guessing the source model's axis.
  old=json.loads(z.read(f'assets/thaumcraft/blockstates/{name}.json'))
  variants={k:{'model':'thaumcraft:block/'+name,**{axis:v for axis,v in value.items() if axis in ('x','y')}} for k,value in old['variants'].items() if k.startswith('facing=') and (name not in ['smelter_aux','smelter_vent'] or k not in ['facing=up','facing=down'])}
  if variants:js(f'assets/thaumcraft/blockstates/{name}.json',{'variants':variants})
 copy('assets/thaumcraft/models/block/thaumatorium.obj');copy('assets/thaumcraft/textures/blocks/thaumatorium.png')
 path='assets/thaumcraft/models/block/thaumatorium.mtl';mtl='\n'.join(('map_Kd thaumcraft:block/thaumatorium' if line.strip().startswith('map_Kd ') else 'map_Ka thaumcraft:block/thaumatorium' if line.strip().startswith('map_Ka ') else line) for line in z.read(path).decode().splitlines())+'\n';(res/path).write_text(mtl);records.append({'source':path,'target':path,'transformation':'Modern singular atlas paths; texture extension removed'})
 js('assets/thaumcraft/models/block/thaumatorium.json',{'loader':'neoforge:obj','model':'thaumcraft:models/block/thaumatorium.obj','flip_v':True,'textures':{'texture':'thaumcraft:block/thaumatorium','particle':'thaumcraft:block/thaumatorium'}})
 js('assets/thaumcraft/models/block/thaumatorium_top.json',{'textures':{'particle':'thaumcraft:block/thaumatorium'},'elements':[]})
 block('thaumatorium');block('thaumatorium_top');loot('thaumatorium','metal_alchemical');loot('thaumatorium_top','metal_alchemical')
 old=json.loads(z.read('assets/thaumcraft/blockstates/thaumatorium.json'))
 js('assets/thaumcraft/blockstates/thaumatorium.json',{'variants':{k:{'model':'thaumcraft:block/thaumatorium',**{a:v for a,v in value.items() if a in ('x','y')}} for k,value in old['variants'].items() if k in ['facing=north','facing=south','facing=east','facing=west']}})
 # The upper half has no independent model/item geometry in TC6; use the original construct in inventory.
 js('assets/thaumcraft/models/item/thaumatorium_top.json',{'parent':'thaumcraft:block/metal_alchemical'})
 copy('assets/thaumcraft/textures/items/mind_clockwork.png');js('assets/thaumcraft/models/item/mind_clockwork.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:item/mind_clockwork'}})
 recipe('tube_buffer','TUBES',25,{},['PVP','TWT','PRP'],{'P':'phial','V':'tube_valve','T':'tube','W':'plate_iron','R':'tube_restrict'})
 recipe('bellows','BELLOWS',25,{'aer':1},['WW ','LLI','WW '],{'W':'#minecraft:planks','L':'minecraft:leather','I':'minecraft:iron_ingot'})
 recipe('smelter_aux','IMPROVEDSMELTING',100,{'aer':1,'terra':1},['WTW','RGR','IBI'],{'W':'plank_greatwood','T':'tube_filter','R':'plate_brass','G':'metal_alchemical','I':'plate_iron','B':'bellows'})
 recipe('smelter_vent','IMPROVEDSMELTING2',150,{'aer':1},['IBI','MGF','IBI'],{'I':'plate_iron','B':'plate_brass','M':'filter','F':'filter','G':'metal_alchemical'})
 recipe('brain_box','THAUMATORIUM',50,{'terra':1,'ordo':1},['IAI','ABA','IAI'],{'I':'plate_iron','A':'amber','B':'mind_clockwork'})
 copy('assets/thaumcraft/textures/items/mind_clockwork.png.mcmeta')
 recipe('mind_clockwork','MINDCLOCKWORK@2',25,{'ignis':1,'ordo':1},[' P ','PGP','BCB'],{'P':'minecraft:glass_pane','G':'mechanism_simple','B':'plate_brass','C':'minecraft:comparator'})
 names={'tube_buffer':('Essentia Buffer','Буфер эссенции'),'bellows':('Arcane Bellows','Магические мехи'),'smelter_aux':('Auxiliary Slurry Pump','Дополнительный насос'),'smelter_vent':('Smeltery Vent','Вентиляция алхимической печи'),'brain_box':('Mnemonic Matrix','Мнемоническая матрица'),'thaumatorium':('Thaumatorium','Тауматориум'),'thaumatorium_top':('Thaumatorium Upper Half','Верхняя часть тауматориума')}
 for lang in ['en_us','ru_ru']:
  path=res/f'assets/thaumcraft/lang/{lang}.json';data=json.loads(path.read_text());ru=lang=='ru_ru'
  for k,v in names.items():data['block.thaumcraft.'+k]=v[ru]
  data['item.thaumcraft.mind_clockwork']='Механический разум' if ru else 'Clockwork Mind';data['gui.thaumcraft.heated']='Нагрет' if ru else 'Heated';data['gui.thaumcraft.cold']='Нет нагрева' if ru else 'No heat';data['gui.thaumcraft.thaumatorium_research']='Нужно исследование «Тауматориум»' if ru else 'Thaumatorium research required'
  path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
(root/'reference/essentia-extensions-provenance.json').write_text(json.dumps({'source_sha256':hashlib.sha256(args.jar.read_bytes()).hexdigest(),'assets':records,'notes':['Thaumatorium uses the original OBJ/UVs through NeoForge OBJ loader. GUI is a modern server-synchronized layout.','Bellows moving parts use original ModelBellows dimensions and UV offsets. Buffer choke markers use colored original label texture.','Vanilla furnace acceleration and bellows-to-buffer nozzle visual are not yet ported.']},indent=2)+'\n')
print('Imported',len(records),'essentia extension asset records')
