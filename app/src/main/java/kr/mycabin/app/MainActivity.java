package kr.mycabin.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.JavascriptInterface;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import java.io.ByteArrayInputStream;

/** Local-first contest app. Real SDK requests are isolated behind explicit support and parked-state checks. */
public final class MainActivity extends Activity {
 private WebView web;
 private TextToSpeech tts;
 private boolean ttsReady = false;
 private PleosVehicleGateway pleos;
 private VoiceGateway voice;
 private AiGateway ai;
 private static final String ORIGIN = "https://appassets.androidplatform.net/assets/";

 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  web = new WebView(this);
  web.setBackgroundColor(0xfff7f8f7);
  WebSettings settings=web.getSettings();
  settings.setJavaScriptEnabled(true);
  settings.setDomStorageEnabled(true);
  // Respect the page's device-width viewport when the vehicle resizes an app window.
  settings.setUseWideViewPort(true);
  settings.setLoadWithOverviewMode(false);
  // Let WebView use device density instead of forcing 100% physical-pixel scaling.
  web.setInitialScale(0);
  settings.setAllowFileAccess(false);
  settings.setAllowContentAccess(false);
  settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  settings.setMediaPlaybackRequiresUserGesture(true);
  WebView.setWebContentsDebuggingEnabled(false);
  web.addJavascriptInterface(new Bridge(), "CabinNative");
  pleos=new PleosVehicleGateway(this,web);
  voice=new VoiceGateway(this,web);
  ai=new AiGateway(web);
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request){
    String url=request.getUrl().toString();
    if(!url.startsWith(ORIGIN))return blocked();
    String path=url.substring(ORIGIN.length()).split("\\?",2)[0];
    if(path.contains("..") || path.contains("%"))return blocked();
    String mime=path.endsWith(".html")?"text/html":path.endsWith(".js")?"application/javascript":path.endsWith(".css")?"text/css":path.endsWith(".wav")?"audio/wav":path.endsWith(".ttf")?"font/ttf":"application/octet-stream";
    try{return new WebResourceResponse(mime,"UTF-8",getAssets().open(path));}catch(Exception e){return blocked();}
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !request.getUrl().toString().startsWith(ORIGIN);}
  });
  tts=new TextToSpeech(this,status->{ttsReady=status==TextToSpeech.SUCCESS;});
  tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){
   @Override public void onStart(String id){}
   @Override public void onDone(String id){speechResult(id,false);}
   @Override public void onError(String id){speechResult(id,true);}
  });
  setContentView(web);
  web.loadUrl(ORIGIN+"index.html");
 }
 private void speechResult(String id,boolean error){if(id==null||id.isEmpty())return;runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.dispatchEvent(new CustomEvent('cabin-audio',{detail:{id:"+org.json.JSONObject.quote(id)+",error:"+error+"}}))",null);});}
 private void notifyMessage(String text){web.evaluateJavascript("window.dispatchEvent(new CustomEvent('cabin-message',{detail:"+org.json.JSONObject.quote(text)+"}))",null);}
 private static WebResourceResponse blocked(){return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
 public final class Bridge {
  @JavascriptInterface public String load(){return getPreferences(MODE_PRIVATE).getString("state","null");}
  @JavascriptInterface public void save(String json){if(json!=null&&json.length()<2000000)getPreferences(MODE_PRIVATE).edit().putString("state",json).apply();}
  @JavascriptInterface public void speak(String text,String language,String id){if(text==null)return;runOnUiThread(()->{if(!ttsReady){speechResult(id,true);return;}int languageStatus=tts.setLanguage(Locale.forLanguageTag(language));if(languageStatus==TextToSpeech.LANG_MISSING_DATA||languageStatus==TextToSpeech.LANG_NOT_SUPPORTED){speechResult(id,true);return;}if(tts.speak(text.substring(0,Math.min(text.length(),3500)),TextToSpeech.QUEUE_FLUSH,null,id)==TextToSpeech.ERROR)speechResult(id,true);});}
  @JavascriptInterface public void stopSpeech(){runOnUiThread(()->{if(tts!=null)tts.stop();});}
  @JavascriptInterface public void askAI(String endpoint,String json){if(endpoint!=null&&endpoint.length()<2000&&json!=null&&json.length()<150000)runOnUiThread(()->{if(pleos.verifiedParked())ai.ask(endpoint,json);else web.evaluateJavascript("window.dispatchEvent(new CustomEvent('cabin-ai',{detail:{error:'실제 주차 상태를 확인할 수 없습니다. SDK 지원 기능 조회에서 권한과 차량 상태를 확인해 주세요.'}}))",null);});}
  @JavascriptInterface public void openVideo(String url){if(url==null||url.length()>2000)return;runOnUiThread(()->{try{android.net.Uri uri=android.net.Uri.parse(url);String host=uri.getHost();if(!"https".equals(uri.getScheme())||!("youtube.com".equals(host)||"www.youtube.com".equals(host)||"m.youtube.com".equals(host)||"youtu.be".equals(host)))return;if(!pleos.verifiedParked()){notifyMessage("실제 주차 상태를 확인한 뒤 다시 열어 주세요.");return;}startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){notifyMessage("영상을 열 수 있는 앱이 없습니다.");}});}
  @JavascriptInterface public void listen(){runOnUiThread(()->voice.listen());}
  @JavascriptInterface public void inspectVehicle(){runOnUiThread(()->{if(checkSelfPermission("android.car.permission.CAR_SPEED")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.car.permission.CAR_SPEED","android.car.permission.CAR_POWERTRAIN"},42);else pleos.inspect();});}
  @JavascriptInterface public void applySeat(String json){if(json!=null&&json.length()<10000)runOnUiThread(()->pleos.applySeat(json));}
  @JavascriptInterface public void applyVehicle(String json){if(json!=null&&json.length()<10000)runOnUiThread(()->pleos.apply(json));}
  @JavascriptInterface public void exportData(String json){if(json==null||json.length()>2000000)return;runOnUiThread(()->{Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,json);startActivity(Intent.createChooser(share,"마이씬 내보내기"));});}
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==41&&voice!=null)voice.onPermission(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED);if(request==42&&pleos!=null)pleos.inspect();}
 @Override public void onBackPressed(){web.evaluateJavascript("window.CabinApp && window.CabinApp.back()",null);}
 @Override protected void onPause(){super.onPause();if(tts!=null)tts.stop();if(voice!=null)voice.cancel();web.evaluateJavascript("window.dispatchEvent(new Event('pagehide'))",null);web.onPause();}
 @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();}
 @Override protected void onDestroy(){if(voice!=null)voice.release();if(pleos!=null)pleos.release();if(tts!=null)tts.shutdown();if(web!=null){web.removeJavascriptInterface("CabinNative");web.destroy();}super.onDestroy();}
}
