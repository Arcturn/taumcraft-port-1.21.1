#!/usr/bin/env python3
"""Import original crystal geometry; repair 1.21 rotations and Nitor's particle-only world model."""
from pathlib import Path
import argparse,hashlib,json,zipfile
p=argparse.ArgumentParser();p.add_argument('--jar',type=Path,required=True);args=p.parse_args();root=Path(__file__).resolve().parents[1];res=root/'src/main/resources'
sha=hashlib.sha256(args.jar.read_bytes()).hexdigest();assert sha=='9425f8643581b27ff8845b087c8bc6fc10425a32942f1a3f0e265ce6b38f7b5f'
def write(path,data):
 p=res/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
records=[]
with zipfile.ZipFile(args.jar) as z:
 path='assets/thaumcraft/models/obj/crystal.obj';data=z.read(path);(res/path).write_bytes(data);records.append({'source':path,'target':path,'sha256':hashlib.sha256(data).hexdigest()})
 path='assets/thaumcraft/models/obj/crystal.mtl';original=z.read(path).decode();lines=[]
 for line in original.splitlines():
  if line.strip().startswith(('map_Ka ','map_Kd ')):line='map_Kd thaumcraft:block/crystal' if line.strip().startswith('map_Kd ') else 'map_Ka thaumcraft:block/crystal'
  # MeshPart in TC6 ignores MTL diffuse/ambient values; do not darken every shard by 0.588.
  if line.strip().startswith('Kd '):line='Kd 1.0 1.0 1.0'
  lines.append(line)
 lines.append('neoforge_TintIndex 0');(res/path).write_text('\n'.join(lines)+'\n');records.append({'source':path,'target':path,'transformation':'Modern atlas path, tint index 0; white material matches TC6 MeshPart vertex colors'})
 for i in range(4):write(f'assets/thaumcraft/models/block/crystal_cluster_{i}.json',{'loader':'thaumcraft:crystal_cluster','model':'thaumcraft:models/obj/crystal.obj','automatic_culling':False,'shade_quads':True,'flip_v':True,'emissive_ambient':False,'ambientocclusion':True,'render_type':'minecraft:cutout','textures':{'particle':'thaumcraft:block/crystal'}})
 for aspect in ['aer','ignis','aqua','terra','ordo','perditio','vitium']:write(f'assets/thaumcraft/blockstates/crystal_{aspect}.json',{'variants':{'':{'model':'thaumcraft:block/crystal_cluster_3'}}})
 for path in (res/'assets/thaumcraft/models/block').glob('nitor_*.json'):write(path.relative_to(res),{'textures':{'particle':'thaumcraft:block/nitor'},'elements':[]})
 item=json.loads(z.read('assets/thaumcraft/models/item/nitor.json'));item['textures']={k:v.replace(':blocks/',':block/') for k,v in item['textures'].items()}
 for path in (res/'assets/thaumcraft/models/item').glob('nitor_*.json'):write(path.relative_to(res),item)
write('assets/thaumcraft/particles/nitor_core.json',{'textures':['thaumcraft:legacy']})
angles={'down':{},'up':{'x':180},'north':{'x':270},'south':{'x':90},'west':{'x':90,'y':90},'east':{'x':90,'y':270}}
for path in (res/'assets/thaumcraft/blockstates').glob('tube*.json'):
 doc=json.loads(path.read_text())
 for part in doc.get('multipart',[]):
  when=part.get('when',{});face=when.get('facing') or next((d for d in angles if when.get(d)=='true'),None)
  if face:
   part['apply'].pop('x',None);part['apply'].pop('y',None);part['apply'].update(angles[face])
 write(path.relative_to(res),doc)
(root/'reference/visual-fixes-provenance.json').write_text(json.dumps({'source_sha256':sha,'assets':records,'notes':['Original eight crystal mesh parts and UVs, no generated cuboid substitute.','Nitor world model is empty as in TC6; every-client-tick flames and original white core use the original particle sheet.','Minecraft BlockModelRotation uses negative X/Y angles; face metadata now matches transformed geometry.','Screens render their background once, before content; Screen.render would blur custom content a second time.']},ensure_ascii=False,indent=2)+'\n')
print('Imported original crystal OBJ; fixed six tube directions and Nitor block/item models')
