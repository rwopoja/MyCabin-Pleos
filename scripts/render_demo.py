"""Render tested DOM snapshots as an explicitly labeled prototype flow video.
This does NOT make an emulator or browser recording. Requires Pillow, WeasyPrint, PyMuPDF and ffmpeg.
Run tests/ui-smoke.mjs first. Set CABIN_CAPTURE_DIR to choose the snapshot directory.
"""
import os, pathlib, subprocess, html as h
from bs4 import BeautifulSoup
from weasyprint import HTML,CSS
from PIL import Image,ImageDraw,ImageFont
import fitz
ROOT=pathlib.Path(__file__).resolve().parents[1]
CAP=pathlib.Path(os.environ.get('CABIN_CAPTURE_DIR','/tmp/mycabin-captures'))
OUT=ROOT/'media';FRAMES=OUT/'frames';FRAMES.mkdir(parents=True,exist_ok=True)
FONT=os.environ.get('CABIN_VIDEO_FONT','/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc')
def font(size):return ImageFont.truetype(FONT,size,index=1 if FONT.endswith('.ttc') else 0)
PRINT='''@page{size:1269px 902px;margin:0} html,body{width:1269px;height:902px} #stage{position:absolute!important;left:0!important;top:0!important;transform:none!important;width:1269px;height:902px} #app{height:902px;display:block}header{height:94px}footer{position:absolute;bottom:0;left:0;width:1269px;height:42px}.body{display:block}nav{position:absolute;left:0;top:94px;bottom:42px;width:195px;max-width:195px;display:block}nav button{display:block;width:163px;height:70px;white-space:nowrap}nav small{margin-right:14px}nav .bottom{position:absolute;bottom:25px;width:163px}.content{position:absolute;left:195px;top:94px;bottom:42px;width:1074px;overflow:hidden}.row{width:100%;flex-wrap:nowrap}.row>div:first-child{flex:1;min-width:0}.row>.primary,.row>.secondary{flex:0 0 180px}.row h1{white-space:nowrap}.primary,.secondary,.danger{display:inline-block;width:auto;flex-shrink:0}#toast{display:none!important}.field{display:block;min-height:52px}.visual-range{width:270px;height:7px;border-radius:6px;background:#d9e4e5}.visual-range span{display:block;height:7px;background:#007c81;border-radius:6px}.visual-select{padding:12px 15px;background:#fbfcfc;border:1px solid #c8d5d8;border-radius:8px;min-width:230px}.visual-check{font-size:22px;color:#007c81}.visual-color{width:56px;height:28px;border-radius:7px}'''
def render(name):
 soup=BeautifulSoup((CAP/(name+'.html')).read_text(),'html.parser')
 # WeasyPrint has no interactive native form controls: display the tested DOM values explicitly.
 for el in soup.select('input,textarea,select'):
  tag=soup.new_tag('div');tag['class']=el.get('class',[])
  if el.name=='select':
   opt=el.select_one('option[selected]') or el.select_one('option');tag['class']=['visual-select'];tag.string=opt.text if opt else ''
  elif el.get('type')=='range':
   lo=float(el.get('min',0));hi=float(el.get('max',100));v=float(el.get('value',lo));tag['class']=['visual-range'];span=soup.new_tag('span');span['style']=f'width:{(v-lo)/(hi-lo)*100}%';tag.append(span)
  elif el.get('type')=='checkbox':tag['class']=['visual-check'];tag.string='☑' if el.has_attr('checked') else '□'
  elif el.get('type')=='color':tag['class']=['visual-color'];tag['style']='background:'+el.get('value','#60c9c0')
  else:tag.string=el.text if el.name=='textarea' else el.get('value') or el.get('placeholder','')
  el.replace_with(tag)
 pdf=HTML(string=str(soup),base_url=str(ROOT/'web')+'/').write_pdf(stylesheets=[CSS(string=PRINT)])
 doc=fitz.open(stream=pdf,filetype='pdf');assert len(doc)==1
 pix=doc[0].get_pixmap(matrix=fitz.Matrix(1.6,1.6));im=Image.frombytes('RGB',[pix.width,pix.height],pix.samples);im.save(FRAMES/(name+'.png'));return im
STEPS=[
 ('01-home','My Cabin','차 안의 시간을, 내가 원하는 방식으로',7),
 ('02-scenes','01  나의 마이씬','휴식 · 수면 · 학습 · 식사, 목적에 맞게 선택',6),
 ('03-edit','02  취향을 저장하다','온도와 무드등, 시트 메모리를 직접 편집',8),
 ('04-saved','03  다음에도 그대로','기본 마이씬도 자유롭게 편집하고 저장',7),
 ('05-confirm','04  시작 전 확인','적용할 설정과 시연 상태를 확인',5),
 ('06-session','05  나에게 맞춘 휴식','저장한 설정 적용 · 휴식 타이머 · 종료 후 복원',8),
 ('07-courses','06  어제의 배움을 이어서','이전 학습을 이어가거나 새 자료를 등록',7),
 ('08-lesson','07  한 구간씩 듣기','음성 재생과 학습 위치 자동 저장',7),
 ('09-summary','08  원문과 설명을 구분','원문 발췌 제공 · AI 설명은 별도 서버 연결',7),
 ('10-new-course','09  내가 고른 자료로','직접 등록한 자료와 유튜브 원본 주소 보관',7),
 ('12-blocked','10  상황에 맞는 사용','주행 시연 중 휴식 · 수면 · 식사 시작 제한',6),
 ('13-meal','11  짧은 대기도 나답게','식사에도, 쉬는 시간에도, 나의 마이씬',7),
 ('01-home','My Cabin / 마이캐빈','Pleos Vehicle SDK 2.3.2 포함 · 차량 서비스 실행 검증 대기',7)
]
def text(draw,xy,value,size,fill):draw.text(xy,value,font=font(size),fill=fill)
rendered={name:render(name) for name in dict.fromkeys(x[0] for x in STEPS)}
land=[];vertical=[]
for i,(name,title,caption,sec) in enumerate(STEPS):
 im=Image.new('RGB',(1920,1080),'#eef2f2');d=ImageDraw.Draw(im)
 text(d,(65,26),title,32,'#002c47');text(d,(65,73),caption,24,'#506b77')
 screen=rendered[name].copy();screen.thumbnail((1500,860));im.paste(screen,((1920-screen.width)//2,130))
 text(d,(65,1014),'프로토타입 기능 흐름 영상 · 모의 차량 · 실제 APK/에뮬레이터 화면 녹화가 아닙니다',21,'#536e7b')
 text(d,(1775,1014),f'{i+1:02}/{len(STEPS):02}',21,'#536e7b')
 path=FRAMES/f'land-{i:02}.png';im.save(path);land.append((path,sec))
 # Short-form uses the same tested screen states and a readable explanatory caption.
 if i in [0,2,3,5,6,7,10,11,12]:
  v=Image.new('RGB',(1080,1920),'#002c47');dv=ImageDraw.Draw(v)
  text(dv,(62,150),'MY CABIN',58,'white');text(dv,(62,260),title,42,'#9fd9d1')
  sc=rendered[name].copy();sc.thumbnail((1000,950));v.paste(sc,((1080-sc.width)//2,505))
  # Wrap caption for portrait.
  words=caption.split(' ');lines=[];line=''
  for word in words:
   new=(line+' '+word).strip()
   if dv.textbbox((0,0),new,font=font(34))[2]>950:lines.append(line);line=word
   else:line=new
  lines.append(line)
  for j,line in enumerate(lines):text(dv,(62,1400+j*59),line,34,'white')
  text(dv,(62,1750),'프로토타입 · 차량 동작은 모의 시연',26,'#a9c9cc')
  text(dv,(62,1800),'실제 APK / 에뮬레이터 녹화 아님',26,'#a9c9cc')
  vp=FRAMES/f'portrait-{i:02}.png';v.save(vp);vertical.append((vp,5))
def encode(entries,name,w,h):
 listing=FRAMES/(name+'.txt')
 listing.write_text(''.join(f"file '{p.as_posix()}'\nduration {seconds}\n" for p,seconds in entries)+f"file '{entries[-1][0].as_posix()}'\n")
 subprocess.run(['ffmpeg','-y','-hide_banner','-loglevel','error','-f','concat','-safe','0','-i',str(listing),'-vf',f'fps=12,scale={w}:{h}', '-c:v','libx264','-preset','fast','-crf','24','-pix_fmt','yuv420p','-movflags','+faststart','-t',str(sum(s for _,s in entries)),str(OUT/(name+'.mp4'))],check=True)
encode(land,'MyCabin_Prototype_Demo',1920,1080)
encode(vertical,'MyCabin_Shortform',1080,1920)
print('Created prototype flow videos; not a screen recording.',flush=True)
