#include <CurieIMU.h>
#include <MadgwickAHRS.h>
#include <CurieBLE.h>


BLEPeripheral blePeripheral; 
BLEService SquatService("19B10000-E8F2-537E-4F6C-D104768A1214");
BLECharCharacteristic angleChar("19B10001-E8F2-537E-4F6C-D104768A1214", BLERead);

Madgwick filter;  

unsigned long microsNow;
unsigned long microsPerReading, microsPrevious;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
  
void setup() {
  initIMU();
  initBLE();
}

void loop() {
  int aix, aiy, aiz;
  int gix, giy, giz;
  float ax, ay, az;
  float gx, gy, gz;
  float roll, pitch, heading; 

  microsNow = micros();
  if (microsNow - microsPrevious >= microsPerReading) {
    CurieIMU.readMotionSensor(aix, aiy, aiz, gix, giy, giz);
	
    ax = convertRawAcceleration(aix);
    ay = convertRawAcceleration(aiy);
    az = convertRawAcceleration(aiz);
    gx = convertRawGyro(gix);
    gy = convertRawGyro(giy);
    gz = convertRawGyro(giz);

    filter.updateIMU(gx, gy, gz, ax, ay, az);
    roll = filter.getRoll();
    pitch = filter.getPitch();
    heading = filter.getYaw();

    int pitch_int = (int) pitch;
    char pitch_char = 0;
    if (pitch_int > 0 ) pitch_char = (char) pitch_int;
    else {
      pitch_char = (char) -pitch_int;
    }
    angleChar.setValue(pitch_char);

    microsPrevious = microsPrevious + microsPerReading;  
  }
  
    blePeripheral.poll();
}

float convertRawAcceleration(int aRaw) { 
  float a = (aRaw * 2.0) / 32768.0;
  return a;
}

float convertRawGyro(int gRaw) {  
  float g = (gRaw * 250.0) / 32768.0;
  return g;
}

void initBLE(void) {
  blePeripheral.setLocalName("ETrain");
  
  blePeripheral.setAdvertisedServiceUuid(SquatService.uuid());

  blePeripheral.addAttribute(SquatService);
  blePeripheral.addAttribute(angleChar);

  angleChar.setValue(0);

  blePeripheral.begin();
}

void initIMU(void) {
  
  CurieIMU.begin();
  CurieIMU.setGyroRate(100);     
  CurieIMU.setAccelerometerRate(100);
  
  filter.begin(100);
  
  CurieIMU.setAccelerometerRange(2);
  CurieIMU.setGyroRange(250);
  
  microsPerReading = 1000000 / 100;
  microsPrevious = micros();
}
