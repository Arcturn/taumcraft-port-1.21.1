#!/usr/bin/env python3
"""Run the unchanged project using workspace-owned JDK/Gradle/proxy settings."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.parse

support = Path(__file__).resolve().parent
project = support.parent / 'thaumcraft-port'
jdk = support / 'jdk-21.0.13+9'
gradle_home = support / 'gradle-home'
gradle_home.mkdir(exist_ok=True)
truststore = support / 'truststore.jks'
shutil.copyfile('/etc/ssl/certs/java/cacerts', truststore)
certificate = os.environ.get('CODEX_PROXY_CERT')
if certificate and Path(certificate).is_file():
    result = subprocess.run([str(jdk/'bin/keytool'), '-importcert', '-noprompt', '-alias', 'codex-current-proxy', '-file', certificate, '-keystore', str(truststore), '-storepass', 'changeit'], capture_output=True, text=True)
    if result.returncode:
        raise SystemExit('Could not prepare local JVM proxy truststore: ' + result.stderr)
properties = ['org.gradle.internal.http.connectionTimeout=30000', 'org.gradle.internal.http.socketTimeout=60000', f'systemProp.javax.net.ssl.trustStore={truststore}', 'org.gradle.workers.max=2']
for scheme in ['http', 'https']:
    proxy = urllib.parse.urlsplit(os.environ.get(scheme+'_proxy') or os.environ.get(scheme.upper()+'_PROXY', ''))
    if proxy.hostname:
        properties.extend([f'systemProp.{scheme}.proxyHost={proxy.hostname}', f'systemProp.{scheme}.proxyPort={proxy.port or 80}'])
(gradle_home/'gradle.properties').write_text('\n'.join(properties)+'\n')
environment = dict(os.environ, JAVA_HOME=str(jdk), GRADLE_USER_HOME=str(gradle_home))
environment['PATH'] = str(jdk/'bin') + os.pathsep + environment.get('PATH', '')
environment['JAVA_TOOL_OPTIONS'] = ' '.join([os.environ.get('JAVA_TOOL_OPTIONS', ''), *['-D'+prop.removeprefix('systemProp.') for prop in properties if prop.startswith('systemProp.')]]).strip()
init = ['--init-script', str(support/'runtime.init.gradle')] if (support/'runtime.init.gradle').is_file() else []
sys.exit(subprocess.call(['bash', str(support/'gradle-9.2.1/bin/gradle'), '--no-daemon', '--console=plain', *init, *sys.argv[1:]], cwd=project, env=environment))
