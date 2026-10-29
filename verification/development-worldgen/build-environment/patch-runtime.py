#!/usr/bin/env python3
"""Container-only NeoFormRuntime fallback; does not patch game or mod classes."""
from pathlib import Path
import subprocess
import zipfile

support = Path(__file__).resolve().parent
jar = next((support/'gradle-home').rglob('neoform-runtime-2.0.31-all.jar'))
annotations = next((support/'gradle-home').rglob('annotations-24.1.0.jar'))
with zipfile.ZipFile(support/'neoform-runtime-2.0.31-sources.jar') as source_archive:
    source = source_archive.read('net/neoforged/neoform/runtime/engine/NeoFormEngine.java').decode()
    external_source = source_archive.read('net/neoforged/neoform/runtime/actions/ExternalJavaToolAction.java').decode()
original = 'this.javaExecutable = ProcessHandle.current()\n                .info()\n                .command()\n                .orElseThrow();'
replacement = 'this.javaExecutable = ProcessHandle.current()\n                .info()\n                .command()\n                .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());'
assert source.count(original) == 1
source_path = support/'runtime-patch/src/net/neoforged/neoform/runtime/engine/NeoFormEngine.java'
source_path.parent.mkdir(parents=True, exist_ok=True)
source_path.write_text(source.replace(original, replacement))
external_original = 'javaExecutablePath = ProcessHandle.current()\n                    .info()\n                    .command()\n                    .orElseThrow();'
external_replacement = 'javaExecutablePath = ProcessHandle.current()\n                    .info()\n                    .command()\n                    .orElse(java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString());'
assert external_source.count(external_original) == 1
external_path = support/'runtime-patch/src/net/neoforged/neoform/runtime/actions/ExternalJavaToolAction.java'
external_path.parent.mkdir(parents=True, exist_ok=True)
external_path.write_text(external_source.replace(external_original, external_replacement))
classes = support/'runtime-patch/classes'
classes.mkdir(parents=True, exist_ok=True)
subprocess.run([str(support/'jdk-21.0.13+9/bin/javac'), '-cp', str(jar)+':'+str(annotations),
                '-d', str(classes), str(source_path), str(external_path)], check=True)
replacements = {file.relative_to(classes).as_posix(): file.read_bytes() for file in classes.rglob('*.class')}
with zipfile.ZipFile(jar) as original_jar, zipfile.ZipFile(support/'neoform-runtime-2.0.31-local.jar', 'w', zipfile.ZIP_DEFLATED) as output:
    for info in original_jar.infolist():
        output.writestr(info, replacements.pop(info.filename, original_jar.read(info)))
    for name, raw in replacements.items():
        output.writestr(name, raw)
print('Patched workspace NeoFormRuntime only')
