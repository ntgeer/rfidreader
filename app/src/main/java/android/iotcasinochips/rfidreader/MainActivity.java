package android.iotcasinochips.rfidreader;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private final static String TAG = MainActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int BTLE_SERVICES = 2;

    private HashMap<String, BTLE_Device> mBTDevicesHashMap;
    private ArrayList<BTLE_Device> mBTDevicesArrayList;
    private ListAdapter_BTLE_Devices adapter;
    private ListView listView;

    private Button btn_Scan;

    public BroadcastReceiver_BTState mBTStateUpdateReceiver;
    private Scanner_BTLE mBTLeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Utils.toast(getApplicationContext(), "BLE not supported");
            finish();
        }

        // Instantiate BroadcastReceiver and Scanner
        // BroadcastReceiver is the receiver, Scanner is the transmitter
        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());

        // Initiate scanner with a scanning period of 5 seconds, and a signal strength threshold of -90dMb
        // This can be set very high because only SURFER RFIDs are being scanned for, so no reason to filter out devices based on distance
        mBTLeScanner = new Scanner_BTLE(this, 5000, -90);

        // Declare storage lists for devices
        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();

        // Update user interfaces when lists change
        adapter = new ListAdapter_BTLE_Devices(this, R.layout.btle_device_list_item, mBTDevicesArrayList);

        // Add adapter to UI
        listView = new ListView(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        // Configure scan btnInventory
        btn_Scan = (Button) findViewById(R.id.btn_scan);
        ((ScrollView) findViewById(R.id.scrollView)).addView(listView);
        findViewById(R.id.btn_scan).setOnClickListener(this);

        // Check for permissions before running app, and send Toast message accordingly
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }
    }

    // If Bluetooth is enabled in the middle of the app
    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(mBTStateUpdateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    // Tabbing back into app. DEFAULT FUNCTION
    @Override
    protected void onResume() {
        super.onResume();
    }

    // Tabbing out of app
    @Override
    protected void onPause() {
        super.onPause();

        // Stop the scan if the app is out of focus
        stopScan();
    }

    // Closing app.
    @Override
    public void onStop() {
        super.onStop();

        // Unregister phone as Bluetooth receiver if app is closed
        unregisterReceiver(mBTStateUpdateReceiver);

        // Stop scanning for devices if app is closed
        stopScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Context context = view.getContext();

        Utils.toast(context, "Connecting...");

        // do something with the text views and start the next activity.
        stopScan();

        // Get name and address of device to connect to
        String name = mBTDevicesArrayList.get(position).getName();
        String address = mBTDevicesArrayList.get(position).getAddress();

        // Connect to device and open acitivty_btle_services page
        Intent intent = new Intent(this, Activity_BTLE_Services.class);
        intent.putExtra(Activity_BTLE_Services.EXTRA_NAME, name);
        intent.putExtra(Activity_BTLE_Services.EXTRA_ADDRESS, address);
        startActivityForResult(intent, BTLE_SERVICES);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            // If scan btnInventory is pressed, start the scan
            case R.id.btn_scan:
                Utils.toast(getApplicationContext(), "Scan Button Pressed");

                // If btnInventory pressed when scanner not scanning, start the scan
                if (!mBTLeScanner.isScanning()) {
                    startScan();
                }
                // If btnInventory pressed when scanner is scanning, stop the scan
                else {
                    stopScan();
                }

                break;
            default:
                break;
        }

    }

    // Add device to list only once
    public void addDevice(BluetoothDevice device, int rssi) {

        // Get device's MAC address
        String address = device.getAddress();

        // Add RFID reader to list if not on the list
        // Compare existing MAC addresses on list to MAC address of this device
        if (!mBTDevicesHashMap.containsKey(address)) {
            BTLE_Device btleDevice = new BTLE_Device(device);
            btleDevice.setRSSI(rssi);

            // Add device to hash map and array list
            mBTDevicesHashMap.put(address, btleDevice);
            mBTDevicesArrayList.add(btleDevice);
        }
        // If list already has RFID reader, do not add it again
        // Update the RFID reader RSSI instead
        else {
            mBTDevicesHashMap.get(address).setRSSI(rssi);
        }

        // Notify adapter (the ScrollView updater) of new data
        adapter.notifyDataSetChanged();
    }

    // Clear HashMap and Array list of existing RFID readers and start scan.
    public void startScan(){
        btn_Scan.setText("Scanning...");

        // Clear hash map and array list
        mBTDevicesArrayList.clear();
        mBTDevicesHashMap.clear();

        // Start scanner
        mBTLeScanner.start();
    }

    public void stopScan() {
        btn_Scan.setText("Scan Again");

        // Stop scanner
        mBTLeScanner.stop();
    }
}
