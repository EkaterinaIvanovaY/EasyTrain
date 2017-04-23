

#include "CurieTimerOne.h"
#include <CurieBLE.h>
#include <CurieIMU.h>
#include <MadgwickAHRS.h>

#define SERVICE_UUID  ("19B10000-E8F2-537E-4F6C-D104768A1214")
#define ANGLE_UUID    ("19B10001-E8F2-537E-4F6C-D104768A1214")
#define SWITCH_UUID    ("19B10002-E8F2-537E-4F6C-D104768A1214")
#define TIME_US       5000   // frequency equal to 200 Hz

bool connectedBLE = false; 
unsigned char data = 0;
unsigned char count = 0;
int aix, aiy, aiz;
int gix, giy, giz;
float ax, ay, az;
float gx, gy, gz;
float pitch;


BLEPeripheral blePeripheral; 
BLEService eService(SERVICE_UUID); 

BLEUnsignedCharCharacteristic angle_char(ANGLE_UUID, BLERead);
BLEUnsignedCharCharacteristic switch_char(SWITCH_UUID, BLEWrite);

Madgwick filter;


static unsigned char exchange_data(void) {
  
  CurieIMU.readMotionSensor(aix, aiy, aiz, gix, giy, giz);
      
  ax = convertRawAcceleration(aix);
  ay = convertRawAcceleration(aiy);
  az = convertRawAcceleration(aiz);
  gx = convertRawGyro(gix);
  gy = convertRawGyro(giy);
  gz = convertRawGyro(giz);
    
  filter.updateIMU(gx, gy, gz, ax, ay, az);
  pitch = filter.getPitch();
  
  int pitch_int = static_cast<int>(pitch);
  unsigned char pitch_char = static_cast<unsigned char>(abs(pitch_int));
  return pitch_char;
}

static float convertRawAcceleration(int aRaw) { 
  float f_aRaw = static_cast<float>(aRaw);  
  float a = (f_aRaw * 2.0) / 32768.0;
  return a;
}

static float convertRawGyro(int gRaw) {
  float f_gRaw = static_cast<float>(gRaw);  
  float g = (f_gRaw * 250.0) / 32768.0;
  return g;
}

void BLE_connectedBLE_IRQ(BLEDevice central) {
  digitalWrite(13, HIGH);
  CurieTimerOne.start(TIME_US, &IMUHandler_IRQ);
  connectedBLE = true;
}

void BLE_DisconnectedBLE_IRQ(BLEDevice central) {
  digitalWrite(13, LOW);
  CurieTimerOne.kill();
  connectedBLE = false;
}

void IMUHandler_IRQ() {
  if ( switch_char.value() ) {
    data = exchange_data();
    count++;
    if ( count > 10 ) {
      count = 0;
      angle_char.setValue(data); 
      BLE.poll();
    }
    CurieTimerOne.restart(TIME_US);
  }
}



void setup() {

  pinMode(13, OUTPUT);

  CurieIMU.begin();
  CurieIMU.setGyroRate(200);     
  CurieIMU.setAccelerometerRate(200);
  CurieIMU.setAccelerometerRange(2);
  CurieIMU.setGyroRange(250);
  
  filter.begin(200);

  
  BLE.begin();
  
  BLE.setLocalName("eTrain");
  BLE.setAdvertisedService(eService);
  
  eService.addCharacteristic(angle_char);
  eService.addCharacteristic(switch_char);
  angle_char.setValue(0);
  switch_char.setValue(0);

  BLE.addService(eService);
  BLE.advertise();
  
  BLE.setEventHandler(BLEConnected, BLE_connectedBLE_IRQ);
  BLE.setEventHandler(BLEDisconnected, BLE_DisconnectedBLE_IRQ);

}

void loop() { 
 
}


/* Текущие проблемы:
   1.Для правильной загрузки после включения платы
     следует сделать MASTER_RESET                    ---- Проблема решаема. Если подключить батарейку, то ничего такого не случится
   2.На фильтре Madgwick программа зависает 
     в данной реализации
*/

