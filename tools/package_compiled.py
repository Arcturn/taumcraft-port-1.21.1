#!/usr/bin/env python3
"""Package existing javac output. Does not compile code or replace Gradle's dependency setup."""
import argparse
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--classes', type=Path, required=True)
    args = parser.parse_args()
    props = dict(line.split('=', 1) for line in (ROOT / 'gradle.properties').read_text().splitlines()
                 if '=' in line and not line.startswith('#'))
    version = props['mod_version']
    output = ROOT / 'dist' / f'thaumcraft-port-1.21.1-{version}.jar'
    output.parent.mkdir(exist_ok=True)
    classes = list(args.classes.rglob('*.class'))
    assert classes, 'Compile the project before packaging'
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as jar:
        jar.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\r\nImplementation-Title: Thaumcraft Experimental Port\r\n'
                     f'Implementation-Version: {version}\r\n\r\n')
        for file in sorted(classes):
            jar.write(file, file.relative_to(args.classes).as_posix())
        for library in sorted((ROOT / 'libs').glob('*.jar')):
            jar.write(library, 'META-INF/jarjar/' + library.name)
        resources = ROOT / 'src/main/resources'
        for file in sorted(resources.rglob('*')):
            if file.is_file():
                raw = file.read_bytes()
                if file.name == 'neoforge.mods.toml':
                    raw = raw.replace(b'${version}', version.encode())
                jar.writestr(file.relative_to(resources).as_posix(), raw)
    print(output)

if __name__ == '__main__':
    main()
