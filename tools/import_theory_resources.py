#!/usr/bin/env python3
"""Import exact TC6 theory artwork and recipe definitions; no generated replacement textures."""
import argparse, hashlib, json, pathlib, zipfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'

def write(path,value):
    path=RES/path;path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--legacy-jar',required=True);args=parser.parse_args()
    original=zipfile.ZipFile(args.legacy_jar);hashes={}
    def copy(name):
        data=original.read(name);path=RES/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data);hashes[name]=hashlib.sha256(data).hexdigest()
    def texture(name):copy('assets/thaumcraft/textures/'+name+'.png')
    for name in ['gui/gui_research_table','gui/gui_base','gui/paper','gui/papergilded','blocks/research_table_model','research/quill']:
        texture(name)
    for block in ['table_wood','research_table']:
        model=json.loads(original.read('assets/thaumcraft/models/block/'+block+'.json'));model['parent']='minecraft:block/block'
        for value in model['textures'].values():texture(value.removeprefix('thaumcraft:'))
        write(pathlib.Path('assets/thaumcraft/models/block')/(block+'.json'),model)
        write(pathlib.Path('assets/thaumcraft/models/item')/(block+'.json'),{'parent':'thaumcraft:block/'+block})
        variants={'':{'model':'thaumcraft:block/'+block}} if block=='table_wood' else {'facing='+f:{'model':'thaumcraft:block/'+block,'y':a} for f,a in [('north',0),('east',90),('south',180),('west',270)]}
        write(pathlib.Path('assets/thaumcraft/blockstates')/(block+'.json'),{'variants':variants})
        write(pathlib.Path('data/thaumcraft/loot_table/blocks')/(block+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'thaumcraft:'+block}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    types=['arcane','preserved','ancient','eldritch','knowledge','twisted','rites']
    for item,tex in [('vis_resonator','vis_resonator'),('brain','brain'),('fabric','fabric')]+[('curio_'+t,'crimson_rites' if t=='rites' else 'curio_'+t) for t in types]:
        texture('items/'+tex);write(pathlib.Path('assets/thaumcraft/models/item')/(item+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:items/'+tex}})
    write(pathlib.Path('assets/thaumcraft/models/research/quill.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:research/quill'}})
    write(pathlib.Path('assets/thaumcraft/particles/theory_rune.json'),{'textures':['thaumcraft:legacy']})
    atlas=RES/'assets/minecraft/atlases/blocks.json';data=json.loads(atlas.read_text());entry={'type':'minecraft:single','resource':'thaumcraft:research/quill','sprite':'thaumcraft:research/quill'}
    if entry not in data['sources']:data['sources'].append(entry)
    write(pathlib.Path('assets/minecraft/atlases/blocks.json'),data)
    write(pathlib.Path('data/thaumcraft/recipe/tablewood.json'),{'type':'minecraft:crafting_shaped','pattern':['SSS','W W'],'key':{'S':{'tag':'minecraft:wooden_slabs'},'W':{'tag':'minecraft:planks'}},'result':{'id':'thaumcraft:table_wood'}})
    write(pathlib.Path('data/thaumcraft/recipe/vis_resonator.json'),{'type':'thaumcraft:arcane','research':'UNLOCKAUROMANCY@2','vis':50,'crystals':{'aer':1,'aqua':1},'shapeless':True,'pattern':['PQ'],'key':{'P':{'tag':'c:plates/iron'},'Q':{'tag':'c:gems/quartz'}},'result':{'id':'thaumcraft:vis_resonator'}})
    write(pathlib.Path('data/thaumcraft/recipe/enchanted_fabric.json'),{'type':'thaumcraft:arcane','research':'UNLOCKINFUSION','vis':5,'pattern':[' S ','SCS',' S '],'key':{'S':{'item':'minecraft:string'},'C':{'tag':'minecraft:wool'}},'result':{'id':'thaumcraft:fabric'}})
    tag=RES/'data/minecraft/tags/block/mineable/axe.json';data=json.loads(tag.read_text());data['values']=list(dict.fromkeys(data['values']+['thaumcraft:table_wood','thaumcraft:research_table']));write(pathlib.Path('data/minecraft/tags/block/mineable/axe.json'),data)
    # Original amber ore replaces the entire amber stack with one preserved curiosity (6.6%).
    amber=RES/'data/thaumcraft/loot_table/blocks/ore_amber.json';data=json.loads(amber.read_text());children=data['pools'][0]['entries'][0]['children'];children[:]=[c for c in children if c.get('name')!='thaumcraft:curio_preserved'];children.insert(1,{'type':'minecraft:item','name':'thaumcraft:curio_preserved','conditions':[{'condition':'minecraft:random_chance','chance':.066}]});write(pathlib.Path('data/thaumcraft/loot_table/blocks/ore_amber.json'),data)
    aspectfile=RES/'thaumcraft-port/aspect-tags.json';data=json.loads(aspectfile.read_text());data['item:thaumcraft:brain']={'victus':5,'cognitio':20,'exanimis':10}
    aspects=[{'cognitio':15,'praecantatio':15},{'cognitio':15,'bestia':15},{'cognitio':15,'mortuus':15},{'cognitio':15,'alienis':15},{'cognitio':30},{'cognitio':15,'vitium':15},{'cognitio':15,'alienis':5,'spiritus':5,'praecantatio':5}]
    for t,a in zip(types,aspects):data['item:thaumcraft:curio_'+t]=a
    write(pathlib.Path('thaumcraft-port/aspect-tags.json'),data)
    migration=ROOT/'reference/aspect-migration.json';rows=json.loads(migration.read_text())
    for i,item in enumerate(['brain']+['curio_'+t for t in types]):
        target='item:thaumcraft:'+item
        if not any(row['target']==target for row in rows):rows.append({'legacy':'new ItemStack(ItemsTC.brain)' if i==0 else 'new ItemStack(ItemsTC.curio, 1, '+str(i-1)+')','target':target,'aspects':data[target],'source':'ConfigAspects.java','source_line':536 if i==0 else 575+i})
    migration.write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
    for language in ['en_us','ru_ru']:
        path=RES/('assets/thaumcraft/lang/'+language+'.json');data=json.loads(path.read_text())
        legacy={line.split('=',1)[0]:line.split('=',1)[1] for line in original.read('assets/thaumcraft/lang/'+language+'.lang').decode('utf-8-sig').splitlines() if '=' in line and not line.startswith('#')}
        for block in ['table_wood','research_table']:data['block.thaumcraft.'+block]=legacy['tile.'+block+'.name']
        for item in ['vis_resonator','brain','fabric']:data['item.thaumcraft.'+item]=legacy['item.'+item+'.name']
        for t in types:data['item.thaumcraft.curio_'+t]=legacy['item.curio.'+t+'.name']
        data.update({'thaumcraft.theory.unavailable':'Не хватает ресурсов или это действие уже недоступно.' if language=='ru_ru' else 'Missing resources or this action is no longer available.', 'thaumcraft.theory.other_owner':'Теория другого исследователя' if language=='ru_ru' else 'Another researcher’s theory'})
        for aid in ['PortalEnd','PortalNether','PortalCrimson']:data['thaumcraft.theory.aid.'+aid]=({'PortalEnd':'Портал Края','PortalNether':'Портал Нижнего мира','PortalCrimson':'Портал культистов'} if language=='ru_ru' else {'PortalEnd':'End portal','PortalNether':'Nether portal','PortalCrimson':'Crimson portal'})[aid]
        write(pathlib.Path('assets/thaumcraft/lang')/(language+'.json'),data)
    record={'source':'Thaumcraft 6.1.BETA26','source_jar_sha256':hashlib.sha256(pathlib.Path(args.legacy_jar).read_bytes()).hexdigest(),'unchanged_texture_sha256':hashes,'gui_size':[255,255],'ink_slot':[16,15],'paper_slot':[224,16],'card_scale':[6,8,6],'original_card_count':33,'original_aid_count':14,'unregistered_original_cards':['Truth','DragonEgg'],'runtime_visual_validation':False}
    parity_path=ROOT/'reference/theory-parity.json';prior=json.loads(parity_path.read_text()) if parity_path.exists() else {};prior.update(record)
    parity_path.write_text(json.dumps(prior,ensure_ascii=False,indent=2)+'\n')
    print('Imported',len(hashes),'original textures; theory recipes, models, translations and parity record.')

if __name__=='__main__':main()
