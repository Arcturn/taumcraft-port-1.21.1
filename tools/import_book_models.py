#!/usr/bin/env python3
"""Bakeable, display-only copies of original item/block models. No gameplay registration."""
import json,pathlib,re,zipfile,argparse,copy
p=argparse.ArgumentParser();p.add_argument('--jar',type=pathlib.Path,required=True);a=p.parse_args();root=pathlib.Path(__file__).resolve().parents[1];res=root/'src/main/resources';assets=res/'assets/thaumcraft';j=json.loads((res/'thaumcraft-port/legacy-book-models.json').read_text());models=j['models'];states=j['blockstates'];z=zipfile.ZipFile(a.jar);done={};index={};missing=[];transforms={}
variants={'ingot':'thaumium,void,brass','plate':'brass,iron,thaumium,void','mind':'clockwork,biothaumic','nugget':'iron,copper,tin,silver,lead,quicksilver,thaumium,void,brass,quartz,rareearth','cluster':'iron,gold,copper,tin,silver,lead,cinnabar,quartz','thaumonomicon':'normal,cheat','baubles':'amulet_mundane,ring_mundane,girdle_mundane,ring_apprentice,amulet_fancy,ring_fancy,girdle_fancy','module':'vision,aggression','turret':'basic,advanced,bore','curio':'arcane,preserved,ancient,eldritch,knowledge,twisted,rites','amulet_vis':'found,crafted','label':'blank,filled','phial':'empty,filled'}
variants['chunk']='beef,chicken,pork,fish,rabbit,mutton'

def gui_transform(transform):
 """ForgeBlockStateV1 TRSR -> modern display.gui, for these GUI-only models.

 Forge 1.12 Transforms.convert divides its predefined translation by 16;
 custom TRSR translations already use block units. Both rotate about the
 model center. Custom variant states replace, rather than merge, defaults.
 Other display contexts in the copied item/block JSON are left untouched.
 """
 identity={'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]}
 if transform is None:return None
 if transform=='forge:default-block':return {'rotation':[30,225,0],'translation':[0,0,0],'scale':[.625,.625,.625]}
 if transform in ('identity','forge:default-item','forge:default-tool'):return identity
 if not isinstance(transform,dict):raise ValueError('Unsupported Forge transform '+str(transform))
 gui=transform.get('gui',{})
 if set(gui)-{'rotation','translation','scale'}:raise ValueError('Unsupported GUI TRSR '+str(gui))
 rotation=gui.get('rotation',[0,0,0])
 if isinstance(rotation,dict):rotation=[rotation]
 if rotation and isinstance(rotation[0],dict):
  # In BETA26 all GUI rotations occur in XYZ order; preserve their original
  # angles exactly instead of introducing a lossy Euler/quaternion round trip.
  axes=[next(iter(axis)) for axis in rotation]
  if any(len(axis)!=1 for axis in rotation) or axes!=sorted(set(axes),key='xyz'.index):
   raise ValueError('GUI rotation requires ordered quaternion conversion: '+str(rotation))
  rotation=[sum(axis.get(name,0) for axis in rotation) for name in 'xyz']
 if not isinstance(rotation,list) or len(rotation)!=3:raise ValueError('Unsupported GUI rotation '+str(rotation))
 translation=[round(value*16,8) for value in gui.get('translation',[0,0,0])]
 scale=gui.get('scale',1);scale=scale if isinstance(scale,list) else [scale]*3
 if len(translation)!=3 or len(scale)!=3:raise ValueError('Invalid GUI vector '+str(gui))
 return {'rotation':rotation,'translation':translation,'scale':scale}

def write(path,obj):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,indent=2)+'\n')
def tex(value):
 if value.startswith('blocks/') or value.startswith('minecraft:blocks/'):
  n=value.split('blocks/')[1];n={'redstone_torch_off':'redstone_torch_off','stone_slab_top':'smooth_stone','fire_layer_0':'fire_0'}.get(n,n);return 'minecraft:block/'+n
 return value
def parent(value):
 if value.startswith('thaumcraft:'):
  name=value[11:];name=name if '/' in name else 'block/'+name;return original(name)
 if value.startswith('forge:'):return 'minecraft:item/generated'
 if ':' not in value:value='minecraft:'+value
 if value.startswith('minecraft:') and '/' not in value:value='minecraft:block/'+value[10:]
 return value.replace('minecraft:block/half_slab','minecraft:block/slab').replace('minecraft:block/upper_slab','minecraft:block/slab_top')

def original(name):
 target='research/legacy/'+name
 if name in done:return done[name]
 if name not in models:raise ValueError('model '+name)
 done[name]='thaumcraft:'+target;out=copy.deepcopy(models[name]);out.pop('overrides',None)
 # Original caster has only overrides: use its empty-focus model.
 if models[name].get('overrides') and 'textures' not in out and 'elements' not in out:
  done[name]=parent(models[name]['overrides'][0]['model']);return done[name]
 if 'parent' in out:out['parent']=parent(out['parent'])
 if 'textures' in out:out['textures']={k:tex(v) for k,v in out['textures'].items()}
 write(assets/'models'/f'{target}.json',out);return 'thaumcraft:'+target

def geometry(out):
 m=out['model'];textures={k.lstrip('#'):tex(v) for k,v in out.get('textures',{}).items()};result={'textures':textures}
 if m.endswith('.obj'):
  obj=m.split(':')[-1];base='assets/thaumcraft/models/';base=next((base+p for p in ['','block/','item/','obj/'] if base+p+obj in z.namelist()),base);data=z.read(base+obj);dest=assets/'models/research/obj'/obj;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data)
  for mtl in re.findall(rb'mtllib\s+(\S+)',data):
   n=mtl.decode();lines=[];material=None
   for line in z.read(base+n).decode().splitlines():
    fields=line.strip().split(maxsplit=1)
    if fields and fields[0]=='newmtl':material=fields[1]
    if fields and fields[0] in ('map_Kd','map_Ka'):
     # Forge 1.12 replaced a material by its name when blockstate textures
     # supplied #name. NeoForge resolves a material's texture string instead:
     # keep that binding explicit, especially for the three pillar textures.
     if material in textures:value='#'+material
     else:
      value=fields[1].strip().replace('\\','/')
      match=re.search(r'(?:^|/)assets/([^/]+)/textures/(.+?)(?:\.png)?$',value)
      if match:value=match[1]+':'+match[2]
      value=tex(value.removesuffix('.png'))
      if not value.startswith('#') and not re.fullmatch(r'[a-z0-9_.-]+:[a-z0-9/._-]+',value):raise ValueError('Invalid OBJ material texture '+value)
     line=fields[0]+' '+value
    lines.append(line)
   (dest.parent/n).write_text('\n'.join(lines)+'\n')
  # Both Forge 1.12 OBJModel and NeoForge ObjModel default to unflipped V.
  # Only the scanner opts into flip-v; flipping every OBJ samples unused
  # magenta padding in the original golem builder texture.
  result.update({'loader':'neoforge:obj','model':'thaumcraft:models/research/obj/'+obj,'flip_v':out.get('custom',{}).get('flip-v',False),'automatic_culling':False,'parent':'minecraft:block/block'})
 elif m=='forge:item-layer':result['parent']='minecraft:item/generated'
 else:result['parent']=parent(m)
 return result

def model(name,meta):
 if name=='verdant_charm' and meta in (1,2):return {'parent':original('item/verdant_charm_'+str(meta))}
 # ProxyBlock maps every nitor colour to the original flat item/nitor model.
 # Its world block/empty JSON must never replace that inventory artwork.
 if name.startswith('nitor_'):
  item=name.replace('nitor_silver','nitor_light_gray').replace('nitor_lightblue','nitor_light_blue')
  assert (assets/'models/item'/f'{item}.json').is_file(),item
  return {'parent':'thaumcraft:item/'+item}
 name=name.replace('light_blue','lightblue').replace('light_gray','silver')
 if name=='metal':name={2:'metal_brass',3:'metal_thaumium'}.get(meta,name)
 if name=='arcane_stone':name='stone_arcane'
 if 'item/'+name in models:return {'parent':original('item/'+name)}
 if name not in states:raise ValueError('state '+name)
 state=states[name];out=copy.deepcopy(state.get('defaults',{}));vs=state.get('variants',{});allv=variants.get(name,'').split(',');v=allv[meta] if 0<=meta<len(allv) else ''
 if 'half' in vs and 'variant' in vs:
  out.update(vs['variant'].get('default',{}));entry=vs['half']['bottom']
 else:
  # ItemTCBase without metadata registers #normal. Some assets (thaumometer)
  # deliberately contain an empty inventory list, which is not a real model.
  entry=next((vs[key] for key in [v,'inventory','normal'] if key in vs and vs[key]!=[]),next(iter(vs.values()),{}))
 if isinstance(entry,list):entry=entry[0] if entry else {}
 out.update(entry);result=geometry(out)
 gui=gui_transform(out.get('transform'))
 if gui is not None:
  result['display']={'gui':gui}
  transforms[name+'_'+str(meta)]={'source':out['transform'],'gui':gui}
 return result
sources=set()
for f in (assets/'research').glob('*.json'):
 for e in json.loads(f.read_text())['entries']:
  sources.update(e.get('icons',[]))
  for st in e.get('stages',[]):sources.update(st.get('required_item',[])+st.get('required_craft',[]))
def collect(o):
 if isinstance(o,dict):
  for k,v in o.items():
   if k=='legacy' and isinstance(v,str):sources.add(v)
   else:collect(v)
 elif isinstance(o,list):
  for x in o:collect(x)
collect(json.loads((res/'thaumcraft-port/book-recipes.json').read_text()))
ingredient_variants=res/'thaumcraft-port/book-ingredient-variants.json'
if ingredient_variants.exists():
 for table in json.loads(ingredient_variants.read_text()).values():
  for alternatives in table.values():sources.update(alternatives)
for source in sorted(sources):
 if not source.startswith('thaumcraft:') or 'textures/' in source:continue
 ps=source.split(';');name=ps[0][11:];meta=int(ps[2]) if len(ps)>2 else 0
 if meta==32767:meta=0
 if name=='verdant_charm' and len(ps)>3:
  charm_type=re.search(r'\btype\s*:\s*(-?\d+)',ps[3]);meta=max(0,min(2,int(charm_type[1]))) if charm_type else 0
 key=name+'_'+str(meta)
 if name in ['seal','seals']:
  name='seal_'+(ps[3].split(':')[-1] if len(ps)>3 else 'blank');key=name+'_0'
 if key in index:continue
 try:out=model(name,meta);write(assets/'models/research/icons'/f'{key}.json',out);index[key]='thaumcraft:research/icons/'+key
 except (ValueError,KeyError) as ex:missing.append({'icon':source,'reason':str(ex)})
write(res/'thaumcraft-port/book-model-index.json',index)

# Blueprint cells require world geometry; an inventory sprite is never a valid
# substitute. These are the TC6 source/target cells used by the six blueprints.
world_blocks={name:[0] for name in ['infusion_matrix','stone_arcane','stone_ancient','stone_eldritch_tile',
 'pedestal_arcane','metal_alchemical','crucible','table_stone','infernal_furnace','thaumatorium','golem_builder']}
world_blocks.update({'pedestal_ancient':[2],'pedestal_eldritch':[1]})
world_blocks.update({name:[0,1,2,3] for name in ['pillar_arcane','pillar_ancient','pillar_eldritch']})
world_index={}
for name,metas in world_blocks.items():
 for meta in metas:
  state=states[name];out=copy.deepcopy(state.get('defaults',{}));vs=state.get('variants',{})
  # BlockPillar uses EnumFacing.getHorizontalIndex/getHorizontal, not the
  # six-direction index used by BlockTCDevice and vanilla pistons.
  if name.startswith('pillar_'):entry=vs['facing='+{0:'south',1:'west',2:'north',3:'east'}[meta]]
  elif name in ['infernal_furnace','thaumatorium']:entry=vs['facing=north']
  elif name.startswith('pedestal_'):entry=vs['charge'][str(meta)]
  else:entry=vs.get('normal',{})
  entries=entry if isinstance(entry,list) else [entry]
  entries=entries or [{}]
  # GolemBuilder is INVISIBLE + TESR in TC6. Its original complete OBJ is the
  # static blueprint fallback; do not substitute the dummy base_stone blockstate.
  key=name+'_'+str(meta);path='research/world/'+key
  selections=[];geometries={}
  for variant in entries:
   selected=copy.deepcopy(out);selected.update(variant);shape=geometry(selected)
   if name=='infernal_furnace':shape['render_type']='minecraft:cutout_mipped'
   fingerprint=json.dumps(shape,sort_keys=True)
   if fingerprint not in geometries:
    model_path=path if not geometries else path+'_variant_'+str(len(geometries))
    write(assets/'models'/f'{model_path}.json',shape);geometries[fingerprint]='thaumcraft:'+model_path
   selections.append({'model':geometries[fingerprint],'x':selected.get('x',0),'y':selected.get('y',0),'weight':selected.get('weight',1)})
  # WeightedBakedModel.Builder sorts by descending weight, with stable ties.
  # Keep every source rotation even when the model geometry is shared.
  selections.sort(key=lambda variant:-variant['weight'])
  world_index[key]={k:v for k,v in selections[0].items() if k!='weight'}
  if len(selections)>1:world_index[key]['variants']=selections
write(res/'thaumcraft-port/book-block-model-index.json',world_index)
for name in ['pillar_arcane','pillar_ancient','pillar_eldritch']:
 for invalid_meta in [4,5]:
  (assets/'models/research/world'/f'{name}_{invalid_meta}.json').unlink(missing_ok=True)

# BlueprintBlockAccess held the actual TC6 states, including invisible cells.
# These values describe Block/IBlockState semantics, NOT mesh bounds: several
# non-full decorative meshes deliberately retain a full selection/collision box.
# The client creates virtual states owned by an existing vanilla block so no
# gameplay block or registry entry is added merely to render a book diagram.
def context_state(source,opaque=False,full_cube=False,shape=None,collision=None,face_shape='undefined',light=0,invisible=False):
 shape=shape or [0,0,0,1,1,1]
 return {'source_class':source,'opaque':opaque,'full_cube':full_cube,'material_solid':True,
         'shape':shape,'collision':collision if collision is not None else [shape],
         'face_shape':face_shape,'light':light,'invisible':invisible}
world_states={}
for name in ['stone_arcane','stone_ancient','stone_eldritch_tile']:
 world_states[name]=context_state('thaumcraft.common.blocks.basic.BlockStoneTC',True,True,face_shape='solid')
world_states['metal_alchemical']=context_state('thaumcraft.common.blocks.basic.BlockMetalTC',True,True,face_shape='solid')
for name in ['pedestal_arcane','pedestal_ancient','pedestal_eldritch']:
 world_states[name]=context_state('thaumcraft.common.blocks.devices.BlockPedestal')
for name in ['pillar_arcane','pillar_ancient','pillar_eldritch']:
 world_states[name]=context_state('thaumcraft.common.blocks.basic.BlockPillar',shape=[0,0,0,1,.5,1],collision=[[0,0,0,1,2,1]])
world_states['infusion_matrix']=context_state('thaumcraft.common.blocks.crafting.BlockInfusionMatrix',invisible=True)
world_states['table_stone']=context_state('thaumcraft.common.blocks.basic.BlockTable')
world_states['crucible']=context_state('thaumcraft.common.blocks.crafting.BlockCrucible',face_shape='solid',collision=[
 [0,0,0,1,.3125,1],[0,0,0,1,1,.125],[0,0,.875,1,1,1],[.875,0,0,1,1,1],[0,0,0,.125,1,1]])
# Infernal furnace does NOT override vanilla isOpaqueCube/isFullCube, despite
# its half-height selection box and much larger rendered multiblock geometry.
world_states['infernal_furnace']=context_state('thaumcraft.common.blocks.devices.BlockInfernalFurnace',True,True,
 shape=[0,0,0,1,.5,1],face_shape='solid',light=13)
for name in ['thaumatorium','thaumatorium_top']:
 world_states[name]=context_state('thaumcraft.common.blocks.crafting.BlockThaumatorium',invisible=name.endswith('_top'))
world_states['golem_builder']=context_state('thaumcraft.common.blocks.crafting.BlockGolemBuilder',face_shape='solid',invisible=True)
for name in ['placeholder_brick','placeholder_obsidian','placeholder_bars','placeholder_anvil','placeholder_cauldron','placeholder_table']:
 world_states[name]=context_state('thaumcraft.common.blocks.misc.BlockPlaceholder',face_shape='solid',
  light=13 if name=='placeholder_cauldron' else 0,invisible=True)
write(res/'thaumcraft-port/book-block-states.json',world_states)
# Preserve the focus and animated-strip entries authored by the research-icon
# generator; rerunning this importer must not remove them from the atlas.
atlas_path=res/'assets/minecraft/atlases/blocks.json'
atlas=json.loads(atlas_path.read_text()) if atlas_path.exists() else {'sources':[]}
for source in [{'type':'minecraft:directory','source':s,'prefix':s+'/'} for s in ['items','blocks']]+[{'type':'minecraft:single','resource':r,'sprite':r} for r in ['thaumcraft:entity/armor/caster_advanced','thaumcraft:entity/armor/caster_basic','thaumcraft:models/chesthungry']]:
 if source not in atlas['sources']:atlas['sources'].append(source)
write(atlas_path,atlas)
gui_count=sum('display' in json.loads((assets/'models'/f'{value.split(":",1)[1]}.json').read_text()) for value in index.values())
write(root/'reference/book-model-import.json',{'models':len(index),'world_models':len(world_index),'world_context_states':len(world_states),'weighted_world_variants':sum(len(v.get('variants',[])) for v in world_index.values()),'unresolved':missing,'gui_model_count':gui_count,'gui_transforms':transforms,'world_limits':['Golem builder uses the original static OBJ; TESR moving parts and lava surface are not reproduced here.']});print('Book models',len(index),'GUI models',gui_count,'world models',len(world_index),'context states',len(world_states),'unresolved',len(missing))
