package android.iotcasinochips.rfidreader;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


/**
 * Manages connection and data transfer of Bluetooth LE peripheral
 */
/* Overwritten to be our SURFERPeripheral from the IOS code, I don't want to overwrite it
 * or change it to a different name, because it's a public thing. We can clean up the code later
 * after proving that it works.
 */
public class Service_BTLE_GATT extends Service {
    private final static String TAG = Service_BTLE_GATT.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public BluetoothGattCharacteristic checkDescriptor;
    public int checkSuccess;

    public String logMessage;

    // SURFER CHANGE: This is to notify updateCharacteristic which characteristic was changed for the SM
    public String changedCharacteristicUUID = null;

    /*public int activityState = UNKNOWN;

    private final static int IDLE_UNCONFIGURED = 0;
    private final static int IDLE_CONFIGURED = 1;
    private final static int INITIALIZING = 2;
    private final static int SEARCHING_APP_SPECD = 3;
    private final static int SEARCHING_LAST_INV = 4;
    private final static int INVENTORYING = 5;
    private final static int TESTING_DTC = 6;
    private final static int PROG_APP_SPECD = 7;
    private final static int PROG_LAST_INV = 8;
    private final static int RECOV_WVFM_MEM = 9;
    private final static int RESET_ASICS = 10;
    private final static int KILL_TAG = 11;
    private final static int PROG_TAG_KILL_PW = 12;
    private final static int TRACK_APP_SPECD = 13;
    private final static int TRACK_LAST_INV = 14;
    private final static int UNKNOWN = 15;*/

    public final static String ACTION_GATT_CONNECTED = "android.iotcasinochips.rfidreader.Service_BTLE_GATT.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "android.iotcasinochips.rfidreader.Service_BTLE_GATT.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "android.iotcasinochips.rfidreader.Service_BTLE_GATT.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "android.iotcasinochips.rfidreader.Service_BTLE_GATT.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_UUID = "android.iotcasinochips.rfidreader.Service_BTLE_GATT.EXTRA_UUID";
    public final static String EXTRA_DATA = "android.iotcasinochips.rfidreader.Service_BTLE_GATT.EXTRA_DATA";


    public int getCheckSuccess(){
        Log.i("checkSuccess",String.valueOf(checkSuccess));
        return checkSuccess;
    }

    // Broadcasts/callback events for GATT events
    // Broadcast not viewable in Toast message
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        // Broadcast if connected or disconnected to GATT server
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            // Callback saying the app connected to the GATT server
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;

                mConnectionState = STATE_CONNECTED;

                broadcastUpdate(intentAction);

                Log.i(TAG, "Connected to GATT server.");

                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            }

            // Callback saying the app disconnected from the GATT server
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;

                mConnectionState = STATE_DISCONNECTED;

                Log.i(TAG, "Disconnected from GATT server.");

                broadcastUpdate(intentAction);
            }
        }

        // Broadcast when services discovered
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
            else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        // Check descriptor write status
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor,
                                      int status){

            checkSuccess = status;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluetoothGattDescriptor", "Descriptor Changed: " + descriptor.getCharacteristic().getUuid().toString());
                checkDescriptor = descriptor.getCharacteristic();
            }
        }


        // Broadcast when a characteristic is read
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.i("BluetoothGatt", "Characteristic Data Read");
            }
        }

        // Broadcast when a characteristic is changed
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            // This is supposedly the function that deals with unprompted responses. When we change
            // state data on the MCU it is supposed to send a response back with the respective
            // characteristics. This is where we'll receive said characteristic.
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            Log.i("BluetoothGatt", "Characteristic Data Changed");
            if (characteristic.getUuid().toString().equals(Activity_BTLE_Services.logMessageCharacteristicUUID)){
                Log.i("SURFER",new String(characteristic.getValue()));

            }
        }

        // Send broadcast and Toast message when characteristic written
        // This is a callback, so once we write this function is called? Need to check
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.i("BluetoothGatt", "Wrote Data");
            }
            else if (status == BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH ) {
                Log.i("BluetoothGatt", "Write Invalid Attribute Length");
            }
            else if( status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED ) {
                Log.i( "BluetoothGatt", "Write not Permitted");
            }
            else {
                Log.i("BluetoothGatt", "Write Failed for Unkown Reason");
            }
        }
    };

    // Do something weird with broadcasts.
    // NOT the same as Toast messages
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    // Ditto
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {

        final Intent intent = new Intent(action);

        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());

        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();

        // SURFER CHANGE: Update changedCharacteristicUUID for updateCharacteristic SM
        changedCharacteristicUUID = characteristic.getUuid().toString();

        if (data != null && data.length > 0) {

            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + Utils.hexToString(data));
        }
        else {
            intent.putExtra(EXTRA_DATA, "0");
        }

        sendBroadcast(intent);
    }

    // Gets reference to BTLE service
    public class BTLeServiceBinder extends Binder {

        Service_BTLE_GATT getService() {
            return Service_BTLE_GATT.this;
        }
    }

    // Weird binding function
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    // After using a given device, you should make sure that BluetoothGatt.close() is called
    // such that resources are cleaned up properly.  In this particular example, close() is
    // invoked when the UI is disconnected from the Service.
    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new BTLeServiceBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");

            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            }
            else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        // Maybe we can look into reimplementing this later.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.disconnect();
    }

    /**
     * When finished with a device, dismiss all services and release resources from device
     * */
    @SuppressLint("MissingPermission")
    public void close() {

        if (mBluetoothGatt == null) {
            return;
        }

        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}. The write result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    @SuppressLint("MissingPermission")
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(getString(R.string.CLIENT_CHARACTERISTIC_CONFIG)));

        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        }
        else {
            //descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    @SuppressLint("MissingPermission")
    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(getString(R.string.CLIENT_CHARACTERISTIC_CONFIG)));

        if (enabled) {
            // SURFER CHANGE: Changed to use an Indication, so that we will receive responses
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        }
        else {
            //descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {

        if (mBluetoothGatt == null) {
            return null;
        }

        return mBluetoothGatt.getServices();
    }

    /* void surferCharacteristicHandler() {

    }*/
}