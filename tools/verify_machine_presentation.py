#!/usr/bin/env python3
"""Compare workbench/smelter assets to TC6 and resolve real Minecraft model parents/textures."""
import argparse, hashlib, json, pathlib, zipfile

parser=argparse.ArgumentParser()
parser.add_argument('--legacy-jar',type=pathlib.Path,required=True)
parser.add_argument('--minecraft-jar',type=pathlib.Path,required=True)
parser.add_argument('--jar',type=pathlib.Path,required=True)
args=parser.parse_args()
sha=lambda b:hashlib.sha256(b).hexdigest()
with zipfile.ZipFile(args.legacy_jar) as legacy,zipfile.ZipFile(args.minecraft_jar) as vanilla,zipfile.ZipFile(args.jar) as port:
    models=['arcane_workbench']+[f'smelter_{tier}_{state}' for tier in ['basic','thaumium','void'] for state in ['on','off']]
    textures=set()
    for name in models:
        path=f'assets/thaumcraft/models/block/{name}.json'
        old=json.loads(legacy.read(path));new=json.loads(port.read(path))
        assert old.get('elements')==new.get('elements'),f'Changed original geometry/UVs: {name}'
        for key,source in old['textures'].items():
            target=new['textures'][key]
            if source.startswith('thaumcraft:'):
                expected=source.replace('thaumcraft:blocks/','thaumcraft:block/').replace('thaumcraft:items/','thaumcraft:item/')
                assert target==expected,(name,key,target)
                before='assets/thaumcraft/textures/'+source.split(':')[1]+'.png'
                after='assets/thaumcraft/textures/'+target.split(':')[1]+'.png'
                assert legacy.read(before)==port.read(after),after
                textures.add(after)
            elif not source.startswith('#'):
                assert source=='blocks/furnace_top' and target=='minecraft:block/furnace_top',(name,source,target)
    for name in ['arcaneworkbench','gui_smelter']:
        path=f'assets/thaumcraft/textures/gui/{name}.png'
        assert legacy.read(path)==port.read(path),path
        textures.add(path)
    def resource(identifier,kind):
        ns,path=identifier.split(':',1) if ':' in identifier else ('minecraft',identifier)
        archive=port if ns=='thaumcraft' else vanilla
        return archive.read(f'assets/{ns}/{kind}/{path}'+('.json' if kind=='models' else '.png'))
    def resolve(identifier,seen=frozenset()):
        assert identifier not in seen,identifier
        model=json.loads(resource(identifier,'models'))
        inherited=resolve(model['parent'],seen|{identifier}) if 'parent' in model else {}
        return {**inherited,'textures':{**inherited.get('textures',{}),**model.get('textures',{})},'display':{**inherited.get('display',{}),**model.get('display',{})}}
    standard=resolve('minecraft:block/block')['display'];resolved=0
    for name in ['arcane_workbench','smelter_basic','smelter_thaumium','smelter_void']:
        model=resolve('thaumcraft:item/'+name)
        assert model['display']==standard,f'Nonstandard block hand/inventory transforms: {name}'
        for key,value in model['textures'].items():
            seen=set()
            while value.startswith('#'):
                assert value not in seen;seen.add(value);value=model['textures'][value[1:]]
            resource(value,'textures');resolved+=1
    for tier in ['basic','thaumium','void']:
        variants=json.loads(port.read(f'assets/thaumcraft/blockstates/smelter_{tier}.json'))['variants']
        assert len(variants)==8
        for direction,angle in [('north',0),('east',90),('south',180),('west',270)]:
            for state,suffix in [('true','on'),('false','off')]:
                variant=variants[f'facing={direction},lit={state}']
                assert variant['model']==f'thaumcraft:block/smelter_{tier}_{suffix}' and variant.get('y',0)==angle
    print(f'PASS: {len(models)} original model geometries/UVs; {len(textures)} byte-identical TC6 textures, including both GUI atlases')
    print(f'PASS: {resolved} item model texture references resolve against actual Minecraft 1.21.1; all four block display transforms match vanilla')
    print('PASS: all three smelter tiers have correct facing/lit variants; ordinary smelter bottom points to the current furnace_top texture')
    print('GPU rendering and multiplayer crafting remain unverified')
