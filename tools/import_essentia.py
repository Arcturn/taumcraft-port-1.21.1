#!/usr/bin/env python3
"""Migrate TC6 essentia assets without redrawing source textures or replacing original UVs."""
import argparse, hashlib, json, pathlib, zipfile
p=argparse.ArgumentParser();p.add_argument('--jar',type=pathlib.Path,required=True);args=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];res=root/'src/main/resources';records={}
def js(path,value):
    dest=res/path;dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_text(json.dumps(value,ensure_ascii=False,indent=2).replace('thaumcraft:blocks/','thaumcraft:block/').replace('thaumcraft:items/','thaumcraft:item/')+'\n')
def item(name,texture=None):js(f'assets/thaumcraft/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:item/'+(texture or name)}})
def block(name,model=None):
    js(f'assets/thaumcraft/blockstates/{name}.json',{'variants':{'':{'model':'thaumcraft:block/'+(model or name)}}})
    js(f'assets/thaumcraft/models/item/{name}.json',{'parent':'thaumcraft:block/'+(model or name)})
    js(f'data/thaumcraft/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'thaumcraft:'+name}]}]})
def ing(value):return {'tag' if value.startswith('#') else 'item':value.lstrip('#') if ':' in value else 'thaumcraft:'+value}
recipes=[]
def recipe(name,research,vis,crystals,pattern,key,count=1,shapeless=False):
    value={'type':'thaumcraft:arcane','research':research,'vis':vis,'crystals':crystals,'pattern':pattern,'key':{k:ing(v) for k,v in key.items()},'result':{'id':'thaumcraft:'+name,'count':count}}
    if shapeless:value['shapeless']=True
    js(f'data/thaumcraft/recipe/{name}.json',value);recipes.append(name)
with zipfile.ZipFile(args.jar) as z:
    def copy(path):
        target=path.replace('/textures/blocks/','/textures/block/').replace('/textures/items/','/textures/item/')
        data=z.read(path);out=res/target;out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(data)
        records[path]={'source':path,'target':target,'sha256':hashlib.sha256(data).hexdigest()}
    def texture(ref):
        if not ref.startswith('thaumcraft:'):return
        path='assets/thaumcraft/textures/'+ref.split(':',1)[1]+'.png';copy(path)
        if path+'.mcmeta' in z.namelist():copy(path+'.mcmeta')
    def model(name,render=None):
        path='assets/thaumcraft/models/block/'+name+'.json';data=json.loads(z.read(path))
        if render:data['render_type']=render
        if name.startswith('smelter_basic_'):data['textures']['down']='minecraft:block/furnace_top'
        for ref in data.get('textures',{}).values():texture(ref)
        js(path,data);records[path]={'source':path,'target':path,'transformation':'Original elements/UVs retained; singular texture directories and explicit modern render layer'}
        return data
    for name in ['jar_normal','jar_void']:
        model(name,'minecraft:translucent');block(name)
        overrides=[]
        for n in [25,50,75,100]:
            model(name+'_'+str(n),'minecraft:translucent');overrides.append({'predicate':{'thaumcraft:fill':n/100},'model':'thaumcraft:block/'+name+'_'+str(n)})
        js(f'assets/thaumcraft/models/item/{name}.json',{'parent':'thaumcraft:block/'+name,'overrides':overrides})
    model('alembic_normal','minecraft:cutout');block('alembic','alembic_normal')
    centrifuge=model('centrifuge');block('centrifuge');copy('assets/thaumcraft/textures/models/centrifuge.png');copy('assets/thaumcraft/textures/models/valve.png')
    js('assets/thaumcraft/models/block/centrifuge_item.json',centrifuge)
    js('assets/thaumcraft/models/item/centrifuge.json',{'parent':'thaumcraft:block/centrifuge_item'})
    centrifuge['elements']=centrifuge['elements'][:2];js('assets/thaumcraft/models/block/centrifuge.json',centrifuge)
    records['assets/thaumcraft/models/block/centrifuge.json']['transformation']='Original housing elements/UVs; four moving elements rendered separately using original ModelCentrifuge geometry and texture'
    for name in ['tube_core','tube_core_valve','tube_filter_core','tube_side','tube_side_restrict']:model(name,'minecraft:cutout')
    orientations={'down':{},'up':{'x':180},'north':{'x':270},'south':{'x':90},'west':{'x':90,'y':90},'east':{'x':90,'y':270}}
    for name in ['tube','tube_filter','tube_restrict','tube_oneway','tube_valve']:
        block(name,'tube_core')
        core='tube_filter_core' if name=='tube_filter' else 'tube_core'
        parts=[{'apply':{'model':'thaumcraft:block/'+core}}]
        for direction,angles in orientations.items():parts.append({'when':{direction:'true'},'apply':{'model':'thaumcraft:block/'+('tube_side_restrict' if name=='tube_restrict' else 'tube_side'),**angles}})
        if name=='tube_valve':
            for direction,angles in orientations.items():parts.append({'when':{'facing':direction},'apply':{'model':'thaumcraft:block/tube_core_valve',**angles}})
        js(f'assets/thaumcraft/blockstates/{name}.json',{'multipart':parts})
        tex='tube_normal' if name=='tube' else name;texture('thaumcraft:items/'+tex);item(name,tex)
    for tier in ['basic','thaumium','void']:
        name='smelter_'+tier;block(name,name+'_off');variants={}
        for lit in [False,True]:
            variant=name+('_on' if lit else '_off');model(variant)
            for face,rot in [('north',0),('east',90),('south',180),('west',270)]:variants[f'facing={face},lit={str(lit).lower()}']={'model':'thaumcraft:block/'+variant,'y':rot}
        js(f'assets/thaumcraft/blockstates/{name}.json',{'variants':variants})
    for name in ['metal_alchemical','metal_alchemical_advanced']:
        texture('thaumcraft:blocks/'+name);js(f'assets/thaumcraft/models/block/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':'thaumcraft:block/'+name}});block(name)
    for name in ['phial','phial_overlay','label','label_overlay','filter','plate_iron','plate_brass','plate_thaumium','plate_void','mechanism_simple','morphic_resonator']:
        texture('thaumcraft:items/'+name)
        if not name.endswith('_overlay'):item(name)
    for name in ['phial','label']:
        (res/f'assets/thaumcraft/models/item/{name}_filled.json').unlink(missing_ok=True)
        js(f'assets/thaumcraft/models/item/variants/{name}_filled.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:item/'+name,'layer1':'thaumcraft:item/'+name+'_overlay'}})
        js(f'assets/thaumcraft/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:item/'+name},'overrides':[{'predicate':{'thaumcraft:filled':1},'model':'thaumcraft:item/variants/'+name+'_filled'}]})
    copy('assets/thaumcraft/textures/gui/gui_smelter.png');copy('assets/thaumcraft/textures/models/label.png')
    recipe('jar_normal','WARDEDJARS',5,{},['GWG','G G','GGG'],{'G':'minecraft:glass_pane','W':'#minecraft:wooden_slabs'})
    recipe('jar_void','WARDEDJARS',50,{'perditio':1},['J'],{'J':'jar_normal'})
    recipe('filter','BASEALCHEMY',15,{'aqua':1},['GWG'],{'G':'minecraft:gold_ingot','W':'plank_silverwood'},2)
    recipe('alembic','ESSENTIASMELTER',50,{'aqua':1},['WFW','SBS','WFW'],{'W':'plank_greatwood','F':'filter','S':'plate_brass','B':'minecraft:bucket'})
    recipe('smelter_basic','ESSENTIASMELTER@2',50,{'ignis':1},['BCB','SFS','SSS'],{'B':'plate_brass','C':'crucible','S':'minecraft:cobblestone','F':'minecraft:furnace'})
    recipe('smelter_thaumium','ESSENTIASMELTERTHAUMIUM',250,{'ignis':2},['BFB','IGI','III'],{'B':'plate_brass','F':'smelter_basic','I':'plate_thaumium','G':'metal_alchemical'})
    recipe('smelter_void','ESSENTIASMELTERVOID',750,{'ignis':3},['BFB','IGI','III'],{'B':'plate_brass','F':'smelter_basic','I':'plate_void','G':'metal_alchemical_advanced'})
    recipe('tube','TUBES',10,{},[' Q ','IGI',' B '],{'Q':'nugget_quicksilver','I':'plate_iron','G':'minecraft:glass','B':'nugget_brass'},8)
    recipe('tube_filter','TUBES',10,{},['TF'],{'T':'tube','F':'filter'},shapeless=True)
    recipe('tube_valve','TUBES',10,{},['TL'],{'T':'tube','L':'minecraft:lever'},shapeless=True)
    recipe('tube_oneway','TUBES',10,{'aqua':1},['T'],{'T':'tube'},shapeless=True)
    recipe('tube_restrict','TUBES',10,{'terra':1},['T'],{'T':'tube'},shapeless=True)
    recipe('metal_alchemical','TUBES',75,{'aqua':1,'ordo':1,'perditio':1},['IVI','TWT','IVI'],{'I':'plate_iron','V':'tube_valve','T':'tube','W':'plank_greatwood'},2)
    recipe('mechanism_simple','BASEARTIFICE',10,{'ignis':1,'aqua':1},[' B ','ISI',' B '],{'B':'plate_brass','I':'plate_iron','S':'minecraft:stick'})
    recipe('metal_alchemical_advanced','ESSENTIASMELTERVOID@1',200,{'terra':1,'ignis':1},[' A ','VPV',' A '],{'A':'metal_alchemical','V':'plate_void','P':'primordial_pearl'})
    recipe('morphic_resonator','BASEALCHEMY',50,{'aer':1,'ignis':1},[' G ','BSB',' G '],{'G':'minecraft:glass_pane','B':'plate_brass','S':'nugget_quartz'})
    recipe('centrifuge','CENTRIFUGE',100,{'ordo':1,'perditio':1},[' T ','RCP',' T '],{'T':'tube','R':'morphic_resonator','C':'metal_alchemical','P':'mechanism_simple'})
    for metal in ['iron','brass','thaumium','void']:
        source='minecraft:iron_ingot' if metal=='iron' else 'thaumcraft:ingot_'+metal
        js(f'data/thaumcraft/recipe/plate_{metal}.json',{'type':'minecraft:crafting_shaped','pattern':['BBB'],'key':{'B':ing(source)},'result':{'id':'thaumcraft:plate_'+metal,'count':3}})
    js('data/thaumcraft/recipe/phial.json',{'type':'minecraft:crafting_shaped','pattern':[' C ','G G',' G '],'key':{'C':{'item':'minecraft:clay_ball'},'G':{'item':'minecraft:glass'}},'result':{'id':'thaumcraft:phial','count':8}})
    js('data/thaumcraft/recipe/label.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'tag':'c:dyes/black'},{'item':'minecraft:slime_ball'}]+[{'item':'minecraft:paper'}]*4,'result':{'id':'thaumcraft:label','count':4}})
    js('data/thaumcraft/recipe/label_aspect.json',{'type':'thaumcraft:label_aspect','category':'misc'})
    js('data/neoforge/data_maps/item/furnace_fuels.json',{'values':{'thaumcraft:alumentum':{'burn_time':4800}}})
    for lang in ['en_us','ru_ru']:
        path=res/f'assets/thaumcraft/lang/{lang}.json';data=json.loads(path.read_text());ru=lang=='ru_ru'
        names={'jar_normal':('Warded Jar','Защищённая банка'),'jar_void':('Void Jar','Пустотная банка'),'alembic':('Arcane Alembic','Перегонный куб'),'centrifuge':('Alchemical Centrifuge','Алхимическая центрифуга'),'tube':('Essentia Tube','Эссенциепровод'),'tube_filter':('Filtered Essentia Tube','Фильтрующий эссенциепровод'),'tube_valve':('Essentia Valve','Эссенциевый клапан'),'tube_oneway':('Directional Essentia Tube','Направленный эссенциепровод'),'tube_restrict':('Restricted Essentia Tube','Ограниченный эссенциепровод'),'smelter_basic':('Essentia Smeltery','Алхимическая печь'),'smelter_thaumium':('Thaumium Essentia Smeltery','Таумиевая алхимическая печь'),'smelter_void':('Void Metal Essentia Smeltery','Пустотная алхимическая печь'),'metal_alchemical':('Alchemical Construct','Алхимическая конструкция'),'metal_alchemical_advanced':('Advanced Alchemical Construct','Продвинутая алхимическая конструкция')}
        for name,pair in names.items():data['block.thaumcraft.'+name]=pair[ru]
        for name,pair in {'phial':('Glass Phial','Стеклянная колба'),'label':('Jar Label','Бирка'),'filter':('Essentia Filter','Эссенциевый фильтр'),'mechanism_simple':('Simple Arcane Mechanism','Простой магический механизм'),'morphic_resonator':('Morphic Resonator','Морфический резонатор')}.items():data['item.thaumcraft.'+name]=pair[ru]
        for metal,translation in [('iron','Железная'),('brass','Латунная'),('thaumium','Таумиевая'),('void','Пустотная')]:data['item.thaumcraft.plate_'+metal]=translation+' пластина' if ru else metal.title()+' Plate'
        path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
(root/'reference/essentia-provenance.json').write_text(json.dumps({'source_sha256':hashlib.sha256(args.jar.read_bytes()).hexdigest(),'assets':list(records.values()),'arcane_recipes':recipes,'remaining':'Extended by import_essentia_extensions.py; remaining gaps tracked in PORTING-16.md'},indent=2)+'\n')
print('Imported',len(records),'essentia asset records;',len(recipes),'arcane recipes')
