package cn.ideatape.recorder;
import android.bluetooth.*;
import android.content.Context;
import android.os.*;
import java.util.*;
public final class BleLink {
 public interface Events {void status(String s);void ready(String device);void bytes(byte[] b);void disconnected();}
 private final Context context;private final Handler handler;private final Events events;private BluetoothGatt gatt;
 private BluetoothGattCharacteristic rx;private final ArrayDeque<byte[]> writes=new ArrayDeque<>();private boolean writing,subscribing,closed;private int mtu=23;private String address;
 public BleLink(Context c,Handler h,Events e){context=c;handler=h;events=e;}
 public void connect(String addr){closeGatt();events.disconnected();closed=false;address=addr;BluetoothManager bm=context.getSystemService(BluetoothManager.class);BluetoothAdapter a=bm.getAdapter();if(a==null||!a.isEnabled()){events.status("请先开启手机蓝牙");return;}events.status("正在连接磁带…");gatt=a.getRemoteDevice(addr).connectGatt(context,false,callback,BluetoothDevice.TRANSPORT_LE);}
 public String address(){return address==null?"":address;}
 private final BluetoothGattCallback callback=new BluetoothGattCallback(){
  public void onConnectionStateChange(BluetoothGatt g,int status,int state){handler.post(()->{if(g!=gatt)return;if(state==BluetoothProfile.STATE_CONNECTED&&status==0){events.status("请按设备配对码完成手机配对");try{g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);if(g.getDevice().getBondState()==BluetoothDevice.BOND_BONDED)g.discoverServices();else{if(g.getDevice().getBondState()==BluetoothDevice.BOND_NONE)g.getDevice().createBond();awaitBond(g,SystemClock.elapsedRealtime()+45000);}}catch(SecurityException e){events.status("蓝牙权限未授予");}}else if(state==BluetoothProfile.STATE_DISCONNECTED){closeGatt();events.disconnected();if(!closed){events.status("连接中断，设备仍保留录音，正在重连…");handler.postDelayed(()->{if(!closed)connect(address);},2500);}}});}
  public void onServicesDiscovered(BluetoothGatt g,int status){handler.post(()->{if(g!=gatt)return;BluetoothGattService s=g.getService(UUID.fromString(Wire.SERVICE));if(status!=0||s==null){events.status("设备固件不支持手机同步，请升级固件");close();return;}rx=s.getCharacteristic(UUID.fromString(Wire.RX));if(rx==null){events.status("同步通道不可用");close();return;}if(!g.requestMtu(247))subscribe();handler.postDelayed(()->{if(g==gatt&&!subscribing)subscribe();},2500);});}
  public void onMtuChanged(BluetoothGatt g,int n,int status){handler.post(()->{if(g!=gatt)return;if(status==0)mtu=n;subscribe();});}
  public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){handler.post(()->{if(g!=gatt)return;if(status!=0){events.status("配对未完成，请确认配对码后重连");close();return;}events.ready(g.getDevice().getAddress());});}
  public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){byte[] b=c.getValue();if(b!=null)deliver(g,b.clone());}
  public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] b){deliver(g,b.clone());}
  public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){handler.post(()->{if(g!=gatt)return;writing=false;if(status==0){writes.poll();pump();}else{events.status("同步写入失败，设备缓存保留，请重新连接");close();}});}
 };
 private void awaitBond(BluetoothGatt g,long deadline){
  if(g!=gatt||closed)return;
  if(g.getDevice().getBondState()==BluetoothDevice.BOND_BONDED){g.discoverServices();return;}
  if(SystemClock.elapsedRealtime()>deadline){events.status("配对未完成，请确认设备配对码后重新连接");close();return;}
  handler.postDelayed(()->awaitBond(g,deadline),400);
 }
 private void deliver(BluetoothGatt g,byte[] b){handler.post(()->{if(g==gatt)events.bytes(b);});}
 private void subscribe(){if(gatt==null||subscribing)return;subscribing=true;BluetoothGattService s=gatt.getService(UUID.fromString(Wire.SERVICE));BluetoothGattCharacteristic tx=s.getCharacteristic(UUID.fromString(Wire.TX));BluetoothGattDescriptor c=tx.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));if(c==null||!gatt.setCharacteristicNotification(tx,true)){events.status("无法开启同步通知");close();return;}c.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);if(!gatt.writeDescriptor(c)){events.status("无法开启同步，请重连");close();}}
 public void send(int type,byte[] body){if(gatt==null||rx==null)return;byte[] frame=Wire.frame(type,body);int size=Math.max(20,mtu-3);for(int off=0;off<frame.length;off+=size)writes.add(Arrays.copyOfRange(frame,off,Math.min(off+size,frame.length)));pump();}
 private void pump(){if(writing||writes.isEmpty()||gatt==null||rx==null)return;rx.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);rx.setValue(writes.peek());if(gatt.writeCharacteristic(rx))writing=true;else handler.postDelayed(this::pump,80);}
 private void closeGatt(){BluetoothGatt g=gatt;gatt=null;rx=null;writes.clear();writing=false;subscribing=false;mtu=23;if(g!=null){try{g.disconnect();}catch(Exception ignored){}g.close();}}
 public void close(){closed=true;closeGatt();events.disconnected();}
}
