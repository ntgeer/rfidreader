package android.iotcasinochips.rfidreader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;

import java.util.UUID;

/**
 * Used to scan for RFID readers.
 */
public class Scanner_BTLE {

    private MainActivity ma;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private long scanPeriod;
    private int signalStrength;

    public Scanner_BTLE(MainActivity mainActivity, long scanPeriod, int signalStrength) {
        ma = mainActivity;

        mHandler = new Handler();

        // Control how long scan lasts
        this.scanPeriod = scanPeriod;
        // Control the threshold signal strength
        this.signalStrength = signalStrength;

        // Create BluetoothManager to reference Bluetooth service on phone
        final BluetoothManager bluetoothManager =
                (BluetoothManager) ma.getSystemService(Context.BLUETOOTH_SERVICE);

        // Instantiate Bluetooth adapter to reference Bluetooth module on phone
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    // is app scanning?
    public boolean isScanning() {
        return mScanning;
    }

    // Starts scan
    public void start() {
        if (!Utils.checkBluetooth(mBluetoothAdapter)) {
            Utils.requestUserBluetooth(ma);
            ma.stopScan();
        }
        else {
            scanForSURFERs(true);
        }
    }

    // Stops scan
    public void stop() {
        scanForSURFERs(false);
    }

    // Scans for ONLY SURFERs.
    private void scanForSURFERs(final boolean enable) {
        if (enable && !mScanning) {
            Utils.toast(ma.getApplicationContext(), "Scanning for SURFERs...");

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Utils.toast(ma.getApplicationContext(), "Stopping scan...");

                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    ma.stopScan();
                }
            }, scanPeriod);

            mScanning = true;

            // Get SURFER RFID UUID type to ONLY search for SURFER RFID readers
            UUID surferServiceUUID[] = {UUID.fromString("e7560001-fc1d-8db5-ad46-26e5843b5915")};

            mBluetoothAdapter.startLeScan(surferServiceUUID, mLeScanCallback);
        }
        // Stop the scan
        else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                // Called whenever RFID Reader is found
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

                    // Store RSSI value
                    final int new_rssi = rssi;
                    if (rssi > signalStrength) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                ma.addDevice(device, new_rssi);
                            }
                        });
                    }
                }
            };
}