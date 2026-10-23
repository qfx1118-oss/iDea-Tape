#!/usr/bin/env python3
"""Build the open-source Android companion without a map SDK or API key."""
import argparse,os,subprocess,zipfile,shutil
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--jdk',required=True,type=Path)
p.add_argument('--platform',required=True,type=Path,help='Android 35 directory containing android.jar')
p.add_argument('--tools',required=True,type=Path,help='Android build-tools 35 directory')
p.add_argument('--keystore',required=True,type=Path,help='Private testing signing key outside versioned sources')
a=p.parse_args();root=Path(__file__).resolve().parents[1];src=root/'android/app/src/main';out=root/'build/android';out.mkdir(parents=True,exist_ok=True)
env=dict(os.environ,JAVA_HOME=str(a.jdk.resolve()));env['PATH']=str(a.jdk.resolve()/'bin')+os.pathsep+env['PATH']
def run(args):subprocess.run([str(s) for s in args],env=env,check=True)
for tool in ('aapt2','zipalign'):
 t=a.tools/tool;t.chmod(t.stat().st_mode|0o111)
generated=out/'generated';classes=out/'classes';dex=out/'dex'
for d in (generated,classes,dex):
 if d.exists():shutil.rmtree(d)
 d.mkdir()
run([a.tools/'aapt2','compile','--dir',src/'res','-o',out/'resources.zip'])
run([a.tools/'aapt2','link','-I',a.platform/'android.jar','--manifest',src/'AndroidManifest.xml','--java',generated,'-o',out/'base.apk',out/'resources.zip'])
sources=list((src/'java').rglob('*.java'))+list(generated.rglob('*.java'))
run([a.jdk/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-bootclasspath',str(a.platform/'android.jar')+os.pathsep+str(a.tools/'core-lambda-stubs.jar'),'-d',classes,*sources])
with zipfile.ZipFile(out/'classes.jar','w') as z:
 for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes))
run([a.jdk/'bin/java','-cp',a.tools/'lib/d8.jar','com.android.tools.r8.D8','--lib',a.platform/'android.jar','--min-api','26','--output',dex,out/'classes.jar'])
shutil.copyfile(out/'base.apk',out/'unsigned.apk')
with zipfile.ZipFile(out/'unsigned.apk','a') as z:
 for f in dex.glob('classes*.dex'):z.write(f,f.name)
run([a.tools/'zipalign','-P','16','-f','4',out/'unsigned.apk',out/'aligned.apk'])
# Standard local development key; no production or user credential is embedded.
if not a.keystore.exists():
 a.keystore.parent.mkdir(parents=True,exist_ok=True)
 run([a.jdk/'bin/keytool','-genkeypair','-keystore',a.keystore,'-storepass','android','-keypass','android','-alias','androiddebugkey','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=iDea Tape Development'])
 a.keystore.chmod(0o600)
run([a.jdk/'bin/java','-jar',a.tools/'lib/apksigner.jar','sign','--ks',a.keystore,'--ks-pass','pass:android','--key-pass','pass:android','--out',out/'idea-tape-android.apk',out/'aligned.apk'])
run([a.jdk/'bin/java','-jar',a.tools/'lib/apksigner.jar','verify','--verbose',out/'idea-tape-android.apk'])
print('Android APK: '+str(out/'idea-tape-android.apk'))
