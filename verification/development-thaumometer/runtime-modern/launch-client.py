#!/usr/bin/env python3
"""Launch an isolated client from the unchanged ModDev-generated arguments, with disposable probes."""
import json
import hashlib
import os
from pathlib import Path
import shlex
import shutil
import subprocess
import sys
import time
import zipfile

ROOT = Path(__file__).resolve().parent
WORKSPACE = ROOT.parents[2]
PROJECT = WORKSPACE / 'work/dust-animation/thaumcraft-port'
SUPPORT = WORKSPACE / 'work/dust-animation/build-support'
SYSROOT = ROOT.parent / 'runtime-original/sysroot'
PROFILE = ROOT / 'profile'
PROBE = ROOT / 'probe'
JAVA = SUPPORT / 'jdk-21.0.13+9/bin/java'
JAVAC = SUPPORT / 'jdk-21.0.13+9/bin/javac'

def args(path):
    return shlex.split(path.read_text(), comments=True)

def main():
    PROFILE.mkdir(exist_ok=True)
    mods = PROFILE / 'mods'
    mods.mkdir(exist_ok=True)
    classes = PROBE / 'classes'
    classes.mkdir(exist_ok=True)
    compile_paths = (PROJECT / 'build/moddev/clientLegacyClasspath.txt').read_text().splitlines()
    compile_paths += [str(PROJECT / 'build/moddev/artifacts/neoforge-21.1.259.jar'),
                      str(PROJECT / 'build/classes/java/main'), str(PROJECT / 'build/resources/main')]
    subprocess.run([str(JAVAC), '-proc:none', '-cp', os.pathsep.join(compile_paths), '-d', str(classes),
                    *[str(p) for p in PROBE.glob('*.java')]], check=True)
    probe_jar = mods / 'thaumcraft-tooltip-probe.jar'
    with zipfile.ZipFile(probe_jar, 'w', zipfile.ZIP_DEFLATED) as jar:
        jar.write(PROBE / 'neoforge.mods.toml', 'META-INF/neoforge.mods.toml')
        for path in classes.rglob('*.class'):
            jar.write(path, path.relative_to(classes))
    cache = SUPPORT / 'gradle-home/caches/modules-2/files-2.1'
    curios = next((cache / 'top.theillusivec4.curios').rglob('curios-neoforge-9.5.1+1.21.1.jar'))
    for dependency in [curios, PROJECT / 'libs/TerraBlender-neoforge-1.21.1-4.1.0.8.jar']:
        shutil.copy2(dependency, mods / dependency.name)
    options = PROFILE / 'options.txt'
    if not options.exists():
        options.write_text('version:3955\nrenderDistance:2\nsimulationDistance:5\nmaxFps:30\nguiScale:2\n'
                           'enableVsync:false\nonboardAccessibility:false\ntutorialStep:none\n'
                           'soundCategory_master:0.0\n')
    vm = args(PROJECT / 'build/moddev/clientRunVmArgs.txt')
    program = args(PROJECT / 'build/moddev/clientRunProgramArgs.txt')
    program[program.index('--gameDir') + 1] = str(PROFILE)
    program += ['--username', 'ThaumometerTest', '--uuid', '00000000000000000000000000000917',
                '--accessToken', '0', '--userType', 'legacy', '--width', '1280', '--height', '720']
    environment = dict(os.environ)
    environment['LD_LIBRARY_PATH'] = str(SYSROOT / 'usr/lib/x86_64-linux-gnu')
    environment['DISPLAY'] = '127.0.0.1:92'
    environment['LIBGL_ALWAYS_SOFTWARE'] = '1'
    environment['ALSOFT_DRIVERS'] = 'null'
    environment['MOD_CLASSES'] = os.pathsep.join(['thaumcraft%%' + str(PROJECT / 'build/classes/java/main'),
                                                'thaumcraft%%' + str(PROJECT / 'build/resources/main')])
    vm += ['-Xms256M', '-Xmx1400M', '-XX:ActiveProcessorCount=2',
           '-Dthaumcraft.probe.automate=true', '-Dthaumcraft.probe.scanner=true',
           '-Dthaumcraft.probe.output=' + os.environ.get('THAUMCRAFT_PROBE_OUTPUT', str(ROOT / 'evidence')),
           '-Dthaumcraft.probe.xdotool=' + str(SYSROOT / 'usr/bin/xdotool')]
    command = [str(JAVA), *vm, '-cp', os.pathsep.join(compile_paths), *program]
    inputs = {}
    for name, directory in [('compiled_classes', PROJECT / 'build/classes/java/main'),
                            ('processed_resources', PROJECT / 'build/resources/main')]:
        files = sorted(path for path in directory.rglob('*') if path.is_file())
        digest = hashlib.sha256()
        for path in files:
            digest.update(path.relative_to(directory).as_posix().encode() + b'\0')
            digest.update(hashlib.sha256(path.read_bytes()).digest())
        inputs[name] = {'file_count': len(files), 'ordered_paths_and_file_sha256': digest.hexdigest()}
    inputs['probe_sources'] = {path.name: hashlib.sha256(path.read_bytes()).hexdigest()
                              for path in sorted(PROBE.glob('*.java'))}
    (ROOT / 'runtime-inputs.json').write_text(json.dumps(inputs, indent=2) + '\n')
    (ROOT / 'launch-command.json').write_text(json.dumps({'args': command, 'MOD_CLASSES': environment['MOD_CLASSES'],
                                                        'scope': 'isolated local test profile'}, indent=2) + '\n')
    with (ROOT / 'xvfb.log').open('w') as xlog, (ROOT / 'client.log').open('w') as log:
        xvfb = subprocess.Popen([str(SYSROOT / 'usr/bin/Xvfb'), ':92', '-screen', '0', '1280x720x24',
                                 '-ac', '-nolisten', 'unix', '-listen', 'tcp'], env=environment, stdout=xlog, stderr=subprocess.STDOUT)
        try:
            time.sleep(1)
            if xvfb.poll() is not None:
                raise RuntimeError('Xvfb exited before client startup; inspect xvfb.log')
            client = subprocess.Popen(command, cwd=PROFILE, env=environment, stdout=log, stderr=subprocess.STDOUT)
            (ROOT / 'pids.json').write_text(json.dumps({'java': client.pid, 'xvfb': xvfb.pid}))
            result = client.wait(timeout=900)
            print('Modern client exit:', result)
            return result
        finally:
            if 'client' in locals() and client.poll() is None:
                client.terminate()
            xvfb.terminate()
            xvfb.wait(timeout=15)

if __name__ == '__main__':
    sys.exit(main())
