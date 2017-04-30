
package com.example.android.bluetoothlegatt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBLEGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED =   0;
    private static final int STATE_CONNECTING =     1;
    private static final int STATE_CONNECTED =      2;

    public final static String ETRAIN_SERVICE = "19b10000-e8f2-537e-4f6c-d104768a1214";
    public final static String ANGLE_CHAR =     "19b10001-e8f2-537e-4f6c-d104768a1214";
    public final static String SQUAT_CHAR =     "19b10002-e8f2-537e-4f6c-d104768a1214";
    public final static String ERROR_CHAR =     "19b10003-e8f2-537e-4f6c-d104768a1214";
    public final static String SWITCH_CHAR =    "19b10004-e8f2-537e-4f6c-d104768a1214";

    public final static String ACTION_GATT_CONNECTED =              "com.ideo.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =           "com.ideo.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =    "com.ideo.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =              "com.ideo.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =                         "com.ideo.EXTRA_DATA";

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBLEGatt != null) {
            if (mBLEGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }

        mBLEGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBLEGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBLEGatt.disconnect();
    }

    public void close() {
        if (mBLEGatt == null) {
            return;
        }
        mBLEGatt.close();
        mBLEGatt = null;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBLEGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);

            for(byte byteChar : data)
                stringBuilder.append(String.format("%d", byteChar));

            String post_data = characteristic.getUuid() + stringBuilder.toString();
            intent.putExtra(EXTRA_DATA, post_data);
        }
        sendBroadcast(intent);
    }

    public void readANGLE() {
        if (mBluetoothAdapter == null || mBLEGatt == null) {
            return;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBLEGatt.getService(UUID.fromString(ETRAIN_SERVICE));
        if(mCustomService == null){
           return;
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(UUID.fromString(ANGLE_CHAR));
        mBLEGatt.readCharacteristic(mReadCharacteristic);

    }

    public void readSQUAT() {
        if (mBluetoothAdapter == null || mBLEGatt == null) {
            return;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBLEGatt.getService(UUID.fromString(ETRAIN_SERVICE));
        if(mCustomService == null){
            return;
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(UUID.fromString(SQUAT_CHAR));
        mBLEGatt.readCharacteristic(mReadCharacteristic);

    }

    public void readERRORS() {
        if (mBluetoothAdapter == null || mBLEGatt == null) {
            return;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBLEGatt.getService(UUID.fromString(ETRAIN_SERVICE));
        if(mCustomService == null){
            return;
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(UUID.fromString(ERROR_CHAR));
        mBLEGatt.readCharacteristic(mReadCharacteristic);

    }

    /*
    Вторая стадия:
    После нажатия кнопки здесь происходит запись числа в память девайса
    класса BluetoothGatt методом writeCharacteristic...

    Вопрос: Что делается дальше при выполнении этого метода в сервисе?
     */
    public void writeCustomCharacteristic(int value) {
        if (mBluetoothAdapter == null || mBLEGatt == null) {
            return;
        }

        BluetoothGattService mCustomService = mBLEGatt.getService(UUID.fromString(ETRAIN_SERVICE));
        if(mCustomService == null){
           return;
        }

        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(SWITCH_CHAR));
        mWriteCharacteristic.setValue(value,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
        mBLEGatt.writeCharacteristic(mWriteCharacteristic);
    }
}
