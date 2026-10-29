#!/usr/bin/env python3
"""Import original jar-brace, jar fitting and bellows-nozzle media after the earlier importers."""
import argparse,hashlib,json,pathlib,zipfile
p=argparse.ArgumentParser();p.add_argument('--jar',required=True,type=pathlib.Path);args=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];res=root/'src/main/resources';records=[]
source_hash=hashlib.sha256(args.jar.read_bytes()).hexdigest()
assert source_hash=='9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f','Unexpected TC6 source version'
def js(path,data):
 out=res/path;out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
with zipfile.ZipFile(args.jar) as z:
 for source in ['assets/thaumcraft/textures/items/jar_brace.png','assets/thaumcraft/textures/models/jarbrine.png','assets/thaumcraft/textures/models/bore.png']:
  data=z.read(source);target=source.replace('/items/','/item/');out=res/target;out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(data);records.append({'source':source,'target':target,'sha256':hashlib.sha256(data).hexdigest()})
 path='assets/thaumcraft/models/item/jar_brace.json';data=json.loads(z.read(path));data['textures']={k:v.replace('thaumcraft:items/','thaumcraft:item/') for k,v in data['textures'].items()};js(path,data);records.append({'source':path,'target':path,'transformation':'Original item model; singular texture path'})
js('data/thaumcraft/recipe/jar_brace.json',{'type':'minecraft:crafting_shaped','pattern':['NSN','S S','NSN'],'key':{'N':{'item':'thaumcraft:nugget_brass'},'S':{'tag':'c:rods/wooden'}},'result':{'id':'thaumcraft:jar_brace','count':2}})
for lang in ['en_us','ru_ru']:
 path=res/f'assets/thaumcraft/lang/{lang}.json';d=json.loads(path.read_text());ru=lang=='ru_ru'
 d['item.thaumcraft.jar_brace']='Латунная распорка' if ru else 'Brass Lid Brace'
 d['tooltip.thaumcraft.jar_contents']='%s: %s / 250'
 d['tooltip.thaumcraft.jar_filter']='Бирка: %s' if ru else 'Label: %s'
 d['tooltip.thaumcraft.jar_brace']='Блокирует дистанционный обмен; трубы и колбы работают' if ru else 'Blocks remote transfers; pipes and phials still work'
 path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
(root/'reference/essentia-refinements-provenance.json').write_text(json.dumps({'source_sha256':source_hash,'assets':records,'mechanic_sources':['TileJarFillable.java:isBlocked','BlockJar.java:spawnFilledJar/onBlockActivated','BlockJarItem.java:onItemUseFirst','TileBellows.java:update','ModelJar.java','ModelBoreBase.java:Nozzle1/Nozzle2'],'notes':['Jar braces block remote source access, not pipe/phial access.','Braces drop separately when a jar drops; portable jar data retains contents and filter.','Modern furnace duration replaces hardcoded 199; vanilla completes outputs and records XP.']},indent=2)+'\n')
print('Imported',len(records),'refinement asset records')
