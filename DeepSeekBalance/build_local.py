#!/usr/bin/env python3
"""Offline APK builder: Android SDK 35, build-tools 35, Java 17, and ECJ (if no javac)."""
import os, pathlib, shutil, subprocess, secrets, zipfile
root=pathlib.Path(__file__).resolve().parent
sdk=pathlib.Path(os.environ['ANDROID_SDK_ROOT'])
bt=sdk/'build-tools'/os.environ.get('BUILD_TOOLS_VERSION','35.0.0')
android=sdk/'platforms/android-35/android.jar'
build=root/'build-local'; build.mkdir(exist_ok=True)
for name in ['gen','classes','dex']:
    path=build/name
    if path.exists(): shutil.rmtree(path)
    path.mkdir()
def run(*cmd): subprocess.run([str(x) for x in cmd],check=True,cwd=root)
main=root/'app/src/main'
run(bt/'aapt2','compile','--dir',main/'res','-o',build/'resources.zip')
manifest=build/'AndroidManifest.xml'
manifest.write_text((main/'AndroidManifest.xml').read_text().replace('<manifest ', '<manifest package="cn.balance.deepseek" ', 1))
run(bt/'aapt2','link','-o',build/'base.apk','--manifest',manifest,'-I',android,'--java',build/'gen','--min-sdk-version','26','--target-sdk-version','35',build/'resources.zip')
sources=sorted((main/'java').rglob('*.java'))+sorted((build/'gen').rglob('*.java'))
if shutil.which('javac'):
    run('javac','-source','8','-target','8','-encoding','UTF-8','-classpath',android,'-d',build/'classes',*sources)
else:
    run('java','-jar',os.environ['ECJ_JAR'],'-8','-encoding','UTF-8','-classpath',android,'-d',build/'classes',*sources)
classes=build/'classes.jar'
with zipfile.ZipFile(classes,'w') as z:
    for file in sorted((build/'classes').rglob('*.class')): z.write(file,file.relative_to(build/'classes'))
run(bt/'d8','--lib',android,'--min-api','26','--output',build/'dex',classes)
shutil.copyfile(build/'base.apk',build/'unsigned.apk')
with zipfile.ZipFile(build/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
    for file in sorted((build/'dex').glob('*.dex')):z.write(file,file.name)
run(bt/'zipalign','-f','4',build/'unsigned.apk',build/'aligned.apk')
sign=root/'.signing';sign.mkdir(mode=0o700,exist_ok=True)
password=sign/'password.txt'
if not password.exists(): password.write_text(secrets.token_urlsafe(32));password.chmod(0o600)
keystore=sign/'release.p12'
if not keystore.exists():
    run('keytool','-genkeypair','-keystore',keystore,'-storetype','PKCS12','-storepass:file',password,'-keypass:file',password,'-alias','balance','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=DeepSeek Balance Personal App, O=Personal Tools, C=CN')
out=root.parent/'deliverables/DeepSeekBalance-1.0.0.apk';out.parent.mkdir(exist_ok=True)
run(bt/'apksigner','sign','--ks',keystore,'--ks-key-alias','balance','--ks-pass','file:'+str(password),'--out',out,build/'aligned.apk')
run(bt/'apksigner','verify','--verbose',out)
print(out)
