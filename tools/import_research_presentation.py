#!/usr/bin/env python3
"""Import TC6 book/scan presentation verbatim. Run after the earlier importers."""
import argparse, hashlib, io, json, pathlib, re, zipfile
from PIL import Image

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--jar',type=pathlib.Path,required=True);args=parser.parse_args()
    root=pathlib.Path(__file__).resolve().parents[1];res=root/'src/main/resources';records=[];converted=[]
    def write(name,data):
        p=res/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
    def js(name,obj):write(name,(json.dumps(obj,ensure_ascii=False,indent=2)+'\n').encode())
    with zipfile.ZipFile(args.jar) as z:
        names=set(z.namelist())
        # Keep the original paths used inside research markup and item icon references.
        # These are presentation resources, not registrations of unfinished game content.
        for name in sorted(names):
            if name.startswith('assets/thaumcraft/textures/') and name.endswith(('.png','.jpg','.mcmeta')):
                data=z.read(name);write(name,data)
                records.append({'source':name,'target':name,'sha256':hashlib.sha256(data).hexdigest()})
        # NativeImage 1.21.1 validates a PNG signature before decoding: JPG is rejected.
        # Preserve the source JPG and create a lossless PNG from the decoded source pixels.
        for index in range(1,8):
            source=f'assets/thaumcraft/textures/gui/gui_research_back_{index}.jpg';target=source[:-4]+'.png'
            with Image.open(io.BytesIO(z.read(source))) as original:
                out=io.BytesIO();original.convert('RGB').save(out,format='PNG');data=out.getvalue()
            write(target,data);converted.append({'source':source,'target':target,'sha256':hashlib.sha256(data).hexdigest(),'conversion':'JPEG decoded RGB to lossless PNG; no resizing or repainting'})
        models={n[len('assets/thaumcraft/models/'):-5]:json.loads(z.read(n)) for n in names if n.startswith('assets/thaumcraft/models/') and n.endswith('.json')}
        states={n[len('assets/thaumcraft/blockstates/'):-5]:json.loads(z.read(n)) for n in names if n.startswith('assets/thaumcraft/blockstates/') and n.endswith('.json')}
        js('thaumcraft-port/legacy-book-models.json',{'models':models,'blockstates':states})
        for lang in ['en_us','ru_ru']:
            path=res/f'assets/thaumcraft/lang/{lang}.json';data=json.loads(path.read_text())
            for line in z.read(f'assets/thaumcraft/lang/{lang}.lang').decode('utf-8-sig').splitlines():
                if '=' in line and not line.lstrip().startswith('#'):
                    key,value=line.split('=',1);data.setdefault(key,value)
            data.pop('thaumcraft.port.journal.stage',None)
            ru=lang=='ru_ru'
            data.update({'thaumcraft.port.research.unavailable':'Исследование пока недоступно' if ru else 'Research is not yet available',
                'thaumcraft.port.recipe.unavailable':'Рецепт ещё не перенесён' if ru else 'Recipe not yet ported',
                'item.thaumcraft.scribing_tools':data.get('item.scribing_tools.name','Scribing Tools'),
                'item.thaumcraft.celestial_notes':data.get('item.celestial_notes.name','Celestial Notes')})
            js(f'assets/thaumcraft/lang/{lang}.json',data)
        # Forge 1.12 TRSR translations are in block units; modern display uses sixteenths.
        legacy=json.loads(z.read('assets/thaumcraft/blockstates/thaumometer.json'))['variants']['normal'][0]['transform']
        display={}
        for context,tr in legacy.items():
            context={'firstperson':'firstperson_righthand','thirdperson':'thirdperson_righthand'}.get(context,context)
            rotation=[0,0,0]
            for step in tr.get('rotation',[]):
                for axis,value in step.items():rotation['xyz'.index(axis)]=value
            if tr.get('rotation')==[{'y':90},{'x':90}]:rotation=[90,0,-90]
            scale=tr.get('scale',1)
            display[context]={'rotation':rotation,'translation':[v*16 for v in tr.get('translation',[0,0,0])],'scale':[scale]*3 if isinstance(scale,(float,int)) else scale}
        js('assets/thaumcraft/models/item/thaumometer.json',{'loader':'neoforge:obj','model':'thaumcraft:models/item/scanner.obj','flip_v':True,'automatic_culling':False,'render_type':'minecraft:translucent','textures':{'particle':'thaumcraft:item/scanner'},'display':display})
        for n in ['scanner.png','scanscreen.png','scanscreen.png.mcmeta']:
            write('assets/thaumcraft/textures/item/'+n,z.read('assets/thaumcraft/textures/items/'+n))
        for kind in ['scan_rune','scan_spark']:
            js(f'assets/thaumcraft/particles/{kind}.json',{'textures':['thaumcraft:legacy']})
        for name in ['scribing_tools','celestial_notes']:
            if name=='scribing_tools':
                js(f'assets/thaumcraft/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:items/scribing_tools'}})
            else:
                variants=['sun']+['stars'+str(i) for i in range(1,5)]+['moon'+str(i) for i in range(1,9)]
                model={'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:items/celestial/sheet','layer1':'thaumcraft:items/celestial/sun'},'overrides':[]}
                for i,v in enumerate(variants):
                    model['overrides'].append({'predicate':{'thaumcraft:celestial':i},'model':'thaumcraft:item/variants/celestial_'+v})
                    js('assets/thaumcraft/models/item/variants/celestial_'+v+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:items/celestial/sheet','layer1':'thaumcraft:items/celestial/'+v}})
                js(f'assets/thaumcraft/models/item/{name}.json',model)
    (root/'reference/research-presentation-provenance.json').write_text(json.dumps({'source_sha256':hashlib.sha256(args.jar.read_bytes()).hexdigest(),'assets':records,'converted_backgrounds':converted,'thaumometer_display_source':'assets/thaumcraft/blockstates/thaumometer.json','translation_conversion':16},indent=2)+'\n')
    print('Imported',len(records),'unchanged original presentation resources and scanner transforms')

if __name__=='__main__':main()
