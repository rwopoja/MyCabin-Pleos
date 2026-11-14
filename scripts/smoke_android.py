"""Install the EV prototype on a standard Android emulator. Not a Pleos test."""
import json,pathlib,re,subprocess,time,xml.etree.ElementTree as ET
OUT=pathlib.Path('runtime-artifacts');OUT.mkdir(exist_ok=True)
def adb(*args):return subprocess.run(['adb',*args],capture_output=True,text=True,check=True).stdout
def tree():
 adb('shell','uiautomator','dump','/sdcard/tree.xml')
 return ET.fromstring(adb('shell','cat','/sdcard/tree.xml'))
def shot(name):
 with (OUT/(name+'.png')).open('wb') as f:subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
adb('shell','wm','size','1920x1080');adb('shell','wm','density','160')
adb('install','-r','apk/김재윤_마이캐빈_release.apk')
adb('shell','am','start','-W','-n','kr.mycabin.app/.MainActivity')
for i in range(18):
 time.sleep(4);root=tree()
 if '계획 계산하기' in ET.tostring(root,encoding='unicode'):break
else: raise AssertionError('EV planning screen not loaded')
shot('01-ev-home')
for node in root.iter('node'):
 if '계획 계산하기' in node.get('text',''):
  x1,y1,x2,y2=map(int,re.findall(r'\\d+',node.get('bounds','')))
  adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));break
else: raise AssertionError('Calculate button not found')
time.sleep(3);result=ET.tostring(tree(),encoding='unicode');(OUT/'result-tree.xml').write_text(result);shot('02-ev-result')
assert '19.0 kWh' in result, 'Expected sample energy result not found'
(OUT/'result.json').write_text(json.dumps({'platform':'Android 14; not Pleos','ev_calculation':'passed','vehicle_data':'manual','speech':'not implemented','prediction_accuracy':'not validated'},ensure_ascii=False,indent=2))
print('EV prototype calculation passed on standard Android AVD')
