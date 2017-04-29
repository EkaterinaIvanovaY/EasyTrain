
package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import java.lang.*;

import java.util.ArrayList;

import static com.example.android.bluetoothlegatt.BluetoothLeService.EXTRA_DATA;

public class DeviceControlActivity extends Activity {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mDataField;
    private TextView isright;
    private TextView sqtView;
    private Vibrator vibr;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private final int STATE_START = 1;
    private final int STATE_STOP = 0;
    private int state = 0;
    public ArrayList<Integer> squats = new ArrayList();

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.button_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDataField = (TextView) findViewById(R.id.data_value);
        isright = (TextView) findViewById(R.id.isright);
        sqtView = (TextView) findViewById(R.id.isright);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        vibr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothLeService.ACTION_GATT_CONNECTED)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (action.equals(BluetoothLeService.ACTION_GATT_DISCONNECTED)) {
                mConnected = false;
                invalidateOptionsMenu();
                mDataField.setText(R.string.no_data);
            } else if (action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)) {
                displayData(intent.getStringExtra(EXTRA_DATA));
            }
        }
    };

    private void displayData(String data) {
        if (data != null) {

            StringBuffer data_value = new StringBuffer(data);
            StringBuffer data_uuid = new StringBuffer(data);
            data_value.delete(0, 36);
            data_uuid.delete(36, data_uuid.length());

            int data_int = Integer.parseInt(data_value.toString());
            String uuid_str = new String(data_uuid);

            if (uuid_str.equals(BluetoothLeService.ANGLE_CHAR)) {
                if (data_int > 45) {
                    mDataField.setText(data_value);
                    isright.setText("Good pace!");
                    squats.add(data_int);
                    vibr.cancel();
                } else {
                    mDataField.setText(data_value);
                    squats.add(data_int);
                    isright.setText("Wrong angle!");
                    vibr.vibrate(100);        // vibration for 100 ms
                }

                if (state == STATE_START && (mBluetoothLeService != null)) {
                    mBluetoothLeService.readCustomCharacteristic();
                }
            }
            if (uuid_str.equals(BluetoothLeService.SQUAT_CHAR)) {
                isright.setText(data_value);
            }
        }
    }

    public void onClickStart(View v) {
        if (mBluetoothLeService != null) {
            state = STATE_START;
            mBluetoothLeService.readCustomCharacteristic();
        }
    }

    public void onClickStop(View v) {
        state = STATE_STOP;
    }

    //Метод для перехода на новый экран
    public void newScreen(View v) {

        if (state == 0) {
            Intent intObj = new Intent(this, SecondActivity.class);
            intObj.putIntegerArrayListExtra("graph_intent", squats);
            startActivity(intObj);
        } else {
            isright.setText("Press Stop");
        }

    }
}