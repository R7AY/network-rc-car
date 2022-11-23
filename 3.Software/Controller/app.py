#!/usr/bin/env python
#coding:utf-8


import json
import time
import keyboard
import cv2 as cv
import numpy as np
import threading
from paho.mqtt import client as mqtt_client

# 定义MQTT信息
broker = '192.168.2.6'
port = 1883
keepalive = 60
client_id = 'Controller_v1.0'
data_topic = "/RC/data"
cmd_topic = "/RC/cmd"

# 指令初始信息
cmdx = {
    'brake': 1,
    'throttle': 0,
    'left': 0,
    'right': 0,
    'light': 0,
    'speaker': 0,
}
# 数传初始信息
car_data = {
    'data_gps':{
                'type':'gps',
                'longitude':None,
                'latitude':None,
                'altitude':None,
                'direction':0,
                'speed':0,
                'time':'2022-11-18 06 58 51'
               },

    'data_imu_acceler':{'type':'acceler',
                    'x': 0.0,
                    'y': 0.0,
                    'z': 0.0,
    },
    'data_imu_gyroscope': {'type':'gyroscope',
                    'x': 0.0,
                    'y': 0.0,
                    'z': 0.0,
    },
    'data_info' :{
        'type':'info',
       'power':0,
        'LTE':0,
    }
}

#创建锁
lock = threading.Lock()

def on_connect(client, userdata, flag, rc):
    if rc == 0:
        print("MQTT Connection successful")
    elif rc == 1:
        print("MQTT Protocol version error")
    elif rc == 2:
        print("MQTT Invalid client identity")
    elif rc == 3:
        print("MQTT server unavailable")
    elif rc == 4:
        print("MQTT Wrong user name or password")
    elif rc == 5:
        print("MQTT unaccredited")

def mqtt_retrun_msg_handel(client, userdata, msg):
    global car_data

    data_dict = eval(msg.payload.decode())

    lock.acquire()  # 修改前加一把锁
    if data_dict['type'] == 'gps':
        car_data['data_gps']['longitude'] = data_dict['longitude']
        car_data['data_gps']['latitude'] = data_dict['latitude']
        car_data['data_gps']['altitude'] = data_dict['altitude']
        car_data['data_gps']['direction'] = data_dict['direction']
        car_data['data_gps']['speed'] = data_dict['speed']
        car_data['data_gps']['time'] = data_dict['time']

    if data_dict['type'] == 'acceler':
        car_data['data_imu_acceler']['x'] = data_dict['x']
        car_data['data_imu_acceler']['y'] = data_dict['y']
        car_data['data_imu_acceler']['z'] = data_dict['z']

    if data_dict['type'] == 'gyroscope':
        car_data['data_imu_gyroscope']['x'] = data_dict['x']
        car_data['data_imu_gyroscope']['y'] = data_dict['y']
        car_data['data_imu_gyroscope']['z'] = data_dict['z']

    if data_dict['type'] == 'info':
        car_data['data_info']['power'] = data_dict['power']
        car_data['data_info']['LTE'] = data_dict['LTE']
    lock.release()  # 修改完释放锁

def sub_msg_main_thread():
    client.subscribe(data_topic)
    client.on_message = mqtt_retrun_msg_handel
    client.loop_forever()

def add_alpha_channel(img):
    """ 为jpg图像添加alpha通道 """

    b_channel, g_channel, r_channel = cv.split(img)  # 剥离jpg图像通道
    alpha_channel = np.ones(b_channel.shape, dtype=b_channel.dtype) * 255  # 创建Alpha通道

    img_new = cv.merge((b_channel, g_channel, r_channel, alpha_channel))  # 融合通道
    return img_new

def merge_img(jpg_img, png_img, y1, y2, x1, x2):
    """ 将png透明图像与jpg图像叠加
        y1,y2,x1,x2为叠加位置坐标值
    """

    # 判断jpg图像是否已经为4通道
    if jpg_img.shape[2] == 3:
        jpg_img = add_alpha_channel(jpg_img)

    '''
    当叠加图像时，可能因为叠加位置设置不当，导致png图像的边界超过背景jpg图像，而程序报错
    这里设定一系列叠加位置的限制，可以满足png图像超出jpg图像范围时，依然可以正常叠加
    '''
    yy1 = 0
    yy2 = png_img.shape[0]
    xx1 = 0
    xx2 = png_img.shape[1]

    if x1 < 0:
        xx1 = -x1
        x1 = 0
    if y1 < 0:
        yy1 = - y1
        y1 = 0
    if x2 > jpg_img.shape[1]:
        xx2 = png_img.shape[1] - (x2 - jpg_img.shape[1])
        x2 = jpg_img.shape[1]
    if y2 > jpg_img.shape[0]:
        yy2 = png_img.shape[0] - (y2 - jpg_img.shape[0])
        y2 = jpg_img.shape[0]

    # 获取要覆盖图像的alpha值，将像素值除以255，使值保持在0-1之间
    alpha_png = png_img[yy1:yy2, xx1:xx2, 3] / 255.0
    alpha_jpg = 1 - alpha_png

    # 开始叠加
    for c in range(0, 3):
        jpg_img[y1:y2, x1:x2, c] = ((alpha_jpg * jpg_img[y1:y2, x1:x2, c]) + (alpha_png * png_img[yy1:yy2, xx1:xx2, c]))

    return jpg_img

def key_handle(key):
    #a = keyboard.KeyboardEvent(event_type='down', scan_code=72, name='up')
    #print(key.scan_code)
    global cmdx
    # 油门
    if key.name == 'up' and key.event_type =='down':
        cmdx['throttle'] = 100

    if key.name == 'up' and key.event_type == 'up':
        cmdx['throttle'] = 0
    # 左转
    if key.name == 'left' and key.event_type == 'down':
        cmdx['left'] = 100
    if key.name == 'left' and key.event_type == 'up':
        cmdx['left'] = 0
    # 右转
    if key.name == 'right' and key.event_type == 'down':
        cmdx['right'] = 100
    if key.name == 'right' and key.event_type == 'up':
        cmdx['right'] = 0
    # 倒车
    if key.name == 'down' and key.event_type == 'down':
        cmdx['throttle'] = -100
    if key.name == 'down' and key.event_type == 'up':
        cmdx['throttle'] = 0
    # 手刹
    if key.name == 'space' and key.event_type == 'down':
        if cmdx['brake'] == 1:
            cmdx['brake'] = 0
        elif cmdx['brake'] == 0:
            cmdx['brake'] = 1
    # 喇叭
    if key.name == 'ctrl' and key.event_type == 'down':
        cmdx['speaker'] = 1
    if key.name == 'ctrl' and key.event_type == 'up':
        cmdx['speaker'] = 0

    # 手刹
    if key.name == 'alt' and key.event_type == 'down':
        if cmdx['light'] == 1:
            cmdx['light'] = 0
        elif cmdx['light'] == 0:
            cmdx['light'] = 1
    # 发送
    # result = client.publish(cmd_topic, json.dumps(cmdx))
    # 判断是否发送完成
    # status = result[0]
    # if status == 0:
    #     print(f"Send `{cmdx}` to topic `{cmd_topic}`")
    # else:
    #     print(f"Failed to send message to topic {cmd_topic}")

def cmd_push():
    while True:
        client.publish(cmd_topic, json.dumps(cmdx))
        time.sleep(0.1)


def diaplay_main():

    stream = cv.VideoCapture('rtmp://192.168.2.5:1935/live')
    osd_png = cv.imread('./osd.png', cv.IMREAD_UNCHANGED)
    font = cv.FONT_HERSHEY_SIMPLEX
    if stream.isOpened():
        while True:
            print(cmdx)

            #client.publish(cmd_topic, json.dumps(cmdx))

            fps = round(stream.get(cv.CAP_PROP_FPS))
            ref, frame = stream.read()

            if ref == False:
                frame = cv.imread('./nosignal.jpg')
                cv.imshow('Front Camera', frame)
                c = cv.waitKey(1) & 0xff
                if c == 27:
                    cv.destroyAllWindows()
                import time
                time.sleep(1)
                diaplay_main()
                break


            ###############################叠加OSD开始##########################
            frame = merge_img(frame,osd_png,0,720,0,1280)
            longitude = str(car_data['data_gps']['longitude'])
            latitude = str(car_data['data_gps']['latitude'])
            altitude = str(car_data['data_gps']['altitude'])
            direction = str(car_data['data_gps']['direction'])
            speed = str(car_data['data_gps']['speed'] )+' km/h'
            time = str(car_data['data_gps']['time'] )
            ax = 'X:'+ str(car_data['data_imu_acceler']['x'])
            ay   = ' Y:'+str(car_data['data_imu_acceler']['y'])
            az   = ' Z:'+str(car_data['data_imu_acceler']['z'])
            gx  = 'X:'+str(car_data['data_imu_gyroscope']['x'])
            gy = ' Y:'+str(car_data['data_imu_gyroscope']['y'])
            gz  =' Z:'+ str(car_data['data_imu_gyroscope']['z'])
            power   = str(car_data['data_info']['power'])
            LTE   = str(car_data['data_info']['LTE'])

            brake =  'brake: '+str(cmdx['brake'])
            throttle ='throttle: '+ str(cmdx['throttle'])
            left = 'left: '+str(cmdx['left'])
            right ='right: '+ str(cmdx['right'])
            light = 'light: ' + str(cmdx['light'])
            speaker ='speaker: ' + str(cmdx['speaker'])

            frame = cv.putText(frame, speed, (125, 25), font, 0.7, (255, 255, 255), 2)
            frame = cv.putText(frame, time, (130, 42), font, 0.4, (255, 255, 255), 1)

            frame = cv.putText(frame, longitude, (340, 18), font, 0.5, (255, 255, 255), 1)
            frame = cv.putText(frame, latitude, (340, 42), font, 0.5, (255, 255, 255), 1)

            frame = cv.putText(frame, ax+ay+az, (790, 17), font, 0.5, (255, 255, 255), 1)
            frame = cv.putText(frame, gx+gy+gz, (790, 40), font, 0.5, (255, 255, 255), 1)

            frame = cv.putText(frame, altitude, (535, 18), font, 0.5, (255, 255, 255), 1)
            frame = cv.putText(frame, direction, (535, 42), font, 0.5, (255, 255, 255), 1)

            frame = cv.putText(frame, str(fps), (1060, 30), font, 0.6, (255, 255, 255), 2)
            frame = cv.putText(frame, power, (1240, 30), font, 0.6, (255, 255, 255), 2)
            frame = cv.putText(frame, LTE, (1130, 30), font, 0.6, (255, 255, 255), 2)

            frame = cv.putText(frame, brake, (50, 150), font, 0.6, (255, 255, 0), 1)
            frame = cv.putText(frame, throttle, (50, 170), font, 0.6, (0, 255, 255), 1)
            frame = cv.putText(frame, left, (50, 190), font, 0.6, (255, 0, 255), 1)
            frame = cv.putText(frame, right, (50, 210), font, 0.6, (100, 100, 255), 1)
            frame = cv.putText(frame, light, (50, 230), font, 0.6, (100, 255, 100), 1)
            frame = cv.putText(frame, speaker, (50, 250), font, 0.6, (255, 100, 255), 1)

            ###############################叠加OSD结束##########################

            cv.imshow('Front Camera', frame)
            c = cv.waitKey(1) & 0xff
            if c == 27:
                cv.destroyAllWindows()

    else:
        print('设备不在线')

if __name__ == '__main__':

    # 启动显示
    display = threading.Thread(target=diaplay_main)
    display.start()

    #建立MQTT客户端
    client = mqtt_client.Client(client_id)
    client.on_connect = on_connect
    client.connect(broker, port, keepalive)

    # 建立键盘事件钩子,并定时发送
    keyboard.hook(key_handle)
    cmd_send = threading.Thread(target=cmd_push)
    cmd_send.start()

    # 启动mqtt
    mqtt = threading.Thread(target=sub_msg_main_thread)
    mqtt.start()

