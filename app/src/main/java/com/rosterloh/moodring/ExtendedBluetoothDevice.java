package com.rosterloh.moodring;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 22/12/2015
 */

import android.bluetooth.BluetoothDevice;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class ExtendedBluetoothDevice {
    /** The device instance. */
    private BluetoothDevice mBluetoothDevice;
    /** The device name. The {@link BluetoothDevice#getName()} method returns the cached value of a name and it may not be the current one, if its firmware has changed. */
    private String mName;

    public ExtendedBluetoothDevice(final ScanResult result) {
        mBluetoothDevice = result.getDevice();
        if (result.getScanRecord() != null)
            mName = result.getScanRecord().getDeviceName();
        else
            mName = mBluetoothDevice.getName();
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    public String getName() {
        return mName;
    }

    public String getAddress() {
        return mBluetoothDevice.getAddress();
    }

    public boolean matches(final ScanResult scanResult) {
        return mBluetoothDevice.equals(scanResult.getDevice());
    }
}
