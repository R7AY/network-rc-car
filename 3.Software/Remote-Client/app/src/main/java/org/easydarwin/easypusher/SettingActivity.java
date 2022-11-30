/*
	Copyright (c) 2013-2016 EasyDarwin.ORG.  All rights reserved.
	Github: https://github.com/EasyDarwin
	WEChat: EasyDarwin
	Website: http://www.easydarwin.org
*/

package org.easydarwin.easypusher;

import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.easydarwin.easypusher.databinding.ActivitySettingBinding;
import org.easydarwin.easypusher.util.Config;
import org.easydarwin.easypusher.util.SPUtil;
import org.easydarwin.easypusher.utils.MoblieDbm;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 设置页
 * */
public class SettingActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener, SensorEventListener {

    public static final int REQUEST_OVERLAY_PERMISSION = 1004;  // 悬浮框
    private static final int REQUEST_SCAN_TEXT_URL = 1003;      // 扫描二维码

    EditText url;

    private SensorManager sensorManager ;
    private Sensor mGyroscope ;
    private Sensor mAccelerometer ;
    private Sensor mMagnetic ;

    private TextView jsdView;   //加速度
    private TextView ccView;    //磁场
    private TextView dwView;    //定位
    private TextView tlyView;   //陀螺仪
    private TextView dlView;   //电量


    private EditText broker;
    private ImageView BrokerImage;
    private TextView BrokerState;
    private Switch brokerSwitch;


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
        ActivitySettingBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);



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


        initSensor();

        //初始化定位信息
        initLocation();
        //每一秒读取传感器数据
        showSensorInfo();

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




        setSupportActionBar(binding.mainToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.mainToolbar.setOnMenuItemClickListener(this);
        // 左边的小箭头（注意需要在setSupportActionBar(toolbar)之后才有效果）
        binding.mainToolbar.setNavigationIcon(R.drawable.com_back);




        url = (EditText) findViewById(R.id.push_url);
        url.setText(Config.getServerURL(this));

        // 使能摄像头后台采集
        CheckBox backgroundPushing = (CheckBox) findViewById(R.id.enable_background_camera_pushing);
        backgroundPushing.setChecked(SPUtil.getEnableBackgroundCamera(this));
        backgroundPushing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(SettingActivity.this)) {
                            SPUtil.setEnableBackgroundCamera(SettingActivity.this, true);
                        } else {
                            new AlertDialog
                                    .Builder(SettingActivity.this)
                                    .setTitle("后台上传视频")
                                    .setMessage("后台上传视频需要APP出现在顶部.是否确定?")
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            // 在Android 6.0后，Android需要动态获取权限，若没有权限，提示获取.
                                            final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                                            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                                        }
                                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            SPUtil.setEnableBackgroundCamera(SettingActivity.this, false);
                                            buttonView.toggle();
                                        }
                                    })
                                    .setCancelable(false)
                                    .show();
                        }
                    } else {
                        SPUtil.setEnableBackgroundCamera(SettingActivity.this, true);
                    }
                } else {
                    SPUtil.setEnableBackgroundCamera(SettingActivity.this, false);
                }
            }
        });

        // 是否使用软编码
        CheckBox x264enc = findViewById(R.id.use_x264_encode);
        x264enc.setChecked(SPUtil.getswCodec(this));
        x264enc.setOnCheckedChangeListener(
                (buttonView, isChecked) -> SPUtil.setswCodec(this, isChecked)
        );




        // 推送内容
        RadioGroup push_content = findViewById(R.id.push_content);

        boolean videoEnable = SPUtil.getEnableVideo(this);

        SPUtil.setEnableVideo(SettingActivity.this,true);
        if (videoEnable) {
            boolean audioEnable = SPUtil.getEnableAudio(this);

            if (audioEnable) {
                RadioButton push_av = findViewById(R.id.push_av);
                push_av.setChecked(true);
            } else {
                RadioButton push_v = findViewById(R.id.push_v);
                push_v.setChecked(true);
            }
        } else {
            RadioButton push_a = findViewById(R.id.push_a);
            push_a.setChecked(true);
        }

        push_content.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.push_av) {
                    SPUtil.setEnableVideo(SettingActivity.this, true);
                    SPUtil.setEnableAudio(SettingActivity.this, true);
                } else if (checkedId == R.id.push_a) {
                    SPUtil.setEnableVideo(SettingActivity.this, false);
                    SPUtil.setEnableAudio(SettingActivity.this, true);
                } else if (checkedId == R.id.push_v) {
                    SPUtil.setEnableVideo(SettingActivity.this, true);
                    SPUtil.setEnableAudio(SettingActivity.this, false);
                }
            }
        });

        SeekBar sb = findViewById(R.id.bitrate_seekbar);
        final TextView bitrateValue = findViewById(R.id.bitrate_value);

        int bitrate_added_kbps = SPUtil.getBitrateKbps(this);
        int kbps = 72000 + bitrate_added_kbps;
        bitrateValue.setText(kbps/1000 + "kbps");

        sb.setMax(5000000);
        sb.setProgress(bitrate_added_kbps);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int kbps = 72000 + progress;
                bitrateValue.setText(kbps/1000 + "kbps");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SPUtil.setBitrateKbps(SettingActivity.this, seekBar.getProgress());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this); // 解除监听器注册
        String text = url.getText().toString().trim();
        if (text.toLowerCase().startsWith("rtmp://")) {
            Config.setServerURL(SettingActivity.this, text);
        }
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

    /*
    * 二维码扫码
    * */
    public void onScanQRCode(View view) {
        Intent intent = new Intent(this, ScanQRActivity.class);
        startActivityForResult(intent, REQUEST_SCAN_TEXT_URL);
        overridePendingTransition(R.anim.slide_bottom_in, R.anim.slide_top_out);
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean canDraw = Settings.canDrawOverlays(this);
                SPUtil.setEnableBackgroundCamera(SettingActivity.this, canDraw);

                if (!canDraw) {
                    CheckBox backgroundPushing = (CheckBox) findViewById(R.id.enable_background_camera_pushing);
                    backgroundPushing.setChecked(false);
                }
            }
        } else if (requestCode == REQUEST_SCAN_TEXT_URL) {
            if (resultCode == RESULT_OK) {
                String url = data.getStringExtra("text");
                this.url.setText(url);

                Config.setServerURL(SettingActivity.this, url);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
    }

    // 返回的功能
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                    BatteryManager manager = (BatteryManager) SettingActivity.this.getSystemService(SettingActivity.BATTERY_SERVICE);
                    int currentLevel = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

                    //获取信号
                    int mobileDbm = MoblieDbm.getMobileDbm(SettingActivity.this);

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
        if (android.support.v4.app.ActivityCompat.checkSelfPermission(this,
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
