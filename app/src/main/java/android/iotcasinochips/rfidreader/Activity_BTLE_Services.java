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

    private Intent mBTLE_Service_Intent;
    private Service_BTLE_GATT RFID_reader_service;
    private boolean mBTLE_Service_Bound;
    private BroadcastReceiver_BTLE_GATT mGattUpdateReceiver;

    private String name;
    private String address;

    private MainActivity ma;

    private ServiceConnection mBTLE_ServiceConnection = new ServiceConnection() {

        // When the phone attempts to connect to the RFID reader, this is called automatically
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Service_BTLE_GATT.BTLeServiceBinder binder = (Service_BTLE_GATT.BTLeServiceBinder) service;
            RFID_reader_service = binder.getService();
            mBTLE_Service_Bound = true;

            if (!RFID_reader_service.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            // Connect to the RFID reader.
            RFID_reader_service.connect(address);
        }

        // When reader disconnects from app
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            RFID_reader_service = null;
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
                RFID_reader_service.disconnect();
            }
        });

        // Send initialize code to initialize reader
        btnInitialize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // Send inventory code to tell reader to take inventory
        btnInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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

        RFID_reader_service.disconnect();

        // Unbind GATT server and services handler
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mBTLE_ServiceConnection);
        mBTLE_Service_Intent = null;
    }

    // When the expandable group services are clicked, this function is called
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

            dialog_btle_characteristic.setService(RFID_reader_service);
            dialog_btle_characteristic.setCharacteristic(characteristic);

            // Show a Dialog_BTLE_Characteristic screen
            dialog_btle_characteristic.show(getFragmentManager(), "Dialog_BTLE_Characteristic");
        } else if (Utils.hasReadProperty(characteristic.getProperties()) != 0) {
            if (RFID_reader_service != null) {
                RFID_reader_service.readCharacteristic(characteristic);
            }
        } else if (Utils.hasNotifyProperty(characteristic.getProperties()) != 0) {
            if (RFID_reader_service != null) {
                RFID_reader_service.setCharacteristicNotification(characteristic, true);
            }
        }

        return false;
    }

    // Get services and characteristics from the reader
    public void updateServices() {

        if (RFID_reader_service != null) {

            // Clear the service and characteristics storage
            services_ArrayList.clear();
            characteristics_HashMap.clear();
            characteristics_HashMapList.clear();

            List<BluetoothGattService> servicesList = RFID_reader_service.getSupportedGattServices();

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
            }

            // If services exist, notify that data was changed
            if (servicesList != null && servicesList.size() > 0) {
                expandableListAdapter.notifyDataSetChanged();
            }
        }
    }

    // Change value of data in expandableListAdapter
    public void updateCharacteristic() {
        expandableListAdapter.notifyDataSetChanged();
    }
}
