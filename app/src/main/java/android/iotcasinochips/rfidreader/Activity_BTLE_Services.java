package android.iotcasinochips.rfidreader;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Switch;
import android.widget.TextView;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * This is the Java controller file for activity_btle_services.xml . This is where you can find
 * the click listeners for the buttons
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
    public ArrayList<RFID_Tag> surferTagArrayList;

    private Intent mBTLE_Service_Intent;
    private Service_BTLE_GATT BTLE_GATT_Service; // This is really the GATT Controller, not the Specific RFID Reader Service
    private boolean mBTLE_Service_Bound;
    private BroadcastReceiver_BTLE_GATT mGattUpdateReceiver;

    private String name;
    private String address;

    // SURFER CHANGE: These are the SURFER UUID's
    private final static String surferServiceUUID = "e7560001-fc1d-8db5-ad46-26e5843b5915";
    private final static String writeStateCharacteristicUUID = "e7560002-fc1d-8db5-ad46-26e5843b5915";
    private final static String writeTargetEPCCharacteristicUUID = "e7560003-fc1d-8db5-ad46-26e5843b5915";
    private final static String writeNewEPCCharacteristicUUID = "e7560004-fc1d-8db5-ad46-26e5843b5915";
    public final static String readStateCharacteristicUUID = "e7560005-fc1d-8db5-ad46-26e5843b5915";
    private final static String packetData1CharacteristicUUID = "e7560006-fc1d-8db5-ad46-26e5843b5915";
    private final static String packetData2CharacteristicUUID = "e7560007-fc1d-8db5-ad46-26e5843b5915";
    private final static String waveformDataCharacteristicUUID = "e7560008-fc1d-8db5-ad46-26e5843b5915";
    public final static String logMessageCharacteristicUUID = "e7560009-fc1d-8db5-ad46-26e5843b5915";
    private final static String deviceInformationServiceUUID = "180A";
    private final static String hardwareRevisionStringUUID = "2A27";

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
    private boolean initialByte = true;

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

    volatile public int checkSuccess;

    private MainActivity ma;

    private ServiceConnection mBTLE_ServiceConnection = new ServiceConnection() {

        // Triggered when this ServiceConnection is triggered
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

        // Triggered when this ServiceConnection is killed
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            BTLE_GATT_Service = null;
            mBTLE_Service_Bound = false;
        }
    };

    /**
     * Send command based on int
     **/
    public void sendCommand(int commandInt){
        // Cast commandInt to a byte
        byte[] b = {(byte) (commandInt)};

        // Set write type of writeStateCharacteristic
        surferServiceCharacteristics.get(writeStateCharacteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        // Set value to b
        surferServiceCharacteristics.get(writeStateCharacteristic).setValue(b);

        // Write characteristic
        BTLE_GATT_Service.writeCharacteristic(surferServiceCharacteristics.get(writeStateCharacteristic));
    }

    // Send target EPC
    public void sendTargetEPC(byte[] b){
        // Set write type of writeTargetEPCCharacteristic
        surferServiceCharacteristics.get(writeTargetEPCCharacteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        // Set value to b
        surferServiceCharacteristics.get(writeTargetEPCCharacteristic).setValue(b);

        // Write target EPC
        BTLE_GATT_Service.writeCharacteristic(surferServiceCharacteristics.get(writeTargetEPCCharacteristic));
    }
    // Send command based on given byte array
    public void sendNewEPC(byte[] b){
        // Set write type of writeTargetEPCCharacteristic
        surferServiceCharacteristics.get(writeNewEPCCharacteristic).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        // Set value to b
        surferServiceCharacteristics.get(writeNewEPCCharacteristic).setValue(b);

        // Write new EPC
        BTLE_GATT_Service.writeCharacteristic(surferServiceCharacteristics.get(writeNewEPCCharacteristic));
    }

    // Function to update buttons
    public void updateButtons(int state){
        Button btnInitialize = (Button) findViewById(R.id.btnInitialize);
        Button btnInventory = (Button) findViewById(R.id.btnInventory);
        Button btnReset = (Button) findViewById(R.id.btnReset);
        Button btnTagList = (Button) findViewById(R.id.btnTagList);
        Button btnSendTargetEPC = (Button) findViewById(R.id.btnSendTargetEPC);
        Button btnSendNewEPC = (Button) findViewById(R.id.btnSendNewEPC);
        Button btnSearch = (Button) findViewById(R.id.btnSearch);
        Button btnTrack = (Button) findViewById(R.id.btnTrack);
        Button btnProgram = (Button) findViewById(R.id.btnProgram);
        Button btnKillPassword = (Button) findViewById(R.id.btnKillPassword);
        Button btnKill = (Button) findViewById(R.id.btnKill);
        EditText input_targetEPC = (EditText) findViewById(R.id.input_targetEPC);
        EditText input_newEPC = (EditText) findViewById(R.id.input_newEPC);
        Switch swLastInventory = (Switch) findViewById(R.id.switch_lastInv);

        switch(state){
            case IDLE_CONFIGURED:
            case INVENTORYING:
                // Enable initialize
                btnInitialize.setEnabled(false);
                // Disable buttons before initializing
                btnInventory.setEnabled(true);
                btnReset.setEnabled(true);
                btnTagList.setEnabled(true);
                btnSendTargetEPC.setEnabled(true);
                btnSendNewEPC.setEnabled(true);
                btnSearch.setEnabled(true);
                btnTrack.setEnabled(true);
                btnProgram.setEnabled(true);
                btnKillPassword.setEnabled(true);
                btnKill.setEnabled(true);
                input_targetEPC.setEnabled(true);
                input_newEPC.setEnabled(true);
                swLastInventory.setEnabled(true);
                break;
            default:
                // Enable initialize
                btnInitialize.setEnabled(true);
                // Disable buttons before initializing
                btnInventory.setEnabled(false);
                btnReset.setEnabled(false);
                //btnTagList.setEnabled(false);
                btnSendTargetEPC.setEnabled(false);
                btnSendNewEPC.setEnabled(false);
                btnSearch.setEnabled(false);
                //btnTrack.setEnabled(false);
                btnProgram.setEnabled(false);
                btnKillPassword.setEnabled(false);
                btnKill.setEnabled(false);
                input_targetEPC.setEnabled(false);
                input_newEPC.setEnabled(false);
                swLastInventory.setEnabled(false);
                break;
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set activity_btle_services.xml to be visible
        setContentView(R.layout.activity_btle_services);

        Button btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        Button btnInitialize = (Button) findViewById(R.id.btnInitialize);
        Button btnInventory = (Button) findViewById(R.id.btnInventory);
        Button btnReset = (Button) findViewById(R.id.btnReset);
        Button btnTagList = (Button) findViewById(R.id.btnTagList);
        Button btnSendTargetEPC = (Button) findViewById(R.id.btnSendTargetEPC);
        Button btnSendNewEPC = (Button) findViewById(R.id.btnSendNewEPC);
        Button btnSearch = (Button) findViewById(R.id.btnSearch);
        Button btnTrack = (Button) findViewById(R.id.btnTrack);
        Button btnProgram = (Button) findViewById(R.id.btnProgram);
        Button btnKillPassword = (Button) findViewById(R.id.btnKillPassword);
        Button btnKill = (Button) findViewById(R.id.btnKill);
        EditText input_targetEPC = (EditText) findViewById(R.id.input_targetEPC);
        EditText input_newEPC = (EditText) findViewById(R.id.input_newEPC);
        Switch swLastInventory = (Switch) findViewById(R.id.switch_lastInv);

        // Disconnect from reader and return to Scan page
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Buttons", "Disconnect button pressed.");

                onBackPressed();
            }
        });

        // Send initialize code to initialize reader
        btnInitialize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( surferService != null && surferServiceCharacteristics != null) {
                    // test to see if it's detecting the click
                    // Test to make sure that we're calling the correct Characteristic
                    if (surferServiceCharacteristics.get(writeStateCharacteristic).getUuid().toString().equals(writeStateCharacteristicUUID)) {
                        Log.i("Buttons", "Initialize button pressed.");
                        // Cast 2 to a byte
                        sendCommand(INITIALIZING);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i("Threads","Initialization Thread Start");

                                // This call isn't successful. I'm not sure why, but I had to leave it. It's kind of like an initialization
                                checkSuccess = BluetoothGatt.GATT_FAILURE;
                                BTLE_GATT_Service.setCharacteristicIndication(surferServiceCharacteristics.get(writeTargetEPCCharacteristic), true);
                                while (checkSuccess != BluetoothGatt.GATT_SUCCESS){
                                    try {
                                        checkSuccess = BTLE_GATT_Service.getCheckSuccess();
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                BTLE_GATT_Service.checkSuccess = BluetoothGatt.GATT_FAILURE;
                                // -----------------------------------------------------------------------

                                // Everything below is successful

                                BTLE_GATT_Service.setCharacteristicIndication(surferServiceCharacteristics.get(writeTargetEPCCharacteristic), true);
                                while (BTLE_GATT_Service.getCheckSuccess() != BluetoothGatt.GATT_SUCCESS){
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                BTLE_GATT_Service.checkSuccess = BluetoothGatt.GATT_FAILURE;

                                BTLE_GATT_Service.setCharacteristicIndication(surferServiceCharacteristics.get(writeNewEPCCharacteristic), true);
                                while (BTLE_GATT_Service.getCheckSuccess() != BluetoothGatt.GATT_SUCCESS){
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                BTLE_GATT_Service.checkSuccess = BluetoothGatt.GATT_FAILURE;

                                BTLE_GATT_Service.setCharacteristicIndication(surferServiceCharacteristics.get(packetData1Characteristic), true);
                                while (BTLE_GATT_Service.getCheckSuccess() != BluetoothGatt.GATT_SUCCESS){
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                BTLE_GATT_Service.checkSuccess = BluetoothGatt.GATT_FAILURE;

                                BTLE_GATT_Service.setCharacteristicNotification(surferServiceCharacteristics.get(packetData2Characteristic), true);
                                while (BTLE_GATT_Service.getCheckSuccess() != BluetoothGatt.GATT_SUCCESS){
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                BTLE_GATT_Service.checkSuccess = BluetoothGatt.GATT_FAILURE;

                                BTLE_GATT_Service.setCharacteristicNotification(surferServiceCharacteristics.get(logMessageCharacteristic), true);
                                while (BTLE_GATT_Service.getCheckSuccess() != BluetoothGatt.GATT_SUCCESS){
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                BTLE_GATT_Service.checkSuccess = BluetoothGatt.GATT_FAILURE;
                            }
                        }).start();


                    }
                    else {
                        Utils.toast(getApplicationContext(), "We're not editing the right Characteristic");
                    }
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
                if( surferService != null && surferServiceCharacteristics != null ) {
                    Log.i("Buttons", "Inventory button pressed.");
                    sendCommand(INVENTORYING);
                    surferTagArrayList.clear();
                    //BTLE_GATT_Service.setCharacteristicIndication(surferServiceCharacteristics.get(packetData1Characteristic), true);
                }
            }
        });

        // SURFER CHANGE: Reset button to get SURFER into initial state
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( surferService != null && surferServiceCharacteristics != null ) {
                    Log.i("Buttons", "Reset button pressed.");
                    // Send reset command
                    sendCommand(RESET_ASICS);
                }
            }
        });

        btnTagList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ADD INFORMATION HERE ONCE WE'VE FINISHED EVERYTHING FOR THE CLASSES
                Intent intent2 = new Intent(view.getContext(), RFID_Taglist.class);
                Bundle args = new Bundle();
                args.putSerializable(RFID_Taglist.TAG_ARRAY_LIST, (Serializable)surferTagArrayList );
                intent2.putExtra(RFID_Taglist.TAG_BUNDLE, args);
                startActivity(intent2);
            }
        });

        btnSendTargetEPC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (input_targetEPC.getText().toString().isEmpty()) {
                    Log.i("Buttons", "SendTargetEPC button pressed with blank input.");
                    byte[] b = {};
                    sendTargetEPC(b);
                } else if (input_targetEPC.getText().toString().length() == 1 || input_targetEPC.getText().toString().length() > 24) {
                    Utils.toast(getApplicationContext(), "Please enter a valid Target EPC.");
                }
                else {
                    String input = input_targetEPC.getText().toString();
                    int len = input.length();
                    byte[] b = new byte[12];
                    for (int i = 0; i < len; i += 2) {
                        b[i / 2] = (byte) ((Character.digit(input.charAt(i), 16) << 4)
                                + Character.digit(input.charAt(i+1), 16));
                    }
                    Log.i("Target EPC", Utils.hexToString(b));
                    sendTargetEPC(b);
                }
            }
        });

        btnSendNewEPC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (input_newEPC.getText().toString().isEmpty()) {
                    Log.i("Buttons", "SendNewEPC button pressed with blank input.");
                    byte[] b = {};
                    sendNewEPC(b);
                } else if (input_newEPC.getText().toString().length() == 1 || input_newEPC.getText().toString().length() > 24) {
                    Utils.toast(getApplicationContext(), "Please enter a valid New EPC.");
                }
                else {
                    String input = input_newEPC.getText().toString();
                    int len = input.length();
                    byte[] b = new byte[12];
                    for (int i = 0; i < len; i += 2) {
                        b[i / 2] = (byte) ((Character.digit(input.charAt(i), 16) << 4)
                                + Character.digit(input.charAt(i+1), 16));
                    }
                    Log.i("New EPC", Arrays.toString(b));
                    sendNewEPC(b);
                }
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (swLastInventory.isChecked()){
                    sendCommand(SEARCHING_LAST_INV);
                } else {
                    sendCommand(SEARCHING_APP_SPECD);
                }
            }
        });

        btnTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (swLastInventory.isChecked()){
                    sendCommand(TRACK_LAST_INV);
                } else {
                    sendCommand(TRACK_APP_SPECD);
                }
            }
        });

        btnProgram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (swLastInventory.isChecked()){
                    sendCommand(PROG_LAST_INV);
                } else {
                    sendCommand(PROG_APP_SPECD);
                }
            }
        });

        btnKillPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand(PROG_TAG_KILL_PW);
            }
        });

        btnKill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand(KILL_TAG);
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
        surferTagArrayList = new ArrayList<>();

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

        // Start service to trigger "onServiceConnected"
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

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        // Disconnect from reader
        Utils.toast(getApplicationContext(), "Disconnected from reader");

        // Unbind GATT server and services handler
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mBTLE_ServiceConnection);
        mBTLE_Service_Intent = null;

        BTLE_GATT_Service.disconnect();
    }

    // Whenever the activity is stopped, reader disconnects
    // Make the Android back button do the same thing as the disconnect button
    // QOL feature
    @Override
    protected void onStop() {
        super.onStop();

        /*
        // Disconnect from reader
        Utils.toast(getApplicationContext(), "Disconnected from reader");

        BTLE_GATT_Service.disconnect();

        // Unbind GATT server and services handler
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mBTLE_ServiceConnection);
        mBTLE_Service_Intent = null;*/
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
        } else if (Utils.hasIndicateProperty(characteristic.getProperties()) != 0) {
            if (BTLE_GATT_Service != null) {
                BTLE_GATT_Service.setCharacteristicIndication(characteristic, true);
            }
        }

        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
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

                    Log.i("Activity_BTLE_Services", "SURFER found");
                    surferServiceCharacteristics = characteristics_HashMapList.get(surferService.getUuid().toString()); // This is a supposed to be a pointer/reference, but I'm not fully sure

                    if (surferServiceCharacteristics != null) {
                        BTLE_GATT_Service.setCharacteristicIndication(surferServiceCharacteristics.get(readStateCharacteristic), true);
                    } else {Log.i("SurferNullError", "Surfer characteristics not found");}
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
                EditText input_targetEPC = (EditText) findViewById(R.id.input_targetEPC);

                String s = new String(surferServiceCharacteristics.get(writeTargetEPCCharacteristic).getValue());
                if (s.equals("")){
                    input_targetEPC.setHint("Blank EPC");
                    input_targetEPC.setHintTextColor(Color.GREEN);
                }
                else input_targetEPC.setTextColor(Color.GREEN);
                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(writeNewEPCCharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                EditText input_newEPC = (EditText) findViewById(R.id.input_newEPC);
                String s = new String(surferServiceCharacteristics.get(writeNewEPCCharacteristic).getValue(), StandardCharsets.UTF_8);

                input_newEPC.setTextColor(Color.GREEN);

                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(readStateCharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // Not sure if the peripheral_state byte thing will work like this
                byte[] peripheral_state = surferServiceCharacteristics.get(readStateCharacteristic).getValue();
                Log.i("StateChanged", "State Changed");

                // Enable/Disable correct buttons
                updateButtons(peripheral_state[0]);

                // I was too tired to finish fleshing this out. It essentially doesn't alter the application state, but that's fine because I'm not checking it anywhere for the moment due to a lack
                // of need because of the low amount of implemented functions/buttons.
                switch(a_state) {
                    case IDLE_UNCONFIGURED:
                        switch(peripheral_state[0]) {
                            case INITIALIZING:
                                a_state = INITIALIZING;
                                Utils.toast(getApplicationContext(), "Initialized Received");
                                break;
                            case RESET_ASICS:
                                a_state = RESET_ASICS;
                                break;
                            case IDLE_UNCONFIGURED:
                                a_state = IDLE_UNCONFIGURED;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case IDLE_CONFIGURED:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                a_state = IDLE_UNCONFIGURED;
                                break;
                            case INITIALIZING:
                                a_state = INITIALIZING;
                                break;
                            case SEARCHING_APP_SPECD:
                                a_state = SEARCHING_APP_SPECD;
                                break;
                            case SEARCHING_LAST_INV:
                                a_state = SEARCHING_LAST_INV;
                                break;
                            case INVENTORYING:
                                a_state = INVENTORYING;
                                break;
                            case TESTING_DTC:
                                a_state = TESTING_DTC;
                                break;
                            case PROG_APP_SPECD:
                                a_state = PROG_APP_SPECD;
                                break;
                            case PROG_LAST_INV:
                                a_state = PROG_LAST_INV;
                                break;
                            case RECOV_WVFM_MEM:
                                a_state = RECOV_WVFM_MEM;
                                break;
                            case RESET_ASICS:
                                a_state = RESET_ASICS;
                                break;
                            case KILL_TAG:
                                a_state = KILL_TAG;
                                break;
                            case PROG_TAG_KILL_PW:
                                a_state = PROG_TAG_KILL_PW;
                                break;
                            case TRACK_APP_SPECD:
                                a_state = TRACK_APP_SPECD;
                                break;
                            case TRACK_LAST_INV:
                                a_state = TRACK_LAST_INV;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
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
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case SEARCHING_APP_SPECD:
                        break;
                    case SEARCHING_LAST_INV:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                a_state = IDLE_CONFIGURED;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case INVENTORYING:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                a_state = IDLE_CONFIGURED;
                                Log.i(TAG, "Inventory Over");
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case TESTING_DTC:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                a_state = IDLE_CONFIGURED;
                                break;
                            case TESTING_DTC:
                                a_state = TESTING_DTC;
                                break;
                            case RESET_ASICS:
                                a_state = RESET_ASICS;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case PROG_APP_SPECD:
                        break;
                    case PROG_LAST_INV:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                a_state = IDLE_CONFIGURED;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case RECOV_WVFM_MEM:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                // This isn't finished and will need to receive the Waveform FIFO
                                a_state = IDLE_CONFIGURED;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case RESET_ASICS:
                        switch(peripheral_state[0]) {
                            case IDLE_UNCONFIGURED:
                                a_state = IDLE_UNCONFIGURED;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case KILL_TAG:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                a_state = IDLE_CONFIGURED;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case PROG_TAG_KILL_PW:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                a_state = IDLE_CONFIGURED;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case TRACK_APP_SPECD:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                a_state = IDLE_CONFIGURED;
                                break;
                            case TRACK_APP_SPECD:
                                a_state = TRACK_APP_SPECD;
                                break;
                            case RESET_ASICS:
                                a_state = RESET_ASICS;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case TRACK_LAST_INV:
                        switch(peripheral_state[0]) {
                            case IDLE_CONFIGURED:
                                a_state = IDLE_CONFIGURED;
                                break;
                            case TRACK_LAST_INV:
                                a_state = TRACK_LAST_INV;
                                break;
                            case RESET_ASICS:
                                a_state = RESET_ASICS;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                    case UNKNOWN:
                        Log.i(TAG, "Exiting Unknown State");
                        switch(peripheral_state[0]) {
                            case IDLE_UNCONFIGURED:
                                a_state = IDLE_UNCONFIGURED;
                                break;
                            case IDLE_CONFIGURED:
                                a_state = IDLE_CONFIGURED;
                                break;
                            case TESTING_DTC:
                                a_state = TESTING_DTC;
                                break;
                            case TRACK_APP_SPECD:
                                a_state = TRACK_APP_SPECD;
                                break;
                            case TRACK_LAST_INV:
                                a_state = TRACK_LAST_INV;
                                break;
                            case INITIALIZING:
                                a_state = INITIALIZING;
                                break;
                            default:
                                a_state = peripheral_state[0];
                                Log.i(TAG, "Incompatible State Error!");
                                break;
                        }
                        break;
                }
                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }

            // packetData1 and packetData2 are used for Inventory and Search results
            else if(packetData1CharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // For Inventory Command, prints out message until RFID Tags List is Fully Implemented
                //Utils.toast(getApplicationContext(), Utils.hexToString(surferServiceCharacteristics.get(readStateCharacteristic).getValue()));
                Utils.toast(getApplicationContext(), "Inventory information received");
                byte [] data = new byte[12];
                System.arraycopy(surferServiceCharacteristics.get(packetData1Characteristic).getValue(), 0, data, 0, 12);
                Log.i("TagInfo", Utils.hexToString(surferServiceCharacteristics.get(packetData1Characteristic).getValue()));

                // SURFER CHANGE: Adding RFID_Tags
                boolean found = false;
                for( RFID_Tag element : surferTagArrayList ) { // Weird Loop to see if we found it
                    if(Arrays.equals(element.RFID_EPC, data)) {
                        found = true;
                        element.tagDataProcessor(surferServiceCharacteristics.get(packetData1Characteristic).getValue());
                        int index = surferTagArrayList.indexOf(element);
                        surferTagArrayList.set(index, element);
                        if (surferTagArrayList.get(index).expectingSupplementData)
                            Log.i("List_Change","List Changed.");
                    }
                }
                if(!found) {
                    RFID_Tag new_tag = new RFID_Tag(surferServiceCharacteristics.get(packetData1Characteristic).getValue());
                    surferTagArrayList.add(new_tag);
                }

                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(packetData2CharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // For Inventory Command, prints out message until RFID Tags List is Fully Implemented
                Utils.toast(getApplicationContext(), "Inventory 2 information received");

                Log.i("Packet2Info", Utils.hexToString(surferServiceCharacteristics.get(packetData2Characteristic).getValue()));

                // SURFER CHANGE: Adding RFID_Tags
                boolean found = false;
                for( RFID_Tag element : surferTagArrayList ) { // Weird Loop to see if we found it
                    if(element.expectingSupplementData) {
                        found = true;
                        element.tagDataProcessor(surferServiceCharacteristics.get(packetData2Characteristic).getValue());
                    }
                }
                if(!found) {
                    Log.i("SURFER Error","Couldn't find tag for packet 2");
                }

                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(waveformDataCharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                // To be Implemented

                BTLE_GATT_Service.changedCharacteristicUUID = null;
            }
            else if(logMessageCharacteristicUUID.equals(BTLE_GATT_Service.changedCharacteristicUUID)) {
                //String s = new String(surferServiceCharacteristics.get(logMessageCharacteristic).getValue());
                //Log.i("SURFER:",s);
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
