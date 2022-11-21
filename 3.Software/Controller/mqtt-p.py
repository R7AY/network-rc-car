#!/usr/bin/env python
#coding:utf-8

import time
import json
import psutil
import random
from paho.mqtt import client as mqtt_client

broker = '192.168.2.6'  # mqtt代理服务器地址
port = 1883
keepalive = 60     # 与代理通信之间允许的最长时间段（以秒为单位）
client_id = 'Car_v1.0'
topic = "/RC/data"
def to_M(n):
    '''将B转换为M'''
    u = 1024 * 1024
    m = round(n / u, 2)
    return m

data_gps = {
            'type':'gps',
            'longitude':114.76172232,
            'latitude':25.79769665,
            'altitude':173.201234124,
            'direction':290.0,
            'speed':0.68,
            'time':'2022-11-18 06 58 51'
           }

data_imu_acceler = {'type':'acceler',
                'x': 1.0,
                'y': 9.81,
                'z': 2.0,
}

data_imu_gyroscope = {'type':'gyroscope',
                'x': 5.0,
                'y': 2.0,
                'z': 6.0,
}

data_info = {
    'type':'info',
   'power':80,
    'LTE':-120,
}

def connect_mqtt():
    '''连接mqtt代理服务器'''
    def on_connect(client, userdata, flags, rc):
        '''连接回调函数'''
        # 响应状态码为0表示连接成功
        if rc == 0:
            print("Connected to MQTT OK!")
        else:
            print("Failed to connect, return code %d\n", rc)
    # 连接mqtt代理服务器，并获取连接引用
    client = mqtt_client.Client(client_id)
    client.on_connect = on_connect
    client.connect(broker, port, keepalive)
    return client

def publish(client):
    '''发布消息'''
    while True:


        result1 = client.publish(topic, json.dumps(data_gps))
        if result1[0] == 0:
            data_gps['longi  tude'] =random.uniform(100,  140)
            data_gps['latitude'] = random.uniform(30, 50)
            data_gps['altitude'] = random.randint(0, 10)
            data_gps['direction'] = random.randint(0         , 240)
            data_gps['speed'] = random.randint(0, 50)

            print(f"Send `{json.dumps(data_gps)}` to topic `{topic}`")
        else:
            print(f"Failed to send message to topic {topic}")
        time.sleep(0.2)


        result2 = client.publish(topic, json.dumps(data_imu_acceler))
        if result2[0] == 0:
            print(f"Send `{json.dumps(data_imu_acceler)}` to topic `{topic}`")
        else:
            print(f"Failed to send message to topic {topic}")
        time.sleep(0.2)


        result3 = client.publish(topic, json.dumps(data_imu_gyroscope))
        if result3[0] == 0:
            print(f"Send `{json.dumps(data_imu_gyroscope)}` to topic `{topic}`")
        else:
            print(f"Failed to send message to topic {topic}")
        time.sleep(0.2)


        result4 = client.publish(topic, json.dumps(data_info))
        if result4[0] == 0:
            print(f"Send `{json.dumps(data_info)}` to topic `{topic}`")
        else:
            print(f"Failed to send message to topic {topic}")
        time.sleep(0.2)


def run():
    '''运行发布者'''
    client = connect_mqtt()
    # 运行一个线程来自动调用loop()处理网络事件, 非阻塞
    client.loop_start()
    publish(client)

if __name__ == '__main__':
    run()