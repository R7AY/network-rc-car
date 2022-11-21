package com.llj.mqtt_hardware;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTT {

    private static MqttClient mqttClient;
    private static MqttConnectOptions mqttConnectOptions;


    private static final  String clientId = "Car_v1.0";
    String topicDown = "/RC/cmd";
    String topicUp = "/RC/data";
    public String receiveMsg;



    public MQTT() {

        String userName = "android-demo";   //连接的用户名

        mqttConnectOptions  = new MqttConnectOptions();//MQTT的连接设置
        mqttConnectOptions.setCleanSession(true);//清除连接信息
        mqttConnectOptions.setUserName(userName);//设置连接的用户名
//        mqttConnectOptions.setPassword("11223344".toCharArray());//设置连接的密码
        mqttConnectOptions.setConnectionTimeout(3);// 设置连接超时时间 单位为秒
        mqttConnectOptions.setKeepAliveInterval(60);//心跳包时间60S




    }

    /**
     * 判断是否连接
     * @return
     */
    public boolean isConnect()  {
        if(mqttClient==null)
            return false;
         return mqttClient.isConnected();

    }

    /**
     * 连接
     */
    public void connect(String serverURI)  {
        try {
            mqttClient = new MqttClient(
                    serverURI,//连接的地址信息
                    clientId,//ClientID,使用当前时间戳
                    new MemoryPersistence());//
            mqttClient.setCallback(new MqttCallback() {//回调函数
                @Override//连接断开
                public void connectionLost(Throwable throwable) {
                }

                @Override//接收到消息
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    //先订阅-然后这里处理接收到的消息
                    receiveMsg = mqttMessage.toString();
                    Log.e("receiveMsg",receiveMsg);
                }
                @Override//没用过
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                }
            });
            mqttClient.connect(mqttConnectOptions);
            mqttClient.subscribe(topicDown, 0);   //订阅主题
        }catch (Exception e){
            Log.e("MQTT初始化错误:", e.toString());
        }
    }

    /**
     * 断开连接
     */
    public void disconnect()  {
        try {
            mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    /**
     * 发送消息
     * @param msg
     */
    public  void send(String msg) {
            new Thread(new Runnable() {//用任务
                @Override
                public void run() {
                    try {
                        if (mqttClient.isConnected()) {
                            mqttClient.publish(topicUp,msg.getBytes(),0,false);
                        }
                    }catch (Exception e){
                        Log.e("Mqtt 发送消息错误 ：", e.toString());
                    }
                }
            }).start();
    }

}


