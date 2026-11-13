import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';
const {Window}=await import(process.env.CABIN_DOM_MODULE||'happy-dom');
const root=path.resolve(import.meta.dirname,'..');const dir=path.resolve(process.env.CABIN_CAPTURE_DIR||'/tmp/mycabin-captures');fs.mkdirSync(dir,{recursive:true});
function boot(storage,native){const w=new Window({url:'https://mycabin.test/'});w.innerWidth=1269;w.innerHeight=902;if(native)w.CabinNative=native;
 w.speechSynthesis={speak(){},cancel(){}};w.SpeechSynthesisUtterance=class{constructor(t){this.text=t;}};
 if(storage)w.localStorage.setItem('mycabin-state-v1',storage);
 w.document.write(fs.readFileSync(path.join(root,'web/index.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,''));
 for(const file of ['model.js','app.js'])w.eval(fs.readFileSync(path.join(root,'web',file),'utf8'));
 return w;}
let w=boot();let doc=w.document;
function click(action,id){const q=`[data-action="${action}"]${id?`[data-id="${id}"]`:''}`;const b=doc.querySelector(q);assert.ok(b,`missing ${q}`);assert.ok(!b.disabled);b.click();}
function capture(name){const copy=doc.documentElement.cloneNode(true);for(const el of doc.querySelectorAll('input,textarea,select')){const selector=el.id?'#'+el.id:el.dataset.field?'[data-field="'+el.dataset.field+'"]':el.dataset.vehicle?'[data-vehicle="'+el.dataset.vehicle+'"]':null;if(!selector)continue;const target=copy.querySelector(selector);if(!target)continue;if(el.tagName==='TEXTAREA')target.textContent=el.value;else if(el.tagName==='SELECT'){/* Keep rendered selected attributes; these controls are not changed in this scenario. */}else{target.setAttribute('value',el.value);if(el.checked)target.setAttribute('checked','');else target.removeAttribute('checked');}}let html=copy.outerHTML;html=html.replace(/<script[^>]*>[\s\S]*?<\/script>/g,'');fs.writeFileSync(path.join(dir,name+'.html'),html);}
function field(selector,value){const el=doc.querySelector(selector);assert.ok(el);el.value=value;el.dispatchEvent(new w.Event('input',{bubbles:true}));}
assert.equal(w.CabinApp.getView(),'home');capture('01-home');
click('mode','rest');capture('02-scenes');click('detail','rest20');click('edit');field('[data-field="name"]','나의 20분 휴식');field('[data-field="temperature"]','21.5');field('[data-field="ambient"]','18');capture('03-edit');click('save');capture('04-saved');
assert.equal(w.CabinApp.getState().scenes[0].temperature,21.5);
const stored=w.localStorage.getItem('mycabin-state-v1');w.happyDOM.abort();w=boot(stored);doc=w.document;
assert.equal(w.CabinApp.getState().scenes[0].name,'나의 20분 휴식');click('mode','rest');click('detail','rest20');click('start');capture('05-confirm');click('confirm-start');assert.equal(w.CabinApp.getVehicle().temperature,21.5);capture('06-session');click('end');click('restore');assert.equal(w.CabinApp.getVehicle().temperature,23);
click('mode','learn');capture('07-courses');click('learn','english');capture('08-lesson');click('next');assert.equal(w.CabinApp.getState().courses[0].position,1);click('courses');click('learn','english');assert.match(doc.querySelector('.lesson').textContent,/window seat/);click('summary');capture('09-summary');
click('courses');w.dispatchEvent(new w.CustomEvent('cabin-voice',{detail:{text:'1번'}}));assert.equal(w.CabinApp.getView(),'learning');click('courses');click('new-course');field('#course-title','내가 준비한 업무 표현');field('#course-text','첫 번째 표현을 정리합니다.\n다음 표현도 이어서 학습합니다.');capture('10-new-course');click('save-course');assert.equal(w.CabinApp.getState().courses.length,2);
click('settings');click('drive');capture('11-driving');click('mode','sleep');click('detail','nap');click('start');assert.equal(w.CabinApp.getView(),'detail');assert.match(doc.querySelector('#toast').textContent,/주차/);capture('12-blocked');
click('settings');click('park');click('mode','meal');click('detail','lunch');capture('13-meal');
// A finished audio segment resumes at the next segment after leaving and reopening.
let lastClip;w.Audio=class {constructor(){lastClip=this;} play(){return Promise.resolve();} pause(){}};
click('mode','learn');click('restart','english');click('play');lastClip.onended();
assert.equal(w.CabinApp.getState().courses[0].position,1);
click('courses');click('learn','english');assert.match(doc.querySelector('.lesson').textContent,/window seat/);
click('prev');assert.equal(w.CabinApp.getState().courses[0].progress,0.25);
click('courses');click('restart','english');assert.equal(w.CabinApp.getState().courses[0].progress,0);
w.happyDOM.abort();
// A damaged saved record must never silently overwrite the original data.
const damaged=JSON.stringify({schema:1,scenes:[{mode:'missing',name:'old'}],courses:[]});
w=boot(damaged);doc=w.document;assert.match(doc.querySelector('#toast').textContent,/보호/);
click('mode','rest');click('detail','rest20');click('edit');field('[data-field="name"]','변경');click('save');
assert.equal(w.localStorage.getItem('mycabin-state-v1'),damaged);assert.match(doc.querySelector('#toast').textContent,/보호/);
click('settings');click('reset');assert.match(doc.querySelector('.modal').textContent,/삭제/);click('confirm-reset');
assert.notEqual(w.localStorage.getItem('mycabin-state-v1'),damaged);
w.happyDOM.abort();
// Vehicle writes require a separate confirmation and use the selected scene.
let sent=[],seatSent=[];w=boot(null,{load:()=>null,save(){},stopSpeech(){},applyVehicle(value){sent.push(JSON.parse(value));},applySeat(value){seatSent.push(JSON.parse(value));}});doc=w.document;
click('mode','meal');click('detail','lunch');click('scene-sdk-apply');assert.equal(sent.length,0);click('close-modal');assert.equal(sent.length,0);
click('scene-sdk-apply');click('scene-sdk-confirm');assert.equal(sent.length,1);assert.equal(sent[0].id,'lunch');assert.equal(sent[0].temperature,22);
click('scene-sdk-seat');assert.equal(seatSent.length,0);click('scene-sdk-seat-confirm');assert.equal(seatSent.length,1);assert.equal(seatSent[0].id,'lunch');
w.innerWidth=650;w.innerHeight=900;w.dispatchEvent(new w.Event('resize'));assert.equal(doc.querySelector('#stage').style.width,'650px');assert.equal(doc.querySelector('#stage').style.transform,'none');
w.innerWidth=1269;w.innerHeight=902;w.dispatchEvent(new w.Event('resize'));assert.equal(doc.querySelector('#stage').style.width,'1269px');
// Pausing a bundled recording must resume the same audio object and position.
click('mode','learn');click('restart','english');let created=0;let recording;
w.Audio=class {constructor(){created++;recording=this;this.currentTime=0;}play(){return Promise.resolve();}pause(){}};
click('play');recording.currentTime=4.2;click('pause');assert.match(doc.querySelector('[data-action="play"]').textContent,/이어 듣기/);click('play');assert.equal(created,1);assert.equal(recording.currentTime,4.2);
click('next');click('play');assert.equal(created,2);
w.happyDOM.abort();
// Only the matching lesson utterance can advance a newly created lesson.
let utterance='';w=boot(null,{load:()=>null,save(){},stopSpeech(){},speak(text,language,id){utterance=id;}});doc=w.document;
click('mode','learn');click('new-course');field('#course-title','음성 확인');field('#course-text','첫 번째 문장입니다.\n두 번째 문장입니다.');click('save-course');
const userCourse=w.CabinApp.getState().courses.find(c=>c.title==='음성 확인');click('learn',userCourse.id);click('play');assert.ok(utterance.startsWith('lesson-'));
w.dispatchEvent(new w.CustomEvent('cabin-audio',{detail:{id:'other',error:false}}));assert.equal(w.CabinApp.getState().courses.find(c=>c.id===userCourse.id).position,0);
w.dispatchEvent(new w.CustomEvent('cabin-audio',{detail:{id:utterance,error:false}}));assert.equal(w.CabinApp.getState().courses.find(c=>c.id===userCourse.id).position,1);
console.log('UI smoke passed: edit/save/reload, apply/restore, progress, corrupt storage protection and driving gate.');w.happyDOM.abort();
