package kr.mycabin.app;

import android.webkit.WebView;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.json.JSONObject;

/** User-configured HTTPS backend only. Provider credentials remain on the backend. */
final class AiGateway {
 private final WebView web;
 AiGateway(WebView web){this.web=web;}
 void ask(String endpoint,String payload){
  new Thread(()->{
   JSONObject result=new JSONObject();HttpsURLConnection c=null;
   try{
    URL url=new URL(endpoint);
    if(!"https".equals(url.getProtocol())||url.getUserInfo()!=null||url.getRef()!=null)throw new IllegalArgumentException("HTTPS 서버 주소를 확인해 주세요.");
    JSONObject data=new JSONObject(payload);
    if(data.optString("source").length()>30000||data.optString("question").length()>1000)throw new IllegalArgumentException("자료가 너무 깁니다.");
    c=(HttpsURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setConnectTimeout(10000);c.setReadTimeout(35000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
    byte[] body=data.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(body.length);
    try(java.io.OutputStream out=c.getOutputStream()){out.write(body);}
    if(c.getResponseCode()!=200)throw new IllegalStateException("AI 서버 응답: "+c.getResponseCode());
    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
     byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>150000)throw new IllegalStateException("응답이 너무 큽니다.");out.write(buffer,0,n);}
     result=new JSONObject(out.toString("UTF-8"));
    }
   }catch(Exception e){try{result.put("error",e.getMessage()==null?"AI 연결에 실패했습니다.":e.getMessage());}catch(Exception ignored){}}
   finally{if(c!=null)c.disconnect();}
   final String json=result.toString();web.post(()->web.evaluateJavascript("window.dispatchEvent(new CustomEvent('cabin-ai',{detail:"+json+"}))",null));
  },"cabin-ai-request").start();
 }
}
