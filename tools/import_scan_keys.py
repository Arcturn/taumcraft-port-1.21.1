#!/usr/bin/env python3
"""Recover unchanged enchantment/effect translation keys from Mojang's two client archives.
English names are used only as an unambiguous join; runtime uses the original keys.
"""
import argparse,hashlib,json,pathlib,zipfile
p=argparse.ArgumentParser();p.add_argument('--legacy-client',type=pathlib.Path,required=True);p.add_argument('--modern-client',type=pathlib.Path,required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]
with zipfile.ZipFile(a.legacy_client) as oldzip,zipfile.ZipFile(a.modern_client) as newzip:
 raw=oldzip.read('assets/minecraft/lang/en_us.lang')
 old=dict(l.split('=',1) for l in raw.decode().splitlines() if '=' in l and not l.startswith('#'))
 new=json.loads(newzip.read('assets/minecraft/lang/en_us.json'))
 enchantments={'minecraft:'+n.removeprefix('data/minecraft/enchantment/').removesuffix('.json') for n in newzip.namelist() if n.startswith('data/minecraft/enchantment/') and n.endswith('.json')}
 result={}; evidence={}
 for family,prefix,modernprefix in [('enchantments','enchantment.','enchantment.minecraft.'),('effects','effect.','effect.minecraft.')]:
  result[family]={};evidence[family]=[]
  for key,value in new.items():
   if not key.startswith(modernprefix) or key.endswith('.desc'):continue
   target='minecraft:'+key[len(modernprefix):]
   if family=='enchantments' and target not in enchantments:continue
   match=[k for k,v in old.items() if k.startswith(prefix) and v==value]
   if len(match)==1:
    result[family][target]=match[0];evidence[family].append({'id':target,'legacy_key':match[0],'english_name':value})
 assert len(result['enchantments'])==30 and len(result['effects'])==27
 assert any(n.endswith('.class') and b'effect.moveSpeed' in oldzip.read(n) for n in oldzip.namelist())
(root/'src/main/resources/thaumcraft-port/scan-keys.json').write_text(json.dumps(result,indent=2)+'\n')
(root/'reference/scan-key-migration.json').write_text(json.dumps({'legacy_client_sha1':hashlib.sha1(a.legacy_client.read_bytes()).hexdigest(),'modern_client_sha1':hashlib.sha1(a.modern_client.read_bytes()).hexdigest(),'legacy_client_source':'Mojang 1.12.2 version manifest, downloads.client','source_api':['thaumcraft/common/lib/research/ScanEnchantment.java:getResearchKey','thaumcraft/common/lib/research/ScanPotion.java:getResearchKey'],'mappings':evidence},indent=2)+'\n')
print({k:len(v) for k,v in result.items()})
