#!/usr/bin/env python3
"""Validate imported media hashes, sound references and actual OGG decoding."""
import concurrent.futures,hashlib,json,pathlib,subprocess
root=pathlib.Path(__file__).resolve().parents[1];res=root/'src/main/resources';checked=0
for name in ['presentation-provenance.json','research-provenance.json','machine-provenance.json','essentia-provenance.json','essentia-extensions-provenance.json','essentia-refinements-provenance.json','visual-fixes-provenance.json','research-presentation-provenance.json']:
 for record in json.loads((root/'reference'/name).read_text())['assets']:
  if 'sha256' not in record:continue
  path=res/record['target'];assert hashlib.sha256(path.read_bytes()).hexdigest()==record['sha256'],path;checked+=1
sounds=json.loads((res/'assets/thaumcraft/sounds.json').read_text());references=set()
for key,event in sounds.items():
 assert 'category' not in event,key
 for entry in event['sounds']:
  name=entry if isinstance(entry,str) else entry['name']
  if isinstance(entry,dict) and entry.get('type')=='event':assert name.split(':')[-1] in sounds;continue
  assert name.startswith('thaumcraft:');path=res/'assets/thaumcraft/sounds'/(name.split(':')[1]+'.ogg');assert path.is_file(),path;references.add(path)
files=sorted((res/'assets/thaumcraft/sounds').rglob('*.ogg'))
def decode(path):
 result=subprocess.run(['ffmpeg','-v','error','-i',str(path),'-f','null','-'],capture_output=True,text=True)
 assert result.returncode==0 and not result.stderr,(path,result.stderr)
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(decode,files))
particles=list((res/'assets/thaumcraft/particles').glob('*.json'))
for path in particles:
 for sprite in json.loads(path.read_text())['textures']:
  assert (res/'assets/thaumcraft/textures/particle'/(sprite.split(':')[1]+'.png')).is_file(),sprite
print(f'PASS: {checked} media provenance hashes; {len(sounds)} sound events; {len(references)} referenced OGG files; {len(files)} OGG files decoded; {len(particles)} particle definitions')
