package kr.mycabin.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.WebView;
import org.json.JSONObject;
import java.util.ArrayList;

/** Push-to-talk course selection. No background recording, and no vehicle commands. */
public final class VoiceGateway implements RecognitionListener {
 private final Activity activity;
 private final WebView web;
 private SpeechRecognizer recognizer;
 private boolean listening;
 public VoiceGateway(Activity activity, WebView web){this.activity=activity;this.web=web;}
 private void emit(String key,String value){try{JSONObject data=new JSONObject().put(key,value);web.evaluateJavascript("window.dispatchEvent(new CustomEvent('cabin-voice',{detail:"+data+"}))",null);}catch(Exception ignored){}}
 public void listen(){
  if(listening)return;
  if(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},41);return;}
  if(!SpeechRecognizer.isRecognitionAvailable(activity)){emit("error","이 기기에 음성 인식 서비스가 없습니다. 화면에서 학습을 선택해 주세요.");return;}
  try{
   if(recognizer==null){recognizer=SpeechRecognizer.createSpeechRecognizer(activity);recognizer.setRecognitionListener(this);}
   Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");
   intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
   intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   listening=true;recognizer.startListening(intent);
  }catch(Exception e){listening=false;emit("error","음성 인식을 시작하지 못했습니다. 화면에서 선택해 주세요.");}
 }
 public void onPermission(boolean granted){if(granted)listen();else emit("error","마이크 권한이 필요합니다. 화면 선택은 계속 사용할 수 있어요.");}
 public void cancel(){if(recognizer!=null)recognizer.cancel();listening=false;}
 public void release(){if(recognizer!=null)recognizer.destroy();recognizer=null;listening=false;}
 @Override public void onReadyForSpeech(Bundle params){emit("status","학습 번호나 이름을 말씀해 주세요.");}
 @Override public void onBeginningOfSpeech(){}
 @Override public void onRmsChanged(float rms){}
 @Override public void onBufferReceived(byte[] buffer){}
 @Override public void onEndOfSpeech(){}
 @Override public void onError(int code){listening=false;emit("error","음성을 확인하지 못했어요. 다시 시도하거나 화면에서 선택해 주세요. ("+code+")");}
 @Override public void onResults(Bundle results){listening=false;ArrayList<String> list=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(list!=null&&!list.isEmpty())emit("text",list.get(0));else emit("error","학습 번호나 이름을 다시 말씀해 주세요.");}
 @Override public void onPartialResults(Bundle results){}
 @Override public void onEvent(int type,Bundle params){}
}
