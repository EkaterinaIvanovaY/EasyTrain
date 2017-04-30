

#include "CurieTimerOne.h"
#include <CurieBLE.h>
#include <CurieIMU.h>
#include <MadgwickAHRS.h>

#define SERVICE_UUID  ("19B10000-E8F2-537E-4F6C-D104768A1214")
#define ANGLE_UUID    ("19B10001-E8F2-537E-4F6C-D104768A1214")
#define SQUAT_UUID    ("19B10002-E8F2-537E-4F6C-D104768A1214")
#define ERROR_UUID    ("19B10003-E8F2-537E-4F6C-D104768A1214")
#define SWITCH_UUID   ("19B10004-E8F2-537E-4F6C-D104768A1214")
#define TIME_US       5000   // frequency equal to 200 Hz

bool connectedBLE = false;
unsigned char data = 0;
int count = 0;
int aix, aiy, aiz;
int gix, giy, giz;
float ax, ay, az;
float gx, gy, gz;
float pitch;
int squat, errors = 0;
bool state, error_state = false;

BLEService eService(SERVICE_UUID);

BLEUnsignedCharCharacteristic angle_char(ANGLE_UUID, BLERead);
BLEUnsignedCharCharacteristic squat_char(SQUAT_UUID, BLERead);
BLEUnsignedCharCharacteristic error_char(ERROR_UUID, BLERead);
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

  unsigned char pitch_char = static_cast<unsigned char>(abs(pitch));
  
  angle_char.setValue(pitch_char);
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

static void data_processing(unsigned char data) {
  if ( data < 50 ) {
    state = TRUE;
    if ( (data < 45) and (error_state == false)) {
      errors++;
      error_state = true;
      error_char.setValue(errors);
    }
  }
  else {
      error_state = false;
    }
  if ( (data > 70) and ( state == true) ) {
    state = FALSE;
    squat++;
    squat_char.setValue(squat);
  }

}

void BLE_connectedBLE_IRQ(BLEDevice central) {
  digitalWrite(13, HIGH);
  CurieTimerOne.start(TIME_US, &IMUHandler_IRQ);
  connectedBLE = true;
}

void BLE_DisconnectedBLE_IRQ(BLEDevice central) {
  digitalWrite(13, LOW);
  digitalWrite(12, LOW);
  CurieTimerOne.kill();
  connectedBLE = false;
}

void IMUHandler_IRQ() {
  data = exchange_data();
  if ( switch_char.value() ) {
    if ( data < 45 ) {
      digitalWrite(12, HIGH);
    }
    else {
      digitalWrite(12, LOW);
    }
    count++;
    if ( count > 10 ) {   
      count = 0;
      data_processing(data);
      BLE.poll();
    }
  }
}


void setup() {
  pinMode(12, OUTPUT);
  pinMode(13, OUTPUT);
  digitalWrite(12, LOW);
  digitalWrite(13, LOW);

  CurieIMU.begin();
  CurieIMU.setGyroRate(200);
  CurieIMU.setAccelerometerRate(200);
  CurieIMU.setAccelerometerRange(2);
  CurieIMU.setGyroRange(250);

  filter.begin(200);
  
  BLE.begin();

  BLE.setLocalName("ETrain");
  BLE.setAdvertisedService(eService);

  eService.addCharacteristic(angle_char);
  eService.addCharacteristic(squat_char);
  eService.addCharacteristic(error_char);
  eService.addCharacteristic(switch_char);
  angle_char.setValue(0);
  error_char.setValue(0);
  squat_char.setValue(0);
  switch_char.setValue(0);

  BLE.addService(eService);
  BLE.advertise();

  BLE.setEventHandler(BLEConnected, BLE_connectedBLE_IRQ);
  BLE.setEventHandler(BLEDisconnected, BLE_DisconnectedBLE_IRQ);
}

void loop() { }


/* Текущие проблемы:
   1. Данные идут с задержкой!!
      Порядка 5 секунд
*/

