package com.rosterloh.moodring;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.rosterloh.moodring.profile.BleManager;
import com.rosterloh.moodring.util.AnalyticsManager;

import java.util.List;
import java.util.UUID;

import static com.rosterloh.moodring.util.LogUtils.LOGD;
import static com.rosterloh.moodring.util.LogUtils.LOGE;
import static com.rosterloh.moodring.util.LogUtils.LOGW;
import static com.rosterloh.moodring.util.LogUtils.makeLogTag;
import static com.rosterloh.moodring.util.ParseUtils.bytesToHex;

/**
 * MoodManager class performs BluetoothGatt operations for connection, service discovery, enabling indication and reading characteristics.
 * Service and reading Mood Ring values are performed here. MoodRingActivity implements MoodManagerCallbacks in order to receive callbacks of BluetoothGatt operations
 *
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 17/10/2014
 */
public class MoodManager implements BleManager<MoodManagerCallbacks> {
    private final String TAG = makeLogTag(MoodManager.class);
    private MoodManagerCallbacks mCallbacks;
    private BluetoothGatt mBluetoothGatt;
    private Context mContext;

    public final static UUID MOOD_SERVICE_UUID = UUID.fromString("713d0000-503e-4c75-ba94-3148f18d941e");

    private static final UUID MOOD_RX_CHARACTERISTIC_UUID = UUID.fromString("713d0002-503e-4c75-ba94-3148f18d941e");
    private static final UUID MOOD_TX_CHARACTERISTIC_UUID = UUID.fromString("713d0003-503e-4c75-ba94-3148f18d941e");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final static UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    private final static UUID DEVICE_MANUFACTURER_NAME_CHARACTERISTIC = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");

    private final static UUID BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private final static UUID BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");

    private final static String ERROR_CONNECTION_STATE_CHANGE = "Error on connection state change";
    private final static String ERROR_DISCOVERY_SERVICE = "Error on discovering services";
    private final static String ERROR_READ_CHARACTERISTIC = "Error on reading characteristic";
    private final static String ERROR_WRITE_DESCRIPTOR = "Error on writing descriptor";
    private final static String ERROR_AUTH_ERROR_WHILE_BONDED = "Phone has lost bonding information";

    private BluetoothGattCharacteristic mTxCharacteristic, mRxCharacteristic, mBatteryCharacteritsic;

    private static MoodManager managerInstance = null;

    /**
     * singleton implementation of MoodManager class
     */
    public static synchronized MoodManager getMoodManager() {
        if (managerInstance == null) {
            managerInstance = new MoodManager();
        }
        return managerInstance;
    }

    /**
     * callbacks for activity {MoodRingActivity} that implements MoodManagerCallbacks interface activity use this method to register itself for receiving callbacks
     */
    @Override
    public void setGattCallbacks(MoodManagerCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    @Override
    public void connect(Context context, BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        mContext = context;
    }

    @Override
    public void disconnect() {
        LOGD(TAG, "Disconnecting device");
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    private BroadcastReceiver mBondingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
            final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

            LOGD(TAG, "Bond state changed for: " + device.getAddress() + " new state: " + bondState + " previous: " + previousBondState);

            // skip other devices
            if (!device.getAddress().equals(mBluetoothGatt.getDevice().getAddress()))
                return;

            if (bondState == BluetoothDevice.BOND_BONDED) {
                // We've read Battery Level, now enabling RX indications
                if (mRxCharacteristic != null) {
                    enableRxNotification();
                }
                mContext.unregisterReceiver(this);
                mCallbacks.onBonded();
            }
        }
    };

    /**
     * BluetoothGatt callbacks for connection/disconnection, service discovery, receiving indication, etc
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    LOGD(TAG, "Device connected");
                    AnalyticsManager.sendEvent("BLE", "State", "Connect", 1L);
                    mBluetoothGatt.discoverServices();
                    //This will send callback to HTSActivity when device get connected
                    mCallbacks.onDeviceConnected();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    LOGD(TAG, "Device disconnected");
                    AnalyticsManager.sendEvent("BLE", "State", "Connect", 0L);
                    //This will send callback to MoodRingActivity when device get disconnected
                    mCallbacks.onDeviceDisconnected();
                }
            } else {
                mCallbacks.onError(ERROR_CONNECTION_STATE_CHANGE, status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    if (service.getUuid().equals(MOOD_SERVICE_UUID)) {
                        mRxCharacteristic = service.getCharacteristic(MOOD_RX_CHARACTERISTIC_UUID);
                        mTxCharacteristic = service.getCharacteristic(MOOD_TX_CHARACTERISTIC_UUID);
                    } else if (service.getUuid().equals(BATTERY_SERVICE)) {
                        mBatteryCharacteritsic = service.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC);
                    }
                }
                if (mRxCharacteristic != null) {
                    mCallbacks.onServicesDiscovered(false);
                } else {
                    mCallbacks.onDeviceNotSupported();
                    gatt.disconnect();
                    return;
                }
                if (mBatteryCharacteritsic != null) {
                    readBatteryLevel();
                } else {
                    enableRxNotification();
                }
            } else {
                mCallbacks.onError(ERROR_DISCOVERY_SERVICE, status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().equals(BATTERY_LEVEL_CHARACTERISTIC)) {
                    int batteryValue = characteristic.getValue()[0];
                    mCallbacks.onBatteryValueReceived(batteryValue);
                    AnalyticsManager.sendEvent("BLE", "Read", "Battery", batteryValue);

                    enableRxNotification();
                }
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_NONE) {
                    LOGW(TAG, ERROR_AUTH_ERROR_WHILE_BONDED);
                    mCallbacks.onError(ERROR_AUTH_ERROR_WHILE_BONDED, status);
                }
            } else {
                mCallbacks.onError(ERROR_READ_CHARACTERISTIC, status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(MOOD_RX_CHARACTERISTIC_UUID)) {
                try {
                    int flag = characteristic.getProperties();
                    //int format = -1;
                    if ((flag & 0x01) != 0) {
                        //format = BluetoothGattCharacteristic.FORMAT_UINT16;
                        LOGD(TAG, "RX format UINT16.");
                    } else {
                        //format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        LOGD(TAG, "RX format UINT8.");
                    }
                    final byte[] rx = characteristic.getValue(); // characteristic.getIntValue(format, 1);
                    LOGD(TAG, "RX: " + bytesToHex(rx));
                    mCallbacks.onRxValueReceived(rx);
                } catch (Exception e) {
                    Log.e(TAG, "invalid temperature value");
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Rx notifications have been enabled
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    mCallbacks.onBondingRequired();

                    final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    mContext.registerReceiver(mBondingBroadcastReceiver, filter);
                } else {
                    Log.w(TAG, ERROR_AUTH_ERROR_WHILE_BONDED);
                    mCallbacks.onError(ERROR_AUTH_ERROR_WHILE_BONDED, status);
                }
            } else {
                LOGE(TAG, ERROR_WRITE_DESCRIPTOR + " (" + status + ")");
                mCallbacks.onError(ERROR_WRITE_DESCRIPTOR, status);
            }
        }
    };

    public void readBatteryLevel() {
        if (mBatteryCharacteritsic != null) {
            mBluetoothGatt.readCharacteristic(mBatteryCharacteritsic);
        } else {
            LOGE(TAG, "Battery Level Characteristic is null");
        }
    }

    /**
     * enable notification on the RX characteristic
     */
    private void enableRxNotification() {
        mBluetoothGatt.setCharacteristicNotification(mRxCharacteristic, true);
        BluetoothGattDescriptor descriptor = mRxCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    @Override
    public void closeBluetoothGatt() {
        try {
            mContext.unregisterReceiver(mBondingBroadcastReceiver);
        } catch (Exception e) {
            // the receiver must have been not registered or unregistered before
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            mBatteryCharacteritsic = null;
            mRxCharacteristic = null;
            mTxCharacteristic = null;
        }
    }
}
