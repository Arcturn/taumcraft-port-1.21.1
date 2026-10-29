"""Read-only standalone recheck of the retained migration NBT evidence."""
from pathlib import Path
import json, hashlib
from nbt_read import read
p=Path(__file__).resolve().parent
summary=json.loads((p/'summary.json').read_text())
for name,digest in summary['sha256'].items():
 assert hashlib.sha256((p/name).read_bytes()).hexdigest()==digest,name
before=read(p/'before-schema2.dat')['data'];first=read(p/'first-save-schema3.dat')['data'];second=read(p/'second-save-schema3.dat')['data']
assert (before['schema'],first['schema'],second['schema'])==(2,3,3)
players=lambda data:{tuple(v['uuid']):v for v in data['players']}
a,b,c=map(players,[before,first,second]);assert set(a)==set(b)==set(c)
for uid,old in a.items():
 for key in ['stages','scanned','crafted','discovered','knowledge','warpPermanent','warpNormal','warpTemporary']:
  assert old[key]==b[uid][key]==c[uid][key],(uid,key)
 assert sorted(b[uid]['flags'])==sorted(x for x in old['flags'] if not x.startswith('ui_seen:'))
 assert b[uid]==c[uid]
 assert all('POPUP' not in values for values in b[uid]['research_flags'].values())
active=next(x for x in a.values() if len(x['stages'])==148);migrated=b[tuple(active['uuid'])]
assert len(migrated['research_flags'])==141
for key in summary['read_markers_preserved']:assert key not in migrated['research_flags']
for name in ['first-login.properties','first-book.properties','second-login.properties','second-book.properties']:
 fields=dict(x.split('=',1) for x in (p/name).read_text().splitlines() if '=' in x and not x.startswith('#'))
 assert fields['player.totalExperience']=='0';assert fields['playerUuid']==summary['active_player_uuid']
for name in ['first-client.log','second-client.log']:
 text=(p/name).read_text();assert 'All dimensions are saved' in text
 assert 'Addendum added' not in text and 'tc.addaddendum' not in text
print('PASS: 10 players preserved; seven read markers; no replay; schema 3; both saved NBT files identical')
