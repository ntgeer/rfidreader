package android.iotcasinochips.rfidreader;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * This is the Java controller file for activity_btle_services.xml . This is where you can find
 * the click listeners for the three buttons
 *
 * */

public class Activity_BTLE_Services extends AppCompatActivity implements ExpandableListView.OnChildClickListener {
    private final static String TAG = Activity_BTLE_Services.class.getSimpleName();
    public static final String EXTRA_NAME = "android.iotcasinochips.rfidreader.Activity_BTLE_Services.NAME";
    public static final String EXTRA_ADDRESS = "android.iotcasinochips.rfidreader.Activity_BTLE_Services.ADDRESS";

    private ListAdapter_BTLE_Services expandableListAdapter;
    private ExpandableListView expandableListView;

    private ArrayList<BluetoothGattService> services_ArrayList;
    private HashMap<String, BluetoothGattCharacteristic> characteristics_HashMap;
    private HashMap<String, ArrayList<BluetoothGattCharacteristic>> characteristics_HashMapList;

    // SURFER Service and Characteristics, this is how we'll be accessing them if they're available
    private BluetoothGattService surferService;
    private ArrayList<BluetoothGattCharacteristic> surferServiceCharacteristics;

    private Intent mBTLE_Service_Intent;
    private Service_BTLE_GATT BTLE_GATT_Service; // This is really the GATT Controller, not the Specific RFID Reader Service
    private boolean mBTLE_Service_Bound;
    private BroadcastReceiver_BTLE_GATT mGattUpdateReceiver;

    private String name;
    private String address;

    // SURFER CHANGE: These are the SURFER UUID's
    private String surferServiceUUID = "e7560001-fc1d-8db5-ad46-26e5843b5915";
    private String writeStateCharacteristicUUID = "e7560002-fc1d-8db5-ad46-26e5843b5915";
    private String writeTargetEPCCharacteristicUUID = "e7560003-fc1d-8db5-ad46-26e5843b5915";
    private String writeNewEPCCharacteristicUUID = "e7560004-fc1d-8db5-ad46-26e5843b5915";
    private String readStateCharacteristicUUID = "e7560005-fc1d-8db5-ad46-26e5843b5915";
    private String packetData1CharacteristicUUID = "e7560006-fc1d-8db5-ad46-26e5843b5915";
    private String packetData2CharacteristicUUID = "e7560007-fc1d-8db5-ad46-26e5843b5915";
    private String waveformDataCharacteristicUUID = "e7560008-fc1d-8db5-ad46-26e5843b5915";
    private String logMessageCharacteristicUUID = "e7560009-fc1d-8db5-ad46-26e5843b5915";
    private String deviceInformationServiceUUID = "180A";
    private String hardwareRevisionStringUUID = "2A27";

    // SURFER CHANGE: These are the Characteristic Element Numbers for ArrayList
    private final static int writeStateCharacteristic = 0;
    private final static int writeTargetEPCCharacteristic = 1;
    private final static int writeNewEPCCharacteristic = 2;
    private final static int readStateCharacteristic = 3;
    private final static int packetData1Characteristic = 4;
    private final static int packetData2Characteristic = 5;
    private final static int waveformDataCharacteristic = 6;
    private final static int logMessageCharacteristic = 7;
    private final static int deviceInformationService = 8;
    private final static int hardwareRevisionString = 9;

    // SURFER CHANGE: Added an Application State
    private int a_state = UNKNOWN;

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
    private final static int UNKNOWN = 15;

    private MainActivity ma;

    private ServiceConnection mBTLE_ServiceConnection = new ServiceConnection() {

        // When the phone attempts to connect to the RFID reader, this is called automatically
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Service_BTLE_GATT.BTLeServiceBinder binder = (Service_BTLE_GATT.BTLeServiceBinder) service;
            BTLE_GATT_Service = binder.getService();
            mBTLE_Service_Bound = true;

            if (!BTLE_GATT_Service.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            // Connect to the RFID reader.
            BTLE_GATT_Service.connect(address);
        }

        // When reader disconnects from app
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            BTLE_GATT_Service = null;
            mBTLE_Service_Bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set activity_btle_services.xml to be visible
        setContentView(R.layout.activity_btle_services);

        Button btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        Button btnInitialize = (Button) findViewById(R.id.btnInitialize);
        Button btnInventory = (Button) findViewById(R.id.btnInventory);

        // Disconnect from reader and return to Scan page
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BTLE_GATT_Service.disconnect();
            }
        });

        // Send initialize code to initialize reader
        btnInitialize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send 02 to e7560002-fc1d-8db5-ad46-26e5843b5915

                // Couldn't figure out the best way to get a characteristic based on UUID.
                // I can think of a few ways, but I figured I'd let you do this since you know more
                // about what this SHOULD look like for the final product.
                // (I guess I'll let you do initialize since it's really only sending 0x02)

                /*BluetoothGattCharacteristic characteristic = characteristics_HashMap.get(services_ArrayList.get(x).getUuid().toString());
                if (characteristic.getUuid().toString() == "e7560002-fc1d-8db5-ad46-26e5843b5915"){
                    if (Utils.hasWriteProperty(characteristic.getProperties()) != 0) {
                        byte[] b = {(byte) 2};
                        // Set value of write from what was typed
                        characteristic.setValue(b);
                        // Send write to reader
                        Service_BTLE_GATT service;
                        BTLE_GATT_Service.writeCharacteristic(characteristic);
                    }
                }*/
                if( surferService != null && surferServiceCharacteristics != null) {
                    // test to see if it's detecting the click
                    Utils.toast(getApplicationContext(),"Initialize sent?");
                    byte[] b = {(byte) (2)};
                    surferServiceCharacteristics.get(writeStateCharacteristic).setValue(b);
                    //expandableListAdapter.notifyDataSetChanged(); // Temporary, and just to see if we're pushing the values correctly
                    BTLE_GATT_Service.writeCharacteristic(surferServiceCharacteristics.get(writeStateCharacteristic));
                } else {
                    // test to see if it's detecting the click
                    Utils.toast(getApplicationContext(),"surferService = null");
                }
            }
        });

        // Send inventory code to tell reader to take inventory
        btnInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( surferService != null ) {
                    byte[] b = {(byte) (5)};
                    surferServiceCharacteristics.get(writeStateCharacteristic).setValue(b);
                    //expandableListAdapter.notifyDataSetChanged(); // Temporary, and just to see if we're pushing the values correctly
                    BTLE_GATT_Service.writeCharacteristic(surferServiceCharacteristics.get(writeStateCharacteristic));
                }
            }
        });

        // Get name and address
        Intent intent = getIntent();
        name = intent.getStringExtra(Activity_BTLE_Services.EXTRA_NAME);
        address = intent.getStringExtra(Activity_BTLE_Services.EXTRA_ADDRESS);

        // Create clear storage variables for services
        services_ArrayList = new ArrayList<>();
        characteristics_HashMap = new HashMap<>();
        characteristics_HashMapList = new HashMap<>();

        // Instantiate services list
        expandableListAdapter = new ListAdapter_BTLE_Services(
                this, services_ArrayList, characteristics_HashMapList);

        // Set view to be lv_expandable and write to it via expandableListAdapter
        expandableListView = (ExpandableListView) findViewById(R.id.lv_expandable);
        expandableListView.setAdapter(expandableListAdapter);
        expandableListView.setOnChildClickListener(this);

        // Set name and address of service on the view
        ((TextView) findViewById(R.id.tv_name)).setText(name + " Services");
        ((TextView) findViewById(R.id.tv_address)).setText(address);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Instantiate GATT server handler
        mGattUpdateReceiver = new BroadcastReceiver_BTLE_GATT(this);

        // Register current activity as a GATT server handler
        registerReceiver(mGattUpdateReceiver, Utils.makeGattUpdateIntentFilter());

        // Instantiate service handler (Activity_BTLE_Services) with GATT server (Service_BTLE_GATT)
        mBTLE_Service_Intent = new Intent(this, Service_BTLE_GATT.class);
        bindService(mBTLE_Service_Intent, mBTLE_ServiceConnection, Context.BIND_AUTO_CREATE);
        startService(mBTLE_Service_Intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    // Whenever the activity is stopped, reader disconnects
    // Make the Android back button do the same thing as the disconnect button
    // QOL feature
    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect from reader
        Utils.toast(getApplicationContext(), "Disconnected from reader");

        // TODO: Do we reset the reader when disconnecting from it?
        // TODO: WILL ALSO NEED TO BE CHANGED FOR DISCONNECT BUTTON

        BTLE_GATT_Service.disconnect();

        // Unbind GATT server and services handler
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mBTLE_ServiceConnection);
        mBTLE_Service_Intent = null;
    }

    // When the expandable group CHARACTERISTICS are clicked, this function is called
    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

        // Get characteristic being clicked on
        BluetoothGattCharacteristic characteristic = characteristics_HashMapList.get(
                services_ArrayList.get(groupPosition).getUuid().toString())
                .get(childPosition);

        // If the characteristic has the write property
        if (Utils.hasWriteProperty(characteristic.getProperties()) != 0) {

            // Get the UUID of the characteristic
            String uuid = characteristic.getUuid().toString();
            Dialog_BTLE_Characteristic dialog_btle_characteristic = new Dialog_BTLE_Characteristic();

            // Set the title of the characteristic to its UUID
            dialog_btle_characteristic.setTitle(uuid);

            dialog_btle_characteristic.setService(BTLE_GATT_Service);
            dialog_btle_characteristic.setCharacteristic(characteristic);

            // Show a Dialog_BTLE_Characteristic screen
            dialog_btle_characteristic.show(getFragmentManager(), "Dialog_BTLE_Characteristic");
        } else if (Utils.hasReadProperty(characteristic.getProperties()) != 0) {
            if (BTLE_GATT_Service != null) {
                BTLE_GATT_Service.readCharacteristic(characteristic);
            }
        } else if (Utils.hasNotifyProperty(characteristic.getProperties()) != 0) {
            if (BTLE_GATT_Service != null) {
                BTLE_GATT_Service.setCharacteristicNotification(characteristic, true);
            }
        }

        return false;
    }

    // Get services and characteristics from the reader
    public void updateServices() {

        if (BTLE_GATT_Service != null) {

            // Clear the service and characteristics storage
            services_ArrayList.clear();
            characteristics_HashMap.clear();
            characteristics_HashMapList.clear();

            // Clear the SURFER Variables, so if we're no longer connected then the buttons won't do anything
            surferService = null;
            if (surferServiceCharacteristics!=null)surferServiceCharacteristics.clear();

            List<BluetoothGattService> servicesList = BTLE_GATT_Service.getSupportedGattServices();

            for (BluetoothGattService service : servicesList) {

                // Add new services and characteristics to a services and characteristics list
                services_ArrayList.add(service);

                List<BluetoothGattCharacteristic> characteristicsList = service.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> newCharacteristicsList = new ArrayList<>();

                for (BluetoothGattCharacteristic characteristic: characteristicsList) {
                    characteristics_HashMap.put(characteristic.getUuid().toString(), characteristic);
                    newCharacteristicsList.add(characteristic);
                }

                // Add the correlating characteristics list to a
                // hasp map with the service as the index
                characteristics_HashMapList.put(service.getUuid().toString(), newCharacteristicsList);

                // If this is the SURFER (it should always be due to BTLE filtering)
                // Had to replace == with .equals
                if(service.getUuid().toString().equals(surferServiceUUID)) {
                    surferService = service;
                    surferServiceCharacteristics = characteristics_HashMapList.get(service.getUuid().toString()); // This is a supposed to be a pointer/reference, but I'm not fully sure
                }
            }

            // If services exist, notify that data was changed
            if (servicesList != null && servicesList.size() > 0) {
                expandableListAdapter.notifyDataSetChanged();
            }
        }
    }

    // Change value of data in expandableListAdapter?
    public void updateCharacteristic() {
        // SURFER CHANGE: SURFER State Machine for Characteristic Handlers
        // For the moment we can just print log messages for testing
        if(surferService != null) {
            if(writeTargetEPCCharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // To be Implemented

                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(writeNewEPCCharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // To be Implemented

                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(readStateCharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // Not sure if the peripheral_state byte thing will work like this
                byte[] peripheral_state = surferServiceCharacteristics.get(readStateCharacteristic).getValue();

                // I was too tired to finish fleshing this out. It essentially doesn't alter the application state, but that's fine because I'm not checking it anywhere for the moment due to a lack
                // of need because of the low amount of implemented functions/buttons.
                switch(a_state) {
                    case IDLE_UNCONFIGURED:
                        switch(peripheral_state[0]) {
                            case INITIALIZING:
                                break;
                            case RESET_ASICS:
                                break;
                            case IDLE_UNCONFIGURED:
                                break;
                            default:
                                break;
                        }
                        break;
                    case IDLE_CONFIGURED:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                break;
                            case INITIALIZING:
                                break;
                            case SEARCHING_APP_SPECD:
                                break;
                            case SEARCHING_LAST_INV:
                                break;
                            case INVENTORYING:
                                break;
                            case TESTING_DTC:
                                break;
                            case PROG_APP_SPECD:
                                break;
                            case PROG_LAST_INV:
                                break;
                            case RECOV_WVFM_MEM:
                                break;
                            case RESET_ASICS:
                                break;
                            case KILL_TAG:
                                break;
                            case PROG_TAG_KILL_PW:
                                break;
                            case TRACK_APP_SPECD:
                                break;
                            case TRACK_LAST_INV:
                                break;
                            default:
                                break;
                        }
                        break;
                    case INITIALIZING:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                break;
                            case IDLE_UNCONFIGURED:
                                break;
                            default:
                                break;
                        }
                        break;
                    case SEARCHING_APP_SPECD:
                        break;
                    case SEARCHING_LAST_INV:
                        switch(peripheral_state[0]) {
                            default:
                                break;
                        }
                        break;
                    case INVENTORYING:
                        switch(peripheral_state[0]) {
                            default:
                                break;
                        }
                        break;
                    case TESTING_DTC:
                        switch(peripheral_state[0]) {
                            default:
                                break;
                        }
                        break;
                    case PROG_APP_SPECD:
                        break;
                    case PROG_LAST_INV:
                        switch(peripheral_state[0]) {
                            default:
                                break;
                        }
                        break;
                    case RECOV_WVFM_MEM:
                        switch(peripheral_state[0]) {
                            default:
                                break;
                        }
                        break;
                    case RESET_ASICS:
                        switch(peripheral_state[0]) {
                            default:
                                break;
                        }
                        break;
                    case KILL_TAG:
                        switch(peripheral_state[0]) {
                            default:
                                break;
                        }
                        break;
                    case PROG_TAG_KILL_PW:
                        switch(peripheral_state[0]) {
                            default:
                                break;
                        }
                        break;
                    case TRACK_APP_SPECD:
                        switch(peripheral_state[0]) {
                            default:
                                break;
                        }
                        break;
                    case TRACK_LAST_INV:
                        switch(peripheral_state[0]) {
                            default:
                                break;
                        }
                        break;
                    case UNKNOWN:
                        switch(peripheral_state[0]) {
                            case IDLE_UNCONFIGURED:
                                break;
                            case IDLE_CONFIGURED:
                                break;
                            case TESTING_DTC:
                                break;
                            case TRACK_APP_SPECD:
                                break;
                            case TRACK_LAST_INV:
                                break;
                            case INITIALIZING:
                                break;
                            default:
                                break;
                        }
                        break;
                }
                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }

            // packetData1 and packetData2 are used for Inventory and Search results
            else if(packetData1CharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // For Inventory Command, prints out message until RFID Tags List is Fully Implemented
                Utils.toast(getApplicationContext(), Utils.hexToString(surferServiceCharacteristics.get(readStateCharacteristic).getValue()));
                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(packetData2CharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // For Inventory Command, prints out message until RFID Tags List is Fully Implemented
                Utils.toast(getApplicationContext(), Utils.hexToString(surferServiceCharacteristics.get(readStateCharacteristic).getValue()));
                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(waveformDataCharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // To be Implemented

                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(logMessageCharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // To be Implemented

                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(hardwareRevisionStringUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // To be Implemented

                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            // This one is an error if the surferService Exists
            else {
                // To be Implemented

                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
        }


        expandableListAdapter.notifyDataSetChanged();
    }
}
