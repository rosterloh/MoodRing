package com.rosterloh.moodring;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.UUID;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

public class MoodRing extends Activity implements ServiceConnection {
    private final static String TAG = MoodRing.class.getSimpleName();

    private TextView rssiValue = null;
    private ColorPicker picker;
    private SVBar svBar;
    private OpacityBar opacityBar;
    private SaturationBar saturationBar;
    private ValueBar valueBar;

    private BluetoothGattCharacteristic characteristicTx = null;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;
    private boolean mScanning;
    private Handler mHandler;
    private int mInterval = 500; // 500 milliseconds by default, can be changed later
    private Handler mScanHandler;
    private boolean mConnected = false;

    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;
    private static final int REQUEST_ENABLE_BT = 1;
    private byte[] data = new byte[3];

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                //setButtonDisable();
            } else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                mConnected = true;
                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                //readAnalogInValue(data);
            } else if (BluetoothLeService.ACTION_GATT_RSSI.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_ring);

        mHandler = new Handler();
        mScanHandler = new Handler();

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rssiValue = (TextView) findViewById(R.id.rssiValue);

        picker = (ColorPicker) findViewById(R.id.picker);
        SVBar svBar = (SVBar) findViewById(R.id.svbar);
        OpacityBar opacityBar = (OpacityBar) findViewById(R.id.opacitybar);
        //SaturationBar saturationBar = (SaturationBar) findViewById(R.id.saturationbar);
        ValueBar valueBar = (ValueBar) findViewById(R.id.valuebar);

        picker.addSVBar(svBar);
        picker.addOpacityBar(opacityBar);
        //picker.addSaturationBar(saturationBar);
        picker.addValueBar(valueBar);
        //picker.setOnColorChangedListener(this);

        findViewById(R.id.btnSet).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mConnected) {
                    int val = picker.getColor();
                    picker.setOldCenterColor(val);
                    byte buf[] = new byte[]{(byte) Color.red(val), (byte) Color.green(val), (byte) Color.blue(val)};
                    characteristicTx.setValue(buf);
                    mBluetoothLeService.writeCharacteristic(characteristicTx);
                }
            }
        });

        findViewById(R.id.btnOff).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mConnected) {
                    byte buf[] = new byte[]{(byte) 0, (byte) 0, (byte) 0};
                    characteristicTx.setValue(buf);
                    mBluetoothLeService.writeCharacteristic(characteristicTx);
                }
            }
        });

        /*
        opacitybar.setOnOpacityChangeListener(new OnOpacityChangeListener() {

        });

        valuebar.setOnValueChangeListener(new OnValueChangeListener() {

        });

        saturationBar.setOnSaturationChangeListener(new OnSaturationChangeListener () {

        });
        */
        //getActionBar().setTitle(mDeviceName);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        bindService(new Intent(this, BluetoothLeService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        scanLeDevice(true);
    }

    private void displayData(String data) {
        if (data != null) {
            rssiValue.setText(data+" dBm");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            mBluetoothLeService.readRssi();
            if(mConnected)
                mScanHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null) return;

        mStatusChecker.run();

        characteristicTx = gattService.getCharacteristic(BluetoothLeService.UUID_TX);

        BluetoothGattCharacteristic characteristicRx = gattService.getCharacteristic(BluetoothLeService.UUID_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx, true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if(mDevice != null) {
                        mDeviceAddress = mDevice.getAddress();
                        mBluetoothLeService.connect(mDeviceAddress);
                    } else {
                        Toast toast = Toast.makeText(MoodRing.this, "Couldn't find BLE device!", Toast.LENGTH_SHORT);
                        toast.setGravity(0, 0, Gravity.CENTER);
                        toast.show();
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            //UUID[] uuids = { BluetoothLeService.UUID_BLEND_SERVICE };
            //mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Found device "+device.getAddress());

                    byte[] serviceUuidBytes = new byte[16];
                    String serviceUuid = "";
                    for (int i = 31, j = 0; i >= 16; i--, j++) {
                        serviceUuidBytes[j] = scanRecord[i];
                    }
                    serviceUuid = BluetoothLeService.bytesToHex(serviceUuidBytes);
                    if (BluetoothLeService.stringToUuidString(serviceUuid).equals(
                            GattAttributes.BLEND_SERVICE.toUpperCase(Locale.ENGLISH))) {
                        mDevice = device;
                    }
                }
            });
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if(mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
            mBluetoothLeService = null;
        }
        mConnected = false;
        mScanHandler.removeCallbacks(mStatusChecker);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Wait for everything to shut down
            }
        }, 600);

        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mood_ring, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
        if (!mBluetoothLeService.initialize()) {
            Log.e(TAG, "Unable to initialize Bluetooth");
            finish();
        }
        // Automatically connects to the device upon successful start-up initialization.
        mBluetoothLeService.connect(mDeviceAddress);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mBluetoothLeService = null;
    }
}
