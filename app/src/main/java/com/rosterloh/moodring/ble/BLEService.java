package com.rosterloh.moodring.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import com.rosterloh.moodring.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 22/12/2015
 */
public class BLEService extends Service {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final String TAG = "BLE";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.rosterloh.moodring.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.rosterloh.moodring.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.rosterloh.moodring.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.rosterloh.moodring.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.rosterloh.moodring.EXTRA_DATA";
    public final static String ACTION_CHARACTERISTIC_WRITE_COMPLETE = "com.rosterloh.moodring.ACTION_CHARACTERISTIC_WRITE_COMPLETE";
    public final static String ACTION_DISMISS_DIALOG = "com.rosterloh.moodring.ACTION_DISMISS_DIALOG";
    private final static ParcelUuid MOOD_SERVICE = ParcelUuid.fromString("54207799-8F40-4FE5-BEBE-6BB7022D3E73");
    public final static UUID LED_CHARACTERISTIC = UUID.fromString("542077A9-8F40-4FE5-BEBE-6BB7022D3E73");

    // Characteristics
    private BluetoothGattCharacteristic mLedCharacteristic;
    private boolean mLedId = false;

    private final IBinder mBinder = new LocalBinder();
    public boolean mIdentifySelected = false;

    public class LocalBinder extends Binder {
        BLEService getService() { return BLEService.this;}
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

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private final BluetoothGattCallback mBLEGattCallBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                close();
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG, "GAT success");
                final BluetoothGattService service = gatt.getService(MOOD_SERVICE.getUuid());
                if(service != null){
                    mLedCharacteristic = service.getCharacteristic(LED_CHARACTERISTIC);
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                }

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(mIdentifySelected && status == 0){
                broadcastUpdate(ACTION_CHARACTERISTIC_WRITE_COMPLETE);
                return;
            }
            if(mLedId && status == 0){
                mLedId = false;
                writeLedCharacteristic();
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }

        sendBroadcast(intent);
    }

    public boolean initialiseBluetooth(){
        if(mBluetoothManager == null){
            mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            if(mBluetoothManager == null){
                Log.v(TAG, "BluetoothManager initialization failed");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter == null){
            Log.v(TAG, "BluetoothAdapter initialisation failed");
            return false;
        }

        return true;
    }

    public boolean connect(final String address){
        if(mBluetoothAdapter == null || address == null){
            Log.v(TAG,"BluetoothAdapter is not initialised or wrong address");
            return false;
        }

        //Previously connected device. Tru to reconnect.
        if(address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null){
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice bleDevice = mBluetoothAdapter.getRemoteDevice(address);
        if(bleDevice == null){
            Log.v(TAG, "Device not found unable to connect");
            return false;
        }

        mBluetoothGatt = bleDevice.connectGatt(this, false, mBLEGattCallBack);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect(){
        if(mBluetoothAdapter == null || mBluetoothGatt == null){
            Log.v(TAG, "Bluetooth adapter not initialised");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close()
    {
        if(mBluetoothGatt == null)
            return;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void writeLedCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Toast.makeText(BLEService.this, getString(R.string.check_bluetooth), Toast.LENGTH_SHORT).show();
            return;
        }

        if(mLedCharacteristic == null){
            Toast.makeText(this, getString(R.string.led_char_error), Toast.LENGTH_SHORT).show();
        }

        final byte[] serviceData = new byte[1];
        ByteBuffer bb = ByteBuffer.wrap(serviceData);
        bb.order(ByteOrder.BIG_ENDIAN);
        //bb.put(((byte) Integer.parseInt(mOpCode)));
        bb.putInt(0x00);
        byte[] data = bb.array();
        mLedCharacteristic.setValue(data);
        mLedId = mBluetoothGatt.writeCharacteristic(mLedCharacteristic);
    }

}
