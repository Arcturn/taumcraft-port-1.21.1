#!/usr/bin/env python3
"""Source/media/catalog integrity only; this is deliberately not a GPU parity test."""
import pathlib,json,hashlib,re,sys,zipfile
from PIL import Image
root=pathlib.Path(__file__).resolve().parents[1];res=root/'src/main/resources';assets=res/'assets/thaumcraft';read=lambda p:json.loads(p.read_text())
provenance=read(root/'reference/research-presentation-provenance.json')
for a in provenance['assets']:assert hashlib.sha256((res/a['target']).read_bytes()).hexdigest()==a['sha256'],a['target']
assert len(provenance['converted_backgrounds'])==7
for a in provenance['converted_backgrounds']:
 assert hashlib.sha256((res/a['target']).read_bytes()).hexdigest()==a['sha256'],a['target']
 with Image.open(res/a['source']) as original,Image.open(res/a['target']) as png:
  assert original.size==png.size and original.convert('RGB').tobytes()==png.convert('RGB').tobytes(),a['target']
print('PASS: 7 PNG backgrounds preserve every decoded RGB pixel of the original JPG')
entries=[e for p in (assets/'research').glob('*.json') for e in read(p)['entries']];models=read(res/'thaumcraft-port/book-model-index.json');recipes=read(res/'thaumcraft-port/book-recipes.json')['recipes'];report=read(root/'reference/book-recipe-import.json')
assert len(entries)==148
assert report['unresolved_references']==['thaumcraft:JarLabelEssence','thaumcraft:arcane_brick','thaumcraft:arcane_stone','thaumcraft:nitorcolor']
refs={r for e in entries for s in e.get('stages',[])+e.get('addenda',[]) for r in s.get('recipes',[])}
resolved={k.lower() for k in recipes}|{r['book_group'].lower() for r in recipes.values() if 'book_group' in r}
assert all(r.lower() in resolved or r in report['unresolved_references'] for r in refs)
for e in entries:
 for source in e.get('icons',[]):
  if source.startswith('focus:'):
   name=source.split('.')[-1].lower().replace('splittarget','split_target').replace('splittrajectory','split_trajectory');assert (assets/f'textures/foci/{name}.png').is_file(),source
  elif 'textures/' in source:assert (res/'assets'/source.replace(':','/')).is_file(),source
  elif source.startswith('thaumcraft:'):
   parts=source.split(';');name=parts[0][11:];meta=int(parts[2]) if len(parts)>2 else 0
   if name in ['seal','seals']:name='seal_blank'
   assert name+'_'+str(meta) in models or (assets/f'models/item/{name}.json').is_file(),source
for value in models.values():assert (assets/'models'/f'{value[11:]}.json').is_file()
scanner=read(assets/'models/item/thaumometer.json');d=scanner['display']
assert d['firstperson_righthand']['translation']==[-.96,.096,-6.4]
assert d['firstperson_righthand']['scale']==[1,1,1]
assert d['thirdperson_righthand']['rotation']==[90,0,-90]
assert (assets/'textures/item/scanscreen.png.mcmeta').is_file()
for r in recipes.values():
 if r.get('type')=='thaumcraft:infusion':assert 'input' in r and 'aspects' in r and 'ingredients' in r
print(f'PASS: {len(provenance["assets"])} original presentation hashes; all 148 research icon sets resolve; {len(models)} standalone models; {report["original_recipe_count"]} original recipe variants; all {len(refs)} book references accounted for (4 stale original IDs documented)')
print('PASS: scanner display conversion and animated lens; visual in-game comparison remains pending')
