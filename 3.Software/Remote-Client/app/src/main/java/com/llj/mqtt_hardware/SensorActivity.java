package com.llj.mqtt_hardware;

import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.llj.mqtt_hardware.utils.MoblieDbm;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SensorActivity extends AppCompatActivity implements SensorEventListener  {

    private SensorManager sensorManager ;
    private Sensor mGyroscope ;
    private Sensor mAccelerometer ;
    private Sensor mMagnetic ;

    private TextView jsdView;   //加速度
    private TextView ccView;    //磁场
    private TextView dwView;    //定位
    private TextView tlyView;   //陀螺仪
    private TextView dlView;   //电量
    private TextView receiveView;   //接收到的消息


    private EditText broker;
    private ImageView BrokerImage;
    private TextView BrokerState;
    private Switch brokerSwitch;

    private EditText server;
    private ImageView serverImage;
    private TextView serverState;
    private TextView serverRate;
    private Switch serverSwitch;

    private   String serverURI;
    //消息服务
    MQTT mqtt = new MQTT();    //只能初始化一次
    SensorEvent sensorEvent=null;   //传感器发生变化时候的Sensor

    private static final int JSD = 0;
    private static final int CC = 1;
    private static final int DW = 2;
    private static final int TLY = 3;
    private static final int RECEIVE = 4;
    private static final int BATTERY = 5;
    private static final long SEND_INTERVAL = 1000; //发送间隔


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        jsdView = (TextView) findViewById(R.id.edtjsd);
        ccView = (TextView) findViewById(R.id.edtcc);
        dwView = (TextView) findViewById(R.id.edtdw);
        tlyView = (TextView) findViewById(R.id.edttly);
        dlView = (TextView) findViewById(R.id.edtdl);
//        receiveView = (TextView) findViewById(R.id.edtReceive);

        broker = (EditText) findViewById(R.id.broker);
        BrokerImage = (ImageView) findViewById(R.id.BrokerImage);
        BrokerState = (TextView) findViewById(R.id.BrokerState);
        brokerSwitch = (Switch) findViewById(R.id.brokerSwitch);

        server = (EditText) findViewById(R.id.server);
        serverImage = (ImageView) findViewById(R.id.serverImage);
        serverState = (TextView) findViewById(R.id.serverState);
        serverRate = (TextView) findViewById(R.id.serverRate);
        serverSwitch = (Switch) findViewById(R.id.serverSwitch);

        initSensor();

        //初始化定位信息
        initLocation();
        //每一秒读取传感器数据
        showSensorInfo();



//        BrokerState.setText(mqtt.isConnect() ? "连接":"断开");

        brokerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    BrokerImage.setImageResource(R.drawable.connected);
                    String brokerURL = broker.getText().toString();
                    BrokerState.setText("已打开");
                    //连接MQTT服务器
                    mqtt.connect(brokerURL); //连接服务器

                }else {
                    BrokerImage.setImageResource(R.drawable.disconnected);
                    BrokerState.setText("已关闭");
                    mqtt.disconnect();  //断开连接
                }
            }
        });

        serverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    serverImage.setImageResource(R.drawable.connected);
                    String brokerURL = broker.getText().toString();
                    serverState.setText("已打开");
                    serverRate.setText("500kps");   //根据速度加载
                    //将获取的视频流发送到brokerURL

                }else {
                    serverImage.setImageResource(R.drawable.disconnected);
                    serverState.setText("已关闭");
                    serverRate.setText("");
                }
            }
        });
    }

    //初始化传感器
    private void initSensor() {
        //获取传感器管理器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //获取加速度
        mGyroscope = sensorManager.getDefaultSensor(TYPE_GYROSCOPE);    //获取陀螺仪
        mMagnetic = sensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);    //获取磁场
    }


    private  void showSensorInfo(){
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                while (true) { //死循环

                    if(sensorEvent==null)
                        continue;

                    Message msg = new Message();    //消息-传给handler然后更新UI
                    Sensor sensor = sensorEvent.sensor;
                    String temp = "X:" + sensorEvent.values[0] + "; Y: " + sensorEvent.values[1] + "; Z: " + sensorEvent.values[2];
                    switch (sensor.getType()){
                        case Sensor.TYPE_ACCELEROMETER:
                            msg.what = JSD;
                            msg.obj="加速度 "+ temp;
                            handler.sendMessage(msg);
                            if(mqtt.isConnect())
                                mqtt.send("加速度 "+ temp);
                            break;
                        case TYPE_GYROSCOPE:
                            msg.what = CC;
                            msg.obj="磁场 "+ temp;
                            handler.sendMessage(msg);
                            if(mqtt.isConnect())
                                mqtt.send("磁场 "+ temp);
                            break;
                        case TYPE_MAGNETIC_FIELD:
                            msg.what = TLY;
                            msg.obj="陀螺仪 "+ temp;
                            handler.sendMessage(msg);
                            if(mqtt.isConnect())
                                mqtt.send("陀螺仪 "+ temp);
                            break;
                    }

                    //接收到的返回消息
                    if(mqtt.receiveMsg!=null){
                        Message msg2 = new Message();
                        msg2.what = RECEIVE;
                        msg2.obj= mqtt.receiveMsg;
                        handler.sendMessage(msg2);
                    }
                    //获取电量
                    BatteryManager manager = (BatteryManager) SensorActivity.this.getSystemService(SensorActivity.BATTERY_SERVICE);
                    int currentLevel = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

                    //获取信号
                    int mobileDbm = MoblieDbm.getMobileDbm(SensorActivity.this);

                    Message msg3 = new Message();
                    msg3.what = BATTERY;
                    msg3.obj="电量 "+currentLevel+", 信号 "+mobileDbm;
                    handler.sendMessage(msg3);
                    if(mqtt.isConnect())
                        mqtt.send("电量 "+currentLevel+", 信号 "+mobileDbm);


                    // 通过睡眠线程来设置定时时间
                    try {
                        Thread.sleep(SEND_INTERVAL);//两秒
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    private void initLocation() {
        //获取系统的LocationManager对象
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //添加权限检查
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //设置每一秒获取一次location信息
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,      //GPS定位提供者
                200,       //更新数据时间为1秒
                1,      //位置间隔为1米
                //位置监听器
                new LocationListener() {  //GPS定位信息发生改变时触发，用于更新位置信息
                    @Override
                    public void onLocationChanged(Location location) {
                        //GPS信息发生改变时，更新位置
                        locationUpdates(location);
                    }
                    @Override
                    //位置状态发生改变时触发
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }
                    @Override
                    //定位提供者启动时触发
                    public void onProviderEnabled(String provider) {
                    }
                    @Override
                    //定位提供者关闭时触发
                    public void onProviderDisabled(String provider) {
                    }
                });
        //从GPS获取最新的定位信息
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        locationUpdates(location);    //将最新的定位信息传递给创建的locationUpdates()方法中
    }

    /***
     * 定位信息更新时进行处理
     * @param location
     */
    public void locationUpdates(Location location) {  //获取指定的查询信息

        //如果location不为空时
        if (location != null) {
            StringBuilder stringBuilder = new StringBuilder();        //使用StringBuilder保存数据
            //获取经度、纬度、等属性值
            stringBuilder.append("您的位置信息：\n");
            stringBuilder.append("经度：");
            stringBuilder.append(location.getLongitude());
            stringBuilder.append("\n纬度：");
            stringBuilder.append(location.getLatitude());
            stringBuilder.append("\n精确度：");
            stringBuilder.append(location.getAccuracy());
            stringBuilder.append("\n高度：");
            stringBuilder.append(location.getAltitude());
            stringBuilder.append("\n方向：");
            stringBuilder.append(location.getBearing());
            stringBuilder.append("\n速度：");
            stringBuilder.append(location.getSpeed());
            stringBuilder.append("\n时间：");
            //设置日期时间格式
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH mm ss");
            stringBuilder.append(dateFormat.format(new Date(location.getTime())));

            dwView.setText(stringBuilder);            //显示获取的信息
            if(mqtt.isConnect())
                mqtt.send(stringBuilder.toString());

        } else {
            //否则输出空信息
            dwView.setText("没有获取到GPS信息");
            if(mqtt.isConnect())
                mqtt.send("没有获取到GPS信息");

        }
    }


    /**
     * 传感器数据发生变化时进行处理
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        this.sensorEvent = sensorEvent;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this); // 解除监听器注册
    }

    @Override
    protected void onResume() {
        super.onResume();
        //为传感器注册监听器
        sensorManager.registerListener(this, mGyroscope,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
    }



    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == JSD) {
                jsdView.setText(msg.obj.toString());
            }else  if (msg.what == CC) {
                ccView.setText(msg.obj.toString());
            }else if (msg.what == RECEIVE) {
//                receiveView.setText(msg.obj.toString());
            }else if (msg.what == TLY){
                tlyView.setText(msg.obj.toString());
            }else if (msg.what == BATTERY){
                dlView.setText(msg.obj.toString());
            }
        }
    };

}