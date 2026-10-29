#!/usr/bin/env python3
"""Read original ConfigRecipes expressions into a presentation-only recipe catalog.
Unresolved expressions are reported, never silently turned into craftable content.
"""
import argparse,csv,json,pathlib,re
P=argparse.ArgumentParser();P.add_argument('--source',type=pathlib.Path,required=True);P.add_argument('--fields',type=pathlib.Path,required=True);a=P.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];res=root/'src/main/resources';src=a.source
fields={r['searge']:r['name'].lower() for r in csv.DictReader(a.fields.open())}
text=(src/'common/config/ConfigRecipes.java').read_text();allsource={p.stem:p.read_text() for p in src.rglob('*.java')}
items={};groups={};recipes={};unresolved=[]
colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','silver','cyan','purple','blue','brown','green','red','black']
aspects=dict(re.findall(r'\b(\w+)\("([a-z]+)",',(root/'src/main/java/thaumcraft/port/aspect/Aspect.java').read_text()))

def split(s):
 out=[];start=0;depth=0;quote=None;escape=False
 for i,c in enumerate(s):
  if quote:
   if escape:escape=False
   elif c=='\\':escape=True
   elif c==quote:quote=None
  elif c in '\"\'':quote=c
  elif c in '([{':depth+=1
  elif c in ')]}':depth-=1
  elif c==',' and depth==0:out.append(s[start:i].strip());start=i+1
 out.append(s[start:].strip());return out

def body(s):
 start=s.index('(');depth=1;quote=None;escape=False
 for i in range(start+1,len(s)):
  c=s[i]
  if quote:
   if escape:escape=False
   elif c=='\\':escape=True
   elif c==quote:quote=None
  elif c in '\"\'':quote=c
  elif c=='(':depth+=1
  elif c==')':
   depth-=1
   if depth==0:return split(s[start+1:i])
 raise ValueError(s)
def stripcast(s):return re.sub(r'^\((?:Item|Block|ItemStack|Object\[\]|ResourceLocation|IRecipe|Ingredient|ItemStack\[\]|FluidStack|Fluid)\)\s*','',s.strip())
for config in ['ConfigItems','ConfigBlocks']:
 for m in re.finditer(r'((?:ItemsTC|BlocksTC)\.\w+)\s*=\s*(.*?);',allsource[config]):
  expr=m[2];v=re.search(r'new \w+\("([a-z0-9_]+)"',expr)
  if not v:v=re.search(r'"([a-z0-9_]+)"',expr)
  if not v:
   cls=re.search(r'new (\w+)\(',expr)
   if cls and cls[1] in allsource:v=re.search(r'(?:super|setRegistryName)\([^;]*?"([a-z0-9_]+)"',allsource[cls[1]])
  direct=re.search(r'setRegistryName\("thaumcraft", "([^"]+)"',expr)
  if direct:v=direct
  if v:
   name=v[1]
   if name=='thaumcraft' and cls and cls[1] in allsource:
    actual=re.search(r'setRegistryName\("thaumcraft", "([^"]+)"',allsource[cls[1]])
    if actual:name=actual[1]
   items[m[1]]='thaumcraft:'+name
for m in re.finditer(r'ResourceLocation\s+(\w+)\s*=\s*new ResourceLocation\((.*?)\);',text):
 parts=split(m[2]);groups[m[1]]=':'.join(json.loads(x) for x in parts)
vanilla_alias={'potionitem':'potion','slime_ball':'slime_ball','reeds':'sugar_cane','speckled_melon':'glistering_melon_slice','fireworks':'firework_rocket','firework_charge':'firework_star','quartz_ore':'nether_quartz_ore','golden_rail':'powered_rail','snow_layer':'snow','hardened_clay':'terracotta','noteblock':'note_block','web':'cobweb','wooden_button':'oak_button','wooden_pressure_plate':'oak_pressure_plate','wooden_door':'oak_door','stone_slab':'smooth_stone_slab','stonebrick':'stone_bricks','brick_block':'bricks','lit_pumpkin':'jack_o_lantern','red_flower':'poppy','waterlily':'lily_pad','deadbush':'dead_bush','grass':'grass_block','melon_block':'melon','melon':'melon_slice','planks':'oak_planks','log':'oak_log','wooden_slab':'oak_slab','redstone_torch':'redstone_torch','boat':'oak_boat','sign':'oak_sign'}
def literal(s,env):
 s=stripcast(s)
 if s in env:return env[s]
 if s in groups:return groups[s]
 if s.startswith('new ResourceLocation('):return ':'.join(str(literal(x,env)) for x in body(s))
 if s.startswith('"') and s.endswith('"') and len(split(s))==1:
  try:return json.loads(s)
  except:pass
 if re.fullmatch(r'-?\d+',s):return int(s)
 if s=='Short.MAX_VALUE':return 32767
 if '+' in s:return ''.join(str(literal(x.strip(),env)) for x in s.split('+'))
 raise ValueError(s)

def item(s,env):
 s=stripcast(s)
 if s in env:return env[s]
 if s.startswith(('Ingredient.func_193367_a(','Ingredient.func_193369_a(','new IngredientNBTTC(')):return item(body(s)[0],env)
 if s.startswith('new ItemStack('):
  p=body(s);base=item(p[0],env).split(';')[0];count=literal(p[1],env) if len(p)>1 else 1;meta=literal(p[2],env) if len(p)>2 else 0
  # Modern vanilla metadata flattening.
  if base=='minecraft:dye':return 'minecraft:'+['ink_sac','red_dye','green_dye','cocoa_beans','lapis_lazuli','purple_dye','cyan_dye','light_gray_dye','gray_dye','pink_dye','lime_dye','yellow_dye','light_blue_dye','magenta_dye','orange_dye','bone_meal'][meta%16]+f';{count}'
  if base=='minecraft:skull':return 'minecraft:'+['skeleton_skull','wither_skeleton_skull','zombie_head','player_head','creeper_head','dragon_head'][meta%6]+f';{count}'
  if base=='minecraft:fish':return 'minecraft:'+['cod','salmon','tropical_fish','pufferfish'][meta%4]+f';{count}'
  if base=='minecraft:coal' and meta==1:return f'minecraft:charcoal;{count}'
  if base in ['minecraft:wool','minecraft:stained_glass','minecraft:stained_glass_pane','minecraft:carpet']:return 'minecraft:'+colors[meta%16].replace('silver','light_gray')+'_'+base[10:]+f';{count}'
  return base+f';{count};{meta}'
 if s.startswith(('ItemSealPlacer.getSealStack(','GolemHelper.getSealStack(')):return 'thaumcraft:seals;1;0;'+str(literal(body(s)[0],env))
 if s.startswith('ItemPhial.makeFilledPhial('):return 'thaumcraft:phial;1;1;{Aspects:[{key:"'+aspects[env.get(body(s)[0],body(s)[0].split('.')[-1])]+'",amount:10}]}'
 if s.startswith('ThaumcraftApiHelper.makeCrystal('):return 'thaumcraft:crystal_essence;1;0;{Aspects:[{key:"'+aspects[env.get(body(s)[0],body(s)[0].split('.')[-1])]+'",amount:1}]}'
 if s=='nitorStacks':return 'oredict:nitor'
 if s.startswith('ConfigRecipes.ingredientsFromBlocks('):return 'oredict:tallowCandles'
 s=re.sub(r'\.func_176(?:223_P|203_a)\(.*','',s)
 if s.startswith('ConfigItems.') and s.endswith('_CRYSTAL'):
  tag=aspects[s.split('.')[1].replace('_CRYSTAL','')];return 'thaumcraft:crystal_essence;1;0;{Aspects:[{key:"'+tag+'",amount:1}]}'
 m=re.fullmatch(r'BlocksTC\.(nitor|candles|banners)\.get\(EnumDyeColor\.(\w+)\)',s)
 if m:return 'thaumcraft:'+{'nitor':'nitor','candles':'candle','banners':'banner'}[m[1]]+'_'+m[2].lower()
 if s.startswith('FluidUtil.getFilledBucket('):return 'thaumcraft:bucket_death'
 if s in items:return items[s]
 m=re.fullmatch(r'(Items|Blocks)\.(\w+)',s)
 if m:
  n=fields.get(m[2],m[2].lower());return 'minecraft:'+vanilla_alias.get(n,n)
 if s.startswith('"'):return 'oredict:'+literal(s,env)
 if s=='ItemStack.field_190927_a':return 'minecraft:air'
 raise ValueError(s)
def al(s):return {aspects[k]:int(n) for k,n in re.findall(r'\.(?:add|merge)\(Aspect\.(\w+), (\d+)\)',s)}
def ingredient_value(expression,env):
 expression=stripcast(expression)
 if expression.startswith('new ItemStack('):
  p=body(expression)
  if len(p)>2 and item(p[0],env).split(';')[0]=='minecraft:wool' and literal(p[2],env)==32767:
   count=literal(p[1],env)
   return [{'legacy':'minecraft:'+color.replace('silver','light_gray')+'_wool;'+str(count)} for color in colors]
 return {'legacy':item(expression,env)}
def ingredients(args,env,shaped):
 args=[stripcast(x) for x in args]
 if len(args)==1 and args[0].startswith('new Object[]{'):args=split(args[0][13:-1])
 if not shaped:return {'ingredients':[ingredient_value(x,env) for x in args]}
 pattern=[]
 while args and args[0].startswith('"'):pattern.append(literal(args.pop(0),env))
 keys={}
 for i in range(0,len(args),2):
  k=re.search(r"'(.+)'",args[i]);keys[k[1]]=ingredient_value(args[i+1],env)
 return {'pattern':pattern,'key':keys}
# Named item stacks used by the fake infusion recipes.
variables={}
for m in re.finditer(r'ItemStack\s+(\w+)\s*=\s*([^;]+);',text):
 try:variables[m[1]]=item(m[2],{})
 except (ValueError,KeyError,IndexError):pass
for m in re.finditer(r'(\w+)\.func_77983_a\("Potion", \(NBTBase\)new NBTTagString\("([^"]+)"\)\);',text):
 if m[1] in variables:variables[m[1]]=variables[m[1]].split(';')[0]+';1;0;{Potion:"'+m[2]+'"}'
for line in text.splitlines():
 line=line.strip();m=re.search(r'(ThaumcraftApi\.(?:add\w+Recipe)|ConfigRecipes\.(?:oreDictRecipe|shapelessOreDictRecipe)|GameRegistry\.addShapedRecipe)\(',line)
 if not m or not line.endswith(';'):continue
 expr=line[m.start():].rstrip(';');env=variables.copy()
 environments=[env]
 if 'EnumDyeColor.' in line:
  for c in colors:env['EnumDyeColor.'+c.upper()]=c
 if 'd.func_176762_d()' in line or 'BlocksTC.nitor.get(d)' in line or 'BlocksTC.banners.get(d)' in line:
  environments=[]
  for i,c in enumerate(colors):
   en=env.copy();en.update({'d.func_176762_d().toLowerCase()':c,'a':i,'BlocksTC.nitor.get(d)':'thaumcraft:nitor_'+c,'BlocksTC.banners.get(d)':'thaumcraft:banner_'+c,'banner':'thaumcraft:banner_'+c,'BlocksTC.candles.get(d)':'thaumcraft:candle_'+c,'ConfigAspects.dyes[15 - a]':'minecraft:'+c.replace('silver','light_gray')+'_dye'});environments.append(en)
 for env in environments:
  try:
   args=body(expr);key=literal(args[0],env);key=key if ':' in key else 'thaumcraft:'+key
   call=m[1];recipe={};group=''
   if 'oreDictRecipe' in call or 'shapelessOreDictRecipe' in call or 'addShapedRecipe' in call:
    group=literal(args[1],env);recipe={'type':'minecraft:crafting_shapeless' if 'shapeless' in call else 'minecraft:crafting_shaped','result':{'legacy':item(args[2],env)},**ingredients(args[3:],env,'shapeless' not in call)}
   else:
    cons=args[1];kind=cons.split('(')[0].replace('new ','');p=body(cons)
    if 'Crucible' in kind:recipe={'type':'thaumcraft:crucible','research':literal(p[0],env),'result':{'legacy':item(p[1],env)},'catalyst':{'legacy':item(p[2],env)},'aspects':al(p[3])}
    elif 'Arcane' in kind:
     group=literal(p[0],env);recipe={'type':'thaumcraft:arcane','research':literal(p[1],env),'vis':literal(p[2],env),'crystals':al(p[3]),'result':{'legacy':item(p[4],env)},**ingredients(p[5:],env,'Shapeless' not in kind)}
    elif kind in ['ShapelessOreRecipe','ShapedOreRecipe']:
     group=literal(p[0],env);recipe={'type':'minecraft:crafting_shapeless' if kind.startswith('Shapeless') else 'minecraft:crafting_shaped','result':{'legacy':item(p[1],env)},**ingredients(p[2:],env,kind.startswith('Shaped'))}
    elif kind=='InfusionRecipe':
     modification=p[1] if p[1].startswith('new Object[]{') else None
     if modification:p[1]=p[4]
     recipe={'type':'thaumcraft:infusion','research':literal(p[0],env),'result':{'legacy':item(p[1],env)},'instability':literal(p[2],env),'aspects':al(p[3]),'input':{'legacy':item(p[4],env)},**ingredients(p[5:],env,False)}
     if modification:
      tag,value=split(modification[len('new Object[]{'):-1])
      nbt=re.fullmatch(r'new NBTTag(Byte|Int)\((-?\d+)\)',value)
      if not nbt:raise ValueError('Unsupported infusion output modification: '+modification)
      descriptor=recipe['result']['legacy'].split(';',3)
      while len(descriptor)<3:descriptor.append('1' if len(descriptor)==1 else '0')
      recipe['result']['legacy']=';'.join(descriptor[:3])+';{'+str(literal(tag,env))+':'+nbt[2]+('b' if nbt[1]=='Byte' else '')+'}'
    else:raise ValueError(kind)
   if group and group not in ['thaumcraft:default','']:recipe['book_group']=group
   recipes[key]=recipe
  except (ValueError,KeyError,IndexError,TypeError,AttributeError) as e:unresolved.append({'line':line,'reason':str(e)})
# Dynamic original catalogs, evaluated explicitly from their definitions.
for k,tag in aspects.items():
 crystal='thaumcraft:crystal_essence;1;0;{Aspects:[{key:"'+tag+'",amount:1}]}'
 recipes['thaumcraft:vis_crystal_'+tag]={'type':'thaumcraft:crucible','research':'BASEALCHEMY','result':{'legacy':crystal},'catalyst':{'legacy':'oredict:nuggetQuartz'},'aspects':{tag:2},'book_group':'thaumcraft:viscrystalgroup'}
 recipes['thaumcraft:label_'+tag]={'type':'minecraft:crafting_shapeless','result':{'legacy':'thaumcraft:label;1;1;{Aspects:[{key:"'+tag+'",amount:1}]}'},'ingredients':[{'legacy':'thaumcraft:label'},{'legacy':'thaumcraft:phial;1;1;{Aspects:[{key:"'+tag+'",amount:10}]}'}],'book_group':'thaumcraft:jarlabels'}
for n in range(3):
 v=20+20*2**n
 recipes['thaumcraft:RunicArmorFake'+str(n)]={'type':'thaumcraft:infusion','research':'RUNICSHIELDING','result':{'legacy':'thaumcraft:baubles;1;1'},'input':{'legacy':'thaumcraft:baubles;1;1'},'instability':5+n//2,'aspects':{'praemunio':v,'vitreus':v//2,'potentia':v//2},'ingredients':[{'legacy':'thaumcraft:salis_mundus'}]+[{'legacy':'oredict:gemAmber'}]*(n+1),'runic_before':n,'runic_after':n+1}
for m in re.finditer(r'InfusionEnchantmentRecipe (IE\w+) = (new InfusionEnchantmentRecipe\([^;]+);',text):
 p=body(m[2]);fake=re.search(r'new InfusionEnchantmentRecipe\('+m[1]+r', (new ItemStack\([^;]+?)\)\);',text)
 if not fake:continue
 stack=item(fake[1],variables)
 recipes['thaumcraft:'+m[1]+'FAKE']={'type':'thaumcraft:infusion','result':{'legacy':stack},'input':{'legacy':stack},'instability':4,'aspects':al(p[1]),**ingredients(p[2:],variables,False),'enchantment':p[0].split('.')[-1]}
# Preserve source AND completed multiblocks. Top layer first, then row, then column.
# Unlike recipe ingredients, block displays must retain legacy state metadata.
def blueprint_block(expression):
 expression=expression.strip()
 if expression=='"AIR"':return 'minecraft:air'
 if expression=='Material.field_151587_i':return 'minecraft:lava'
 if expression=='Material.field_151586_h':return 'minecraft:water'
 # BlockPillar.calcMeta uses getHorizontalIndex, not the six-way getIndex.
 expression=re.sub(r'BlockPillar\.calcMeta\(EnumFacing\.(\w+)\)',lambda m:str({'SOUTH':0,'WEST':1,'NORTH':2,'EAST':3}.get(m[1],2)),expression)
 state=re.search(r'\.func_176203_a\((\d+)\)',expression)
 facing=re.search(r'BlockPistonBase\.field_176387_N, \(Comparable\)EnumFacing\.(\w+)',expression)
 result=item(expression,{})
 if state:result=result.split(';')[0]+';1;'+state[1]
 if facing:result=result.split(';')[0]+';1;'+str({'DOWN':0,'UP':1,'NORTH':2,'SOUTH':3,'WEST':4,'EAST':5}[facing[1]])
 # ConfigBlocks passes false/true to a constructor with two registry names.
 if expression.startswith('BlocksTC.thaumatorium'):
  result='thaumcraft:thaumatorium_top' if expression.startswith('BlocksTC.thaumatoriumTop') else 'thaumcraft:thaumatorium'
 return result
def blueprint_ingredient(expression):
 value=item(expression,{})
 # In 1.12 this ID names a block; the same modern ID names the single brick item.
 if value.split(';',1)[0]=='minecraft:nether_brick':value=value.replace('minecraft:nether_brick','minecraft:nether_bricks',1)
 return value
parts={};target_parts={}
for m in re.finditer(r'Part (\w+) = new Part\(([^;]+);',text):
 p=body('new Part('+m[2]);parts[m[1]]=blueprint_block(p[0]);target_parts[m[1]]=parts[m[1]] if p[1]=='null' else blueprint_block(p[1])
blueprints={};target_blueprints={}
for m in re.finditer(r'Part\[\]\[\]\[\] (\w+) = new Part\[\]\[\]\[\](.*?);',text):
 for source,destination in [(parts,blueprints),(target_parts,target_blueprints)]:
  raw=re.sub(r'\b(null|[A-Z][A-Z0-9]+)\b',lambda x:json.dumps(source.get(x[0],'minecraft:air')),m[2]);destination[m[1]]=json.loads(raw.replace('{','[').replace('}',']'))
for line in text.splitlines():
 if 'ThaumcraftApi.addMultiblockRecipeToCatalog(' not in line:continue
 p=body(line[line.index('ThaumcraftApi.'):]);key=literal(p[0],{});q=body(p[1]);bi=next(i for i,v in enumerate(q) if v in blueprints);layers=blueprints[q[bi]];components=[{'legacy':blueprint_ingredient(x)} for x in q[bi+1:]]
 output=item(q[1],{}) if bi==2 else next((cell for layer in layers for row in layer for cell in row if cell!='minecraft:air'),'minecraft:air')
 recipes[key]={'type':'thaumcraft:blueprint','research':literal(q[0],{}),'result':{'legacy':output},'display_stack':bi==2,'layers':layers,'target_layers':target_blueprints[q[bi]],'ingredients':components}
# Keep already-ported recipes for compatibility, but the original catalog has precedence for book links.
modern={}
for p in (res/'data/thaumcraft/recipe').rglob('*.json'):modern['thaumcraft:'+str(p.relative_to(res/'data/thaumcraft/recipe')).removesuffix('.json')]=json.loads(p.read_text())
modern.update(recipes)
(res/'thaumcraft-port/book-recipes.json').write_text(json.dumps({'recipes':modern},ensure_ascii=False,indent=2)+'\n')
refs={r for p in (res/'assets/thaumcraft/research').glob('*.json') for e in json.loads(p.read_text())['entries'] for st in e.get('stages',[])+e.get('addenda',[]) for r in st.get('recipes',[])}
resolved={k.lower() for k in recipes}|{r['book_group'].lower() for r in recipes.values() if 'book_group' in r}
report={'original_recipe_count':len(recipes),'unresolved_references':sorted(r for r in refs if r.lower() not in resolved),'unresolved_expressions':unresolved,'item_fields':items}
(root/'reference/book-recipe-import.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('Original recipes:',len(recipes),'Unresolved references:',len(report['unresolved_references']),'Expressions:',len(unresolved))
