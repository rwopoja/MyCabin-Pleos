"""Real APK UI smoke/recording on an ordinary Android AVD, NOT a Pleos AVD.
Uses Android accessibility nodes, not browser/CDP access. Saves native screenshots and video.
"""
import subprocess,time,pathlib,xml.etree.ElementTree as ET,re,json
OUT=pathlib.Path('runtime-artifacts');OUT.mkdir(exist_ok=True)
def adb(*args,check=True):return subprocess.run(['adb',*args],capture_output=True,text=True,check=check).stdout

def snapshot(name):
 p=OUT/(name+'.png')
 with p.open('wb') as f:subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
 return p

def tree():
 adb('shell','uiautomator','dump','/sdcard/cabin-tree.xml',check=False)
 text=adb('shell','cat','/sdcard/cabin-tree.xml')
 return ET.fromstring(text)

def tap(label):
 for _ in range(6):
  root=tree();candidates=[]
  for el in root.iter('node'):
   text=el.get('text','')+' '+el.get('content-desc','')
   if label not in text:continue
   coords=list(map(int,re.findall(r'\d+',el.get('bounds',''))))
   if len(coords)!=4:continue
   x1,y1,x2,y2=coords
   if x2>x1 and y2>y1:
    candidates.append((0 if el.get('clickable')=='true' else 1,(x2-x1)*(y2-y1),coords))
  if candidates:
   _,_,(x1,y1,x2,y2)=sorted(candidates)[0]
   adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(2.5);return
  time.sleep(1)
 (OUT/'failed-tree.xml').write_text(ET.tostring(root,encoding='unicode'));snapshot('failed-screen')
 raise AssertionError('Accessibility node not found: '+label)

adb('shell','wm','size','1920x1080');adb('shell','wm','density','160')
adb('install','-r','apk/김재윤_마이캐빈_release.apk')
adb('logcat','-c');print(adb('shell','am','start','-W','-n','kr.mycabin.app/.MainActivity'),flush=True)
time.sleep(5)
record=None
try:
 for attempt in range(12):
  root=tree()
  if '지금, 어떻게' in ET.tostring(root,encoding='unicode'):break
  if attempt in (2,6): adb('shell','am','start','-W','-n','kr.mycabin.app/.MainActivity',check=False)
  time.sleep(5)
 else:
  (OUT/'failed-tree.xml').write_text(ET.tostring(root,encoding='unicode'));snapshot('failed-home')
  raise AssertionError('App home did not render within 60 seconds')
 snapshot('01-home')
 record=subprocess.Popen(['adb','shell','screenrecord','--time-limit','90','--bit-rate','4000000','/sdcard/MyCabin_Android_AVD.mp4'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
 time.sleep(3)
 tap('휴식');snapshot('02-rest')
 tap('20분 휴식');tap('편집');snapshot('03-edit')
 tap('저장');tap('이 마이씬 시작');tap('모의 적용 및 시작');snapshot('04-session');time.sleep(5)
 tap('종료하기');tap('종료 및 모의 복원')
 tap('학습');snapshot('05-courses')
 tap('학습 시작');snapshot('06-lesson');tap('다음');snapshot('07-next')
 tap('학습 목록');tap('이어서 학습');snapshot('08-resume')
 (OUT/'result.json').write_text(json.dumps({'apk_installed':True,'ui_flow':'passed','platform':'ordinary Android API 34 AVD; NOT Pleos Connect','vehicle':'simulated','sdk_runtime_verified':False},ensure_ascii=False,indent=2))
 print('Actual Android APK launch and accessibility flow passed. Pleos service not present.',flush=True)
finally:
 if record:
  # Let screenrecord finish cleanly so its MP4 index is valid.
  record.wait(timeout=105)
  adb('pull','/sdcard/MyCabin_Android_AVD.mp4',str(OUT/'MyCabin_Android_AVD.mp4'),check=False)
 (OUT/'logcat.txt').write_text(adb('logcat','-d'))
