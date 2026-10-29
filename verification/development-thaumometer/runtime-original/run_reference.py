import json, os, subprocess, time, uuid, threading
from pathlib import Path

ROOT=Path(__file__).resolve().parent
SYS=ROOT/'sysroot'
ENV=os.environ.copy()
ENV.update(DISPLAY='127.0.0.1:91',LD_LIBRARY_PATH=str(SYS/'usr/lib/x86_64-linux-gnu'),LIBGL_ALWAYS_SOFTWARE='1',ALSOFT_DRIVERS='null')
ENV['PATH']=str(SYS/'usr/bin')+':'+ENV['PATH']
game=ROOT/'game'
(game/'options.txt').write_text('lang:en_us\nguiScale:2\nfullscreen:false\nrenderDistance:4\nmaxFps:30\nenableVsync:false\nmipmapLevels:0\npauseOnLostFocus:false\nsoundCategory_master:1.0\n')
for name in ('ready','command.txt','ui-command.json','ui-result.json'):(ROOT/name).unlink(missing_ok=True)
xf=open(ROOT/'xvfb.log','w')
xserver=subprocess.Popen([str(SYS/'usr/bin/Xvfb'),':91','-screen','0','1280x720x24','-ac','-nolisten','unix','-listen','tcp'],env=ENV,stdout=xf,stderr=xf)
time.sleep(.7)
args=[str(SYS/'usr/lib/jvm/java-8-openjdk-amd64/bin/java'),'-Xmx1024m','-XX:ActiveProcessorCount=2','-Djavax.accessibility.assistive_technologies=','-Djava.library.path='+str(ROOT/'natives'),'-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true','-Dtcprobe.root='+str(ROOT),'-cp',(ROOT/'classpath.txt').read_text().strip(),'net.minecraft.launchwrapper.Launch','--username','Reference','--version','1.12.2-forge-14.23.5.2860','--gameDir',str(game),'--assetsDir',str(ROOT/'assets'),'--assetIndex','1.12','--uuid',uuid.uuid3(uuid.NAMESPACE_DNS,'OfflinePlayer:Reference').hex,'--accessToken','0','--userType','legacy','--tweakClass','net.minecraftforge.fml.common.launcher.FMLTweaker','--versionType','Forge','--width','1280','--height','720']
log=open(ROOT/'client.log','w')
client=subprocess.Popen(args,env=ENV,cwd=game,stdout=log,stderr=log)
print('REFERENCE_CLIENT_STARTED',flush=True)
try:
    deadline=time.monotonic()+3600
    while client.poll() is None and time.monotonic()<deadline:
        command=ROOT/'ui-command.json'
        if command.exists():
            request=json.loads(command.read_text());command.unlink();result={'id':request.get('id'),'action':request['action']}
            try:
                if request['action']=='xdotool':
                    p=subprocess.run([str(SYS/'usr/bin/xdotool')]+request['args'],env=ENV,capture_output=True,text=True,timeout=12)
                    result.update(exit_code=p.returncode,stdout=p.stdout,stderr=p.stderr)
                elif request['action']=='screenshot':
                    path=ROOT/request['name'];p=subprocess.run(['ffmpeg','-y','-loglevel','error','-f','x11grab','-video_size','1280x720','-i',ENV['DISPLAY'],'-frames:v','1',str(path)],env=ENV,capture_output=True,text=True,timeout=20)
                    result.update(exit_code=p.returncode,path=str(path),stderr=p.stderr)
                elif request['action']=='video':
                    if 'inputs' in request:
                        def trigger(inputs=request['inputs']):
                            for entry in inputs:
                                time.sleep(entry.get('delay',0))
                                subprocess.run([str(SYS/'usr/bin/xdotool')]+entry['args'],env=ENV,timeout=10)
                        threading.Thread(target=trigger).start()
                    path=ROOT/request['name'];p=subprocess.run(['ffmpeg','-y','-loglevel','error','-f','x11grab','-framerate','30','-video_size','1280x720','-i',ENV['DISPLAY'],'-t',str(request.get('seconds',3)),'-c:v','libx264','-pix_fmt','yuv420p',str(path)],env=ENV,capture_output=True,text=True,timeout=30)
                    result.update(exit_code=p.returncode,path=str(path),stderr=p.stderr)
                elif request['action']=='probe':
                    (ROOT/'command.txt').write_text(request['command']);result['exit_code']=0
                elif request['action']=='quit':
                    (ROOT/'command.txt').write_text('quit');result['exit_code']=0
            except Exception as exc:result['error']=str(exc)
            (ROOT/'ui-result.json').write_text(json.dumps(result,indent=2)+'\n')
            print('REFERENCE_UI',json.dumps(result),flush=True)
        time.sleep(.1)
finally:
    if client.poll() is None:client.terminate()
    client.wait(timeout=20)
    xserver.terminate();xserver.wait(timeout=10)
    (ROOT/'client-exit.json').write_text(json.dumps({'client_exit_code':client.returncode})+'\n')
    print('REFERENCE_CLIENT_EXIT',client.returncode,flush=True)
