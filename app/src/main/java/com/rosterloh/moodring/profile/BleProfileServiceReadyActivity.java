/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package com.rosterloh.moodring.profile;

import java.util.UUID;

import com.rosterloh.moodring.R;
import com.rosterloh.moodring.scanner.ScannerFragment;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * <p>
 * The {@link BleProfileServiceReadyActivity} activity is designed to be the base class for profile activities that uses services in order to connect to the device. When user press CONNECT button a
 * service is created and the activity binds to it. The service tries to connect to the service and notifies the activity using Local Broadcasts ({@link LocalBroadcastManager}). See
 * {@link BleProfileService} for messages. If the device is not in range it will listen for it and connect when it become visible. The service exists until user will press DISCONNECT button.
 * </p>
 * <p>
 * When user closes the activity (f.e. by pressing Back button) while being connected, the Service remains working. It's still connected to the device or still listens for it. When entering back to
 * the activity, activity will to bind to the service and refresh UI.
 * </p>
 */
public abstract class BleProfileServiceReadyActivity<E extends BleProfileService.LocalBinder> extends ActionBarActivity implements
            ScannerFragment.OnDeviceSelectedListener {
	private static final String TAG = "BleProfileServiceReadyActivity";

	private static final String DEVICE_NAME = "device_name";
    private static final String LOG_URI = "log_uri";
	protected static final int REQUEST_ENABLE_BT = 2;

	private E mService;

	private TextView mDeviceNameView;
	private TextView mBatteryLevelView;
	private Button mConnectButton;

	private String mDeviceName;

	private BroadcastReceiver mCommonBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			if (BleProfileService.BROADCAST_CONNECTION_STATE.equals(action)) {
				final int state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED);

				switch (state) {
				case BleProfileService.STATE_CONNECTED: {
					mDeviceName = intent.getStringExtra(BleProfileService.EXTRA_DEVICE_NAME);
					onDeviceConnected();
					break;
				}
				case BleProfileService.STATE_DISCONNECTED: {
					onDeviceDisconnected();
					mDeviceName = null;
					break;
				}
				case BleProfileService.STATE_LINK_LOSS: {
					onLinklossOccur();
					break;
				}
				case BleProfileService.STATE_CONNECTING:
				case BleProfileService.STATE_DISCONNECTING:
					// current implementation does nothing in this states
				default:
					// there should be no other actions
					break;

				}
			} else if (BleProfileService.BROADCAST_SERVICES_DISCOVERED.equals(action)) {
				final boolean primaryService = intent.getBooleanExtra(BleProfileService.EXTRA_SERVICE_PRIMARY, false);
				final boolean secondaryService = intent.getBooleanExtra(BleProfileService.EXTRA_SERVICE_SECONDARY, false);

				if (primaryService) {
					onServicesDiscovered(secondaryService);
				} else {
					onDeviceNotSupported();
				}
			} else if (BleProfileService.BROADCAST_BOND_STATE.equals(action)) {
				final int state = intent.getIntExtra(BleProfileService.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
				switch (state) {
				case BluetoothDevice.BOND_BONDING:
					onBondingRequired();
					break;
				case BluetoothDevice.BOND_BONDED:
					onBonded();
					break;
				}
			} else if (BleProfileService.BROADCAST_BATTERY_LEVEL.equals(action)) {
				final int value = intent.getIntExtra(BleProfileService.EXTRA_BATTERY_LEVEL, -1);
				if (value > 0)
					onBatteryValueReceived(value);
			} else if (BleProfileService.BROADCAST_ERROR.equals(action)) {
				final String message = intent.getStringExtra(BleProfileService.EXTRA_ERROR_MESSAGE);
				final int errorCode = intent.getIntExtra(BleProfileService.EXTRA_ERROR_CODE, 0);
				onError(message, errorCode);
			}
		};
	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@SuppressWarnings("unchecked")
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			final E bleService = mService = (E) service;
			Log.d(TAG, "Activity binded to the service");
			onServiceBinded(bleService);

			// update UI
			mDeviceName = bleService.getDeviceName();
			mDeviceNameView.setText(mDeviceName);
			mConnectButton.setText(R.string.action_disconnect);

			// and notify user if device is connected
			if (bleService.isConnected())
				onDeviceConnected();
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			Log.d(TAG, "Activity disconnected from the service");
			mDeviceNameView.setText(getDefaultDeviceName());
			mConnectButton.setText(R.string.action_connect);

			mService = null;
			mDeviceName = null;
			onServiceUnbinded();
		}
	};

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ensureBLESupported();
		if (!isBLEEnabled()) {
			showBLEDialog();
		}

		/*
		 * In this example we use the ProximityManager in the service. This class communicates with the service using local broadcasts. Final activity may bind to the Server
		 * to use its interface.
		 */
		onInitialize(savedInstanceState);
		onCreateView(savedInstanceState);
		onViewCreated(savedInstanceState);

		LocalBroadcastManager.getInstance(this).registerReceiver(mCommonBroadcastReceiver, makeIntentFilter());
	}

	@Override
	protected void onStart() {
		super.onStart();

		/*
		 * If the service has not been started before the following lines will not start it. However, if it's running, the Activity will be binded to it
		 * and notified via mServiceConnection.
		 */
		final Intent service = new Intent(this, getServiceClass());
        if (bindService(service, mServiceConnection, 0)) // we pass 0 as a flag so the service will not be created if not exists
            Log.d(TAG, "Binding to the service..."); // (* - see the comment below)

		/*
		 * * - When user exited the UARTActivity while being connected the log session is kept in the service. We may not get it before binding to it so in this
		 * case this event will not be logged. It will, however, be logged after the orientation changes.
		 */
	}

	@Override
	protected void onStop() {
		super.onStop();

		try {
			Log.d(TAG, "Unbinding from the service...");
			unbindService(mServiceConnection);
			mService = null;

			Log.d(TAG, "Activity unbinded from the service");
			onServiceUnbinded();
			mDeviceName = null;
		} catch (final IllegalArgumentException e) {
			// do nothing, we were not connected to the sensor
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mCommonBroadcastReceiver);
	}

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BleProfileService.BROADCAST_CONNECTION_STATE);
		intentFilter.addAction(BleProfileService.BROADCAST_SERVICES_DISCOVERED);
		intentFilter.addAction(BleProfileService.BROADCAST_BOND_STATE);
		intentFilter.addAction(BleProfileService.BROADCAST_BATTERY_LEVEL);
		intentFilter.addAction(BleProfileService.BROADCAST_ERROR);
		return intentFilter;
	}

	/**
	 * Called when activity binds to the service. The parameter is the object returned in {@link Service#onBind(Intent)} method in your service. The method is
     * called when device gets connected or is created while sensor was connected before. You may use the binder as a sensor interface.
	 */
	protected abstract void onServiceBinded(E binder);

	/**
	 * Called when activity unbinds from the service. You may no longer use this binder because the sensor was disconnected. This method is also called when you
     * leave the activity being connected to the sensor in the background.
	 */
	protected abstract void onServiceUnbinded();

	/**
	 * Returns the service class for sensor communication. The service class must derive from {@link BleProfileService} in order to operate with this class.
	 * 
	 * @return the service class
	 */
	protected abstract Class<? extends BleProfileService> getServiceClass();

	/**
	 * Returns the service interface that may be used to communicate with the sensor. This will return <code>null</code> if the device is disconnected from the
     * sensor.
	 * 
	 * @return the service binder or <code>null</code>
	 */
	protected BleProfileService.LocalBinder getService() {
		return mService;
	}

	/**
	 * You may do some initialization here. This method is called from {@link #onCreate(Bundle)} before the view was created.
	 */
	protected void onInitialize(final Bundle savedInstanceState) {
		// empty default implementation
	}

	/**
	 * Called from {@link #onCreate(Bundle)}. This method should build the activity UI, f.e. using {@link #setContentView(int)}. Use to obtain references to
     * views. Connect/Disconnect button, the device name view and battery level view are manager automatically.
	 * 
	 * @param savedInstanceState
	 *            contains the data it most recently supplied in {@link #onSaveInstanceState(Bundle)}. Note: <b>Otherwise it is null</b>.
	 */
	protected abstract void onCreateView(final Bundle savedInstanceState);

	/**
	 * Called after the view has been created.
	 * 
	 * @param savedInstanceState
     *            contains the data it most recently supplied in {@link #onSaveInstanceState(Bundle)}. Note: <b>Otherwise it is null</b>.
	 */
	protected final void onViewCreated(final Bundle savedInstanceState) {
		// set GUI
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
		mConnectButton = (Button) findViewById(R.id.action_connect);
		mDeviceNameView = (TextView) findViewById(R.id.device_name);
		mBatteryLevelView = (TextView) findViewById(R.id.battery);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(DEVICE_NAME, mDeviceName);
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mDeviceName = savedInstanceState.getString(DEVICE_NAME);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		//getMenuInflater().inflate(R.menu.help, menu);
		return true;
	}

	/**
	 * Use this method to handle menu actions other than home and about.
	 * 
	 * @param itemId
	 *            the menu item id
	 * @return <code>true</code> if action has been handled
	 */
	protected boolean onOptionsItemSelected(final int itemId) {
		// Overwrite when using menu other than R.menu.help
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final int id = item.getItemId();
		switch (id) {
		case android.R.id.home:
			onBackPressed();
			break;
		default:
			return onOptionsItemSelected(id);
		}
		return true;
	}

	/**
	 * Called when user press CONNECT or DISCONNECT button. See layout files -> onClick attribute.
	 */
	public void onConnectClicked(final View view) {
		if (isBLEEnabled()) {
			if (mService == null) {
				setDefaultUI();
				showDeviceScanningDialog(getFilterUUID(), isDiscoverableRequired());
			} else {
				Log.v(TAG, "Disconnecting...");
				mService.disconnect();
			}
		} else {
			showBLEDialog();
		}
	}

	/**
	 * Returns the title resource id that will be used to create logger session. If 0 is returned (default) logger will not be used.
	 * 
	 * @return the title resource id
	 */
	protected int getLoggerProfileTitle() {
		return 0;
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device, final String name) {
		final int titleId = getLoggerProfileTitle();
		mDeviceNameView.setText(mDeviceName = name);
		mConnectButton.setText(R.string.action_disconnect);

		// The device may not be in the range but the service will try to connect to it if it reach it
		Log.v(TAG, "Creating service...");
		final Intent service = new Intent(this, getServiceClass());
		service.putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, device.getAddress());
		startService(service);
        Log.d(TAG, "Binding to the service...");
		bindService(service, mServiceConnection, 0);
	}

	@Override
	public void onDialogCanceled() {
		// do nothing
	}

	/**
	 * Called when the device has been connected. This does not mean that the application may start communication. A service discovery will be handled
     * automatically after this call. Service discovery may ends up with calling {@link #onServicesDiscovered()} or {@link #onDeviceNotSupported()} if required
     * services have not been found.
	 */
	public void onDeviceConnected() {
		mDeviceNameView.setText(mDeviceName);
		mConnectButton.setText(R.string.action_disconnect);
	}

	/**
	 * Called when the device has disconnected (when the callback returned
     * {@link BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} with state DISCONNECTED.
	 */
	public void onDeviceDisconnected() {
		mConnectButton.setText(R.string.action_connect);
		mDeviceNameView.setText(getDefaultDeviceName());
        if (mBatteryLevelView != null)
            mBatteryLevelView.setText(R.string.not_available);

		try {
			Log.d(TAG, "Unbinding from the service...");
			unbindService(mServiceConnection);
			mService = null;

			Log.d(TAG, "Activity unbinded from the service");
			onServiceUnbinded();
			mDeviceName = null;
		} catch (final IllegalArgumentException e) {
			// do nothing. This should never happen but does...
		}
	}

	/**
	 * Some profiles may use this method to notify user that the link was lost. You must call this method in youe Ble Manager instead of
     * {@link #onDeviceDisconnected()} while you discover disconnection not initiated by the user.
	 */
	public void onLinklossOccur() {
        if (mBatteryLevelView != null)
            mBatteryLevelView.setText(R.string.not_available);
	}

	/**
	 * Called when service discovery has finished and primary services has been found. The device is ready to operate. This method is not called if the primary,
     * mandatory services were not found during service discovery. For example in the Blood Pressure Monitor, a Blood Pressure service is a primary service and
     * Intermediate Cuff Pressure service is a optional secondary service. Existence of battery service is not notified by this call.
	 * 
	 * @param optionalServicesFound
	 *            if <code>true</code> the secondary services were also found on the device.
	 */
	public abstract void onServicesDiscovered(final boolean optionalServicesFound);

	/**
	 * Called when the device has started bonding process
	 */
	public void onBondingRequired() {
		// empty default implementation
	}

	/**
	 * Called when the device has finished bonding process successfully
	 */
	public void onBonded() {
		// empty default implementation
	}

	/**
	 * Called when service discovery has finished but the main services were not found on the device. This may occur when connecting to bonded device that does
     * not support required services.
	 */
	public void onDeviceNotSupported() {
		showToast(R.string.not_supported);
	}

	/**
	 * Called when battery value has been received from the device
	 * 
	 * @param value
	 *            the battery value in percent
	 */
	public void onBatteryValueReceived(final int value) {
        if (mBatteryLevelView != null)
            mBatteryLevelView.setText(getString(R.string.battery, value));
	}

	/**
	 * Called when a BLE error has occurred
	 * 
	 * @param message
	 *            the error message
	 * @param errorCode
	 *            the error code
	 */
	public void onError(final String message, final int errorCode) {
		Log.e(TAG, "Error occured: " + message + ",  error code: " + errorCode);
		showToast(message + " (" + errorCode + ")");

		// refresh UI when connection failed
		onDeviceDisconnected();
	}

	/**
	 * Shows a message as a Toast notification. This method is thread safe, you can call it from any thread
	 * 
	 * @param message
	 *            a message to be shown
	 */
	protected void showToast(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(BleProfileServiceReadyActivity.this, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	/**
	 * Shows a message as a Toast notification. This method is thread safe, you can call it from any thread
	 * 
	 * @param messageResId
	 *            an resource id of the message to be shown
	 */
	protected void showToast(final int messageResId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(BleProfileServiceReadyActivity.this, messageResId, Toast.LENGTH_SHORT).show();
			}
		});
	}

	/**
	 * Returns <code>true</code> if the device is connected. Services may not have been discovered yet.
	 */
	protected boolean isDeviceConnected() {
		return mService != null;
	}

	/**
	 * Returns the name of the device that the phone is currently connected to or was connected last time
	 */
	protected String getDeviceName() {
		return mDeviceName;
	}

	/**
	 * Restores the default UI before reconnecting
	 */
	protected abstract void setDefaultUI();

	/**
	 * Returns the default device name resource id. The real device name is obtained when connecting to the device. This one is used when device has
     * disconnected.
	 * 
	 * @return the default device name resource id
	 */
	protected abstract int getDefaultDeviceName();

	/**
	 * Returns the string resource id that will be shown in About box
	 * 
	 * @return the about resource id
	 */
	protected abstract int getAboutTextId();

	/**
	 * The UUID filter is used to filter out available devices that does not have such UUID in their advertisement packet. See also:
     * {@link #isChangingConfigurations()}.
	 * 
	 * @return the required UUID or <code>null</code>
	 */
	protected abstract UUID getFilterUUID();

    /**
     * Whether the scanner must search only for devices with GENERAL_DISCOVERABLE or LIMITER_DISCOVERABLE flag set.
     *
     * @return <code>true</code> if devices must have one of those flags set in their advertisement packets
     */
    protected boolean isDiscoverableRequired() {
        return true;
    }

	/**
	 * Shows the scanner fragment.
	 * 
	 * @param filter
	 *            the UUID filter used to filter out available devices. The fragment will always show all bonded devices as there is no information about their
     *            services
	 * @param discoverableRequired
	 *            <code>true</code> if filter is a custom UUID, <code>false</code> if derived from base SIG UUID
	 * @see #getFilterUUID()
	 * @see #isDiscoverableRequired()
	 */
	private void showDeviceScanningDialog(final UUID filter, final boolean discoverableRequired) {
		final ScannerFragment dialog = ScannerFragment.getInstance(BleProfileServiceReadyActivity.this, filter, discoverableRequired);
		dialog.show(getFragmentManager(), "scan_fragment");
	}

	private void ensureBLESupported() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	protected boolean isBLEEnabled() {
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		final BluetoothAdapter adapter = bluetoothManager.getAdapter();
		return adapter != null && adapter.isEnabled();
	}

	protected void showBLEDialog() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	}
}
