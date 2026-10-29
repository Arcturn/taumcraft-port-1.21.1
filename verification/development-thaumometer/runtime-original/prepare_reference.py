import concurrent.futures, hashlib, json, shutil, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
WORKSPACE = ROOT.parents[2]
MC = json.loads((WORKSPACE/'work/worldgen-audit/original/minecraft-1.12.2-manifest.json').read_text())
FORGE = json.loads((ROOT/'version.json').read_text())
LIBS = ROOT/'libraries'
ASSETS = ROOT/'assets'
NATIVES = ROOT/'natives'
for p in (LIBS, ASSETS/'indexes', ASSETS/'objects', NATIVES, ROOT/'game/mods'):
    p.mkdir(parents=True, exist_ok=True)

def fetch(path, urls, sha1):
    if path.exists() and hashlib.sha1(path.read_bytes()).hexdigest() == sha1:
        return
    failures = []
    for url in urls:
        try:
            with urllib.request.urlopen(url, timeout=40) as response:
                data = response.read()
            actual = hashlib.sha1(data).hexdigest()
            if actual != sha1:
                raise ValueError('SHA1 mismatch: '+actual)
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
            return
        except Exception as exc:
            failures.append(str(exc))
    raise RuntimeError(str(path)+': '+repr(failures))

def allowed(lib):
    if 'rules' not in lib:
        return True
    permit = False
    for rule in lib['rules']:
        if 'os' not in rule or rule['os'].get('name') == 'linux':
            permit = rule['action'] == 'allow'
    return permit

selected = {}
for lib in MC['libraries']+FORGE['libraries']:
    if allowed(lib):
        key = ':'.join(lib['name'].split(':')[:2])
        selected[key] = lib
jobs=[]
classpath=[]
native_archives=[]
for lib in selected.values():
    downloads=lib['downloads']
    entries=[]
    if 'artifact' in downloads:
        entries.append((downloads['artifact'],False))
    if 'linux' in lib.get('natives',{}):
        entries.append((downloads['classifiers'][lib['natives']['linux']],True))
    for artifact,is_native in entries:
        relative=artifact['path'];path=LIBS/relative
        if lib['name'].startswith('net.minecraftforge:forge:'):
            shutil.copy2(ROOT/'forge-1.12.2-14.23.5.2860-universal.jar',path.parent.mkdir(parents=True,exist_ok=True) or path)
        urls=[artifact['url']]
        if 'maven.minecraftforge.net' in urls[0]:
            urls=['https://repo.maven.apache.org/maven2/'+relative,'https://bmclapi2.bangbang93.com/maven/'+relative]
        jobs.append((path,urls,artifact['sha1']))
        (native_archives if is_native else classpath).append(path)
with concurrent.futures.ThreadPoolExecutor(max_workers=12) as executor:
    results=list(executor.map(lambda args:fetch(*args),jobs))
print('LIBRARIES verified:',len(jobs),flush=True)
for path in native_archives:
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            if name.endswith('.so'):
                (NATIVES/Path(name).name).write_bytes(archive.read(name))

index=MC['assetIndex'];index_path=ASSETS/'indexes'/('1.12.json')
fetch(index_path,[index['url']],index['sha1'])
objects=json.loads(index_path.read_text())['objects']
asset_jobs=[]
for value in objects.values():
    sha=value['hash'];path=ASSETS/'objects'/sha[:2]/sha
    asset_jobs.append((path,['https://resources.download.minecraft.net/'+sha[:2]+'/'+sha],sha))
with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
    for i,_ in enumerate(executor.map(lambda args:fetch(*args),asset_jobs),1):
        if i%250==0:print('ASSETS',i,'/',len(asset_jobs),flush=True)

mc_path=ROOT/'minecraft-1.12.2.jar'
shutil.copy2(WORKSPACE/'work/worldgen-audit/original/minecraft-1.12.2-client.jar',mc_path)
assert hashlib.sha1(mc_path.read_bytes()).hexdigest()==MC['downloads']['client']['sha1']
classpath.append(mc_path)
for name in ['Thaumcraft-1.12.2-6.1.BETA26.jar','Baubles-1.12-1.5.2.jar']:
    source=ROOT/'downloads'/name if name.startswith('Thaumcraft') else ROOT/name
    shutil.copy2(source,ROOT/'game/mods'/name)
(ROOT/'classpath.txt').write_text(':'.join(str(p) for p in classpath)+'\n')
(ROOT/'setup-manifest.json').write_text(json.dumps({'minecraft':'1.12.2','forge':'14.23.5.2860','thaumcraft':'6.1.BETA26','baubles':'1.5.2','library_count':len(jobs),'asset_count':len(asset_jobs),'all_manifest_sha1_verified':True,'forge_official_sha1_source':'https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html','original_mod_sha256':hashlib.sha256((ROOT/'game/mods/Thaumcraft-1.12.2-6.1.BETA26.jar').read_bytes()).hexdigest()},indent=2)+'\n')
print('REFERENCE SETUP COMPLETE',flush=True)
