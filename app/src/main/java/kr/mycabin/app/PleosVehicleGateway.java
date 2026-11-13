package kr.mycabin.app;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import org.json.JSONObject;
import java.util.Map;
import java.lang.reflect.Method;
import ai.pleos.playground.vehicle.Vehicle;
import ai.pleos.playground.vehicle.api.model.area.SeatArea;
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds;
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig;
import ai.pleos.playground.vehicle.api.model.capability.AccessType;
import ai.pleos.playground.vehicle.constant.area.SeatValue;
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit;
import kotlin.Unit;

/** Typed public Pleos SDK calls. Never treats initialization alone as a connected service. */
final class PleosVehicleGateway {
 private final Activity activity;private final WebView web;private Vehicle vehicle;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final SeatArea area=new SeatArea(SeatValue.ROW_1_LEFT);
 private AreaBounds<Float> temperatureBounds;private AreaBounds<Integer> lightBounds;
 private boolean canTemperature,canLight,canSeat;
 private AreaBounds<Integer> seatBounds;
 private Object car;
 PleosVehicleGateway(Activity a,WebView w){activity=a;web=w;}
 private void emit(String event,Object value){main.post(()->{try{JSONObject o=new JSONObject();o.put("event",event);o.put("value",String.valueOf(value));web.evaluateJavascript("window.dispatchEvent(new CustomEvent('pleos-result',{detail:"+o.toString()+"}))",null);}catch(Exception ignored){}});}
 private Unit fail(Exception e){emit("ERROR",e.getClass().getSimpleName()+": "+e.getMessage());return Unit.INSTANCE;}
 void inspect(){
  try{
   canTemperature=false;canLight=false;canSeat=false;
   if(vehicle==null){vehicle=new Vehicle(activity);vehicle.initialize();}
   emit("INITIALIZED","SDK 2.3.2 초기화 요청. 아래 capability/read 성공 여부를 확인하세요.");
   vehicle.getHVAC().checkHvacTemperatureCapabilities((CapabilityConfig config,Map<SeatArea,AreaBounds<Float>> areas)->{
    temperatureBounds=areas.get(area);canTemperature=temperatureBounds!=null&&config.getAccessType()==AccessType.READ_WRITE;
    emit("HVAC_CAPABILITY",config+" / "+areas);
    if(temperatureBounds!=null)vehicle.getHVAC().getHvacTemperature(area,TemperatureUnit.CELSIUS,value->{emit("HVAC_READ",value);return Unit.INSTANCE;},this::fail);
    return Unit.INSTANCE;
   },this::fail);
   vehicle.getLight().checkAmbientLightBrightnessCapabilities((CapabilityConfig config,Map<SeatArea,AreaBounds<Integer>> areas)->{
    lightBounds=areas.get(area);canLight=lightBounds!=null&&config.getAccessType()==AccessType.READ_WRITE;
    emit("LIGHT_CAPABILITY",config+" / "+areas);
    if(lightBounds!=null)vehicle.getLight().getAmbientLightBrightness(area,value->{emit("LIGHT_READ",value);return Unit.INSTANCE;},this::fail);
    return Unit.INSTANCE;
   },this::fail);
   vehicle.getSeat().checkSeatMemoryCapabilities((config,areas)->{
    seatBounds=areas.get(area);canSeat=seatBounds!=null&&(config.getAccessType()==AccessType.WRITE||config.getAccessType()==AccessType.READ_WRITE);
    emit("SEAT_MEMORY_CAPABILITY",config+" / "+areas);return Unit.INSTANCE;
   },this::fail);
   emit("PARK_STATE",verifiedParked()?"실제 API: P / 정지 / 주차브레이크 확인":"실제 주차 상태 미확인: 쓰기 잠금");
  }catch(Exception e){fail(e);}
 }
 /** Public Android Automotive API only; reflection avoids a hard dependency on optional android.car. */
 boolean verifiedParked(){
  try{
   Class<?> carClass=Class.forName("android.car.Car");
   if(car==null)car=carClass.getMethod("createCar",android.content.Context.class).invoke(null,activity);
   String propertyService=(String)carClass.getField("PROPERTY_SERVICE").get(null);
   Object manager=carClass.getMethod("getCarManager",String.class).invoke(car,propertyService);
   Class<?> ids=Class.forName("android.car.VehiclePropertyIds");
   int gearId=ids.getField("GEAR_SELECTION").getInt(null),brakeId=ids.getField("PARKING_BRAKE_ON").getInt(null),speedId=ids.getField("PERF_VEHICLE_SPEED").getInt(null);
   int park=Class.forName("android.car.VehicleGear").getField("GEAR_PARK").getInt(null);
   Method getInt=manager.getClass().getMethod("getIntProperty",int.class,int.class),getBool=manager.getClass().getMethod("getBooleanProperty",int.class,int.class),getFloat=manager.getClass().getMethod("getFloatProperty",int.class,int.class);
   return ((Integer)getInt.invoke(manager,gearId,0))==park&&(Boolean)getBool.invoke(manager,brakeId,0)&&Math.abs((Float)getFloat.invoke(manager,speedId,0))<0.1f;
  }catch(Exception e){emit("PARK_UNAVAILABLE",e.getClass().getSimpleName());return false;}
 }
 void apply(String json){
  try{
   if(!verifiedParked())throw new IllegalStateException("실제 주차 상태를 확인할 수 없어 SDK 쓰기를 실행하지 않았습니다.");
   if(!canTemperature||!canLight)throw new IllegalStateException("운전석 HVAC·Light READ_WRITE capability를 먼저 확인하세요.");
   JSONObject s=new JSONObject(json);float temperature=(float)s.getDouble("temperature");int ambient=s.getInt("ambient");
   if(temperature<temperatureBounds.getMinValue()||temperature>temperatureBounds.getMaxValue()||ambient<lightBounds.getMinValue()||ambient>lightBounds.getMaxValue())throw new IllegalArgumentException("차량 capability 범위를 벗어난 값입니다.");
   // The two independent operations are reported separately. No all-or-nothing success claim.
   vehicle.getHVAC().setHvacTemperature(area,temperature,TemperatureUnit.CELSIUS,(a,v)->{
    emit("HVAC_SET_ACK",v);
    vehicle.getHVAC().getHvacTemperature(area,TemperatureUnit.CELSIUS,actual->{emit("HVAC_READBACK",actual);return Unit.INSTANCE;},this::fail);
    return Unit.INSTANCE;
   },this::fail);
   vehicle.getLight().setAmbientLightBrightness(area,ambient,(a,v)->{
    emit("LIGHT_SET_ACK",v);
    vehicle.getLight().getAmbientLightBrightness(area,actual->{emit("LIGHT_READBACK",actual);return Unit.INSTANCE;},this::fail);
    return Unit.INSTANCE;
   },this::fail);
  }catch(Exception e){fail(e);}
 }
 void applySeat(String json){
  try{
   if(!verifiedParked())throw new IllegalStateException("실제 P·정지·주차브레이크 확인 전에는 좌석 메모리를 호출하지 않습니다.");
   if(!canSeat)throw new IllegalStateException("좌석 메모리 쓰기 capability를 먼저 확인하세요.");
   int memory=new JSONObject(json).getInt("seatMemory");
   if(memory<seatBounds.getMinValue()||memory>seatBounds.getMaxValue())throw new IllegalArgumentException("지원하지 않는 좌석 메모리 번호입니다.");
   vehicle.getSeat().selectSeatMemory(area,memory,this::fail);
   emit("SEAT_MEMORY_REQUEST_SENT","메모리 "+memory+" 요청 전송. 완료 콜백이 없는 API이므로 이동 완료를 확인한 상태는 아닙니다.");
  }catch(Exception e){fail(e);}
 }
 void release(){try{if(vehicle!=null)vehicle.release();}catch(Exception ignored){}try{if(car!=null)car.getClass().getMethod("disconnect").invoke(car);}catch(Exception ignored){}}
}
