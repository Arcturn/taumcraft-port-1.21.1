#!/usr/bin/env python3
"""Preserve original research definitions, text and artwork without inventing unlocks."""
import argparse, hashlib, json, pathlib, re, zipfile
p=argparse.ArgumentParser();p.add_argument('--jar',type=pathlib.Path,required=True);p.add_argument('--sources',type=pathlib.Path,required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];res=root/'src/main/resources';records=[]
with zipfile.ZipFile(a.jar) as z:
    for name in z.namelist():
        if (name.startswith('assets/thaumcraft/research/') and name.endswith('.json')) or (name.startswith('assets/thaumcraft/textures/research/') and name.endswith('.png')) or (name.startswith('assets/thaumcraft/textures/gui/gui_research') and name.endswith(('.png','.jpg'))):
            data=z.read(name);path=res/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
            records.append({'source':name,'target':name,'sha256':hashlib.sha256(data).hexdigest()})
    for lang in ['en_us','ru_ru']:
        path=res/f'assets/thaumcraft/lang/{lang}.json';data=json.loads(path.read_text())
        for line in z.read(f'assets/thaumcraft/lang/{lang}.lang').decode('utf-8-sig').splitlines():
            if line.strip().startswith('#') or '=' not in line:continue
            key,val=line.split('=',1)
            if key.startswith(('research.','tc.research_category.','tc.research_name.','tc.knowledge.')):data.setdefault(key,val)
        path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
text=(a.sources/'thaumcraft/common/config/ConfigResearch.java').read_text()
formula={}
for cat,expr in re.findall(r'registerCategory\("([A-Z]+)",.*?new AspectList\(\)(.*?), new ResourceLocation',text):
    formula[cat]={asp:int(n) for asp,n in re.findall(r'\.add\(Aspect\.(\w+), (\d+)\)',expr)}
(res/'thaumcraft-port/research-formulas.json').write_text(json.dumps(formula,indent=2)+'\n')
(root/'reference/research-provenance.json').write_text(json.dumps({'assets':records,'source_sha256':hashlib.sha256(a.jar.read_bytes()).hexdigest()},indent=2)+'\n')
print('Imported',len(records),'research resources;',len(formula),'knowledge formulas')
