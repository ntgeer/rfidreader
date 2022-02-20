package android.iotcasinochips.rfidreader;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

/**
 * This file is for handling the components of the BTLE_Device, which in this case,
 * is only the reader
 */
public class BTLE_Device {

    private BluetoothDevice bluetoothDevice;
    private Context context;
    private int rssi;

    public BTLE_Device(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    // Get address of device. Check if permissions are valid before getting address
    public String getAddress() {return bluetoothDevice.getAddress();}

    // Get name of device. Check if permissions are valid before getting name
    public String getName() {return bluetoothDevice.getName();}

    // Set RSSI
    public void setRSSI(int rssi) {
        this.rssi = rssi;
    }

    // Get RSSI
    public int getRSSI() {
        return rssi;
    }
}
